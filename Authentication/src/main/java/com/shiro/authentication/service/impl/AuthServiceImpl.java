package com.shiro.authentication.service.impl;

import static com.shiro.authentication.constants.Constant.EMAIL;
import static com.shiro.authentication.constants.Constant.PHONE;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.shiro.authentication.config.AppProperties;
import com.shiro.authentication.constants.*;
import com.shiro.authentication.dto.*;
import com.shiro.authentication.entity.Customer;
import com.shiro.authentication.entity.OtpChallenge;
import com.shiro.authentication.entity.User;
import com.shiro.authentication.exception.AuthException;
import com.shiro.authentication.exception.ErrorCode;
import com.shiro.authentication.exception.OtpInvalidException;
import com.shiro.authentication.repository.CustomerRepository;
import com.shiro.authentication.repository.OtpChallengeRepository;
import com.shiro.authentication.repository.UserRepository;
import com.shiro.authentication.security.RateLimiter;
import com.shiro.authentication.security.RolePermissions;
import com.shiro.authentication.service.AuthService;
import com.shiro.authentication.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  private final UserRepository users;
  private final CustomerRepository customers;
  private final OtpChallengeRepository otps;
  private final PasswordEncoder encoder;
  private final StringRedisTemplate redis;
  private final NotificationService notifications;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final RateLimiter rateLimiter;
  private final AppProperties properties;
  private final SecureRandom random = new SecureRandom();

  @Override
  @Transactional
  public void register(RegisterRequest request) {
    String identifier = normalizeIdentifier(request.getIdentifier());
    AuthChannel channel = getChannel(identifier);
    rateLimiter.consume("auth-register-identifier", identifier);

    if (users.existsByIdentifier(identifier)) {
      throw new AuthException(ErrorCode.IDENTIFIER_ALREADY_REGISTERED);
    }

    Map<String, String> pending = new HashMap<>();
    pending.put(Constant.IDENTIFIER, identifier);
    pending.put(Constant.CHANNEL, channel.name());
    pending.put(Constant.PASSWORD_HASH, encoder.encode(request.getPassword()));

    String key = registrationKey(identifier);
    redis.opsForHash().putAll(key, pending);
    redis.expire(key, properties.getAuth().getRegistrationTtl());

    issueOtp(channel, identifier);
  }

  @Override
  @Transactional(noRollbackFor = OtpInvalidException.class)
  public AuthResponse verifyOtp(VerifyOtpRequest request) {
    String identifier = normalizeIdentifier(request.getIdentifier());
    validateIdentifier(identifier);

    rateLimiter.consume("auth-otp-identifier", identifier);
    Map<Object, Object> pending = loadPendingRegistration(identifier);
    validatePendingRegistration(identifier, pending);

    OtpChallenge challenge = findRegistrationOtp(identifier);
    validateOtp(request.getOtp(), challenge);

    User user = createUser(identifier, pending);
    saveVerifiedUser(user, challenge);
    customers.save(createCustomer(user, pending));
    redis.delete(registrationKey(identifier));
    return issueAccessToken(user);
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    String identifier = normalizeIdentifier(request.getIdentifier());
    validateIdentifier(identifier);
    rateLimiter.consume("auth-login-identifier", identifier);

    Optional<User> found = users.findByIdentifier(identifier);
    if (found.isEmpty()) {
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    User user = found.get();
    if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }
    if (!user.isVerified()) {
      throw new AuthException(ErrorCode.IDENTIFIER_NOT_VERIFIED);
    }

    return issueAccessToken(user);
  }

  @Override
  public void logout(String token) {
    if (ObjectUtils.isEmpty(token) || token.isBlank()) {
      return;
    }
    try {
      Jwt jwt = jwtDecoder.decode(token);
      if (ObjectUtils.isNotEmpty(jwt.getId())) {
        redis.delete(sessionKey(jwt.getId()));
      }
    } catch (RuntimeException exception) {
      // Do nothing. Logout stays idempotent for invalid or already-expired tokens.
      log.debug("Logout ignored for invalid or expired token", exception);
    }
  }

  @Override
  public Optional<AuthenticatedPrincipal> authenticate(String token) {
    if (ObjectUtils.isEmpty(token) || token.isBlank()) {
      return Optional.empty();
    }
    try {
      Jwt jwt = jwtDecoder.decode(token);
      String tokenId = jwt.getId();
      if (ObjectUtils.isEmpty(tokenId)) {
        return Optional.empty();
      }

      String userId = redis.opsForValue().get(sessionKey(tokenId));
      if (ObjectUtils.isEmpty(userId) || !ObjectUtils.equals(userId, jwt.getSubject())) {
        return Optional.empty();
      }

      List<GrantedAuthority> authorities = new ArrayList<>();
      String role = jwt.getClaimAsString(Constant.ROLE);
      if (ObjectUtils.isNotEmpty(role)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }
      List<String> permissions = jwt.getClaimAsStringList(Constant.PERMISSIONS);
      if (ObjectUtils.isNotEmpty(permissions)) {
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
      }

      return Optional.of(
          new AuthenticatedPrincipal(UUID.fromString(userId), tokenId, List.copyOf(authorities)));
    } catch (RuntimeException exception) {
      log.debug("Authentication failed for supplied token", exception);
      return Optional.empty();
    }
  }

  public String normalizeIdentifier(String value) {
    if (ObjectUtils.isEmpty(value)) {
      return value;
    }
    String normalized = Optional.of(value).map(String::trim).map(String::toLowerCase).get();

    try {
      PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
      String tempPhone = normalized;
      if (normalized.startsWith("^[1-9].*")) {
        tempPhone = "+" + tempPhone;
      }

      Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(tempPhone, Constant.VIETNAM);
      if (phoneUtil.isValidNumber(phoneNumber)) {
        String e164Number = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        return e164Number.replace("+", "");
      }
    } catch (NumberParseException e) {
      // Do nothing
    }

    return normalized;
  }

  public AuthChannel getChannel(String value) {
    if (EMAIL.matcher(value).matches()) {
      return AuthChannel.EMAIL;
    }
    if (PHONE.matcher(value).matches()) {
      return AuthChannel.PHONE;
    }
    return null;
  }

  private String registrationKey(String identifier) {
    try {
      byte[] digest =
          MessageDigest.getInstance(Constant.SHA_256)
              .digest(identifier.getBytes(StandardCharsets.UTF_8));
      return "auth:registration:" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private void issueOtp(AuthChannel channel, String identifier) {
    String code = String.format("%06d", random.nextInt(1_000_000));

    if (AuthChannel.EMAIL.equals(channel)) {
      notifications.sendEmail(
          NotificationTemplateCode.REGISTER_EMAIL_OTP, identifier, Map.of("otp", code));
    } else {
      notifications.sendSms(
          NotificationTemplateCode.REGISTER_SMS_OTP, identifier, Map.of("otp", code));
    }
    otps.save(
        new OtpChallenge(
            channel,
            identifier,
            OtpPurpose.REGISTER,
            hashOtp(code),
            Instant.now().plus(properties.getAuth().getOtpTtl())));
  }

  private Map<Object, Object> loadPendingRegistration(String identifier) {
    Map<Object, Object> pending = redis.opsForHash().entries(registrationKey(identifier));
    if (pending.isEmpty()) {
      throw new AuthException(ErrorCode.REGISTRATION_EXPIRED);
    }
    return pending;
  }

  private void validatePendingRegistration(String identifier, Map<Object, Object> pending) {
    if (!ObjectUtils.equals(identifier, String.valueOf(pending.get(Constant.IDENTIFIER)))) {
      throw new AuthException(ErrorCode.REGISTRATION_INVALID);
    }
  }

  private OtpChallenge findRegistrationOtp(String identifier) {
    return otps.findTopByIdentifierAndPurposeAndConsumedFalseOrderByExpiresAtDesc(
            identifier, OtpPurpose.REGISTER)
        .orElseThrow(() -> new AuthException(ErrorCode.OTP_INVALID));
  }

  private void validateOtp(String otp, OtpChallenge challenge) {
    if (challenge.getAttempts() >= properties.getAuth().getOtpMaxAttempts()) {
      throw new AuthException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
    }
    boolean expired = challenge.getExpiresAt().isBefore(Instant.now());
    boolean codeMatches =
        MessageDigest.isEqual(
            hashOtp(otp).getBytes(StandardCharsets.UTF_8),
            challenge.getCodeHash().getBytes(StandardCharsets.UTF_8));
    if (expired || !codeMatches) {
      otps.incrementAttempts(challenge.getId().toString());
      throw new OtpInvalidException();
    }
  }

  private User createUser(String identifier, Map<Object, Object> pending) {
    AuthChannel channel = getRegistrationChannel(pending);
    User user = new User(channel, identifier, String.valueOf(pending.get(Constant.PASSWORD_HASH)));
    user.verify();
    return user;
  }

  private AuthChannel getRegistrationChannel(Map<Object, Object> pending) {
    try {
      return AuthChannel.valueOf(String.valueOf(pending.get(Constant.CHANNEL)));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AuthException(ErrorCode.REGISTRATION_INVALID);
    }
  }

  private void saveVerifiedUser(User user, OtpChallenge challenge) {
    try {
      if (otps.consume(challenge.getId().toString()) != 1) {
        throw new AuthException(ErrorCode.OTP_INVALID);
      }
      users.saveAndFlush(user);
    } catch (DataIntegrityViolationException concurrentVerification) {
      // Two verify calls raced; the unique index on identifier settled it.
      throw new AuthException(ErrorCode.IDENTIFIER_ALREADY_REGISTERED);
    }
  }

  private Customer createCustomer(User user, Map<Object, Object> pending) {
    return new Customer(
        user, String.valueOf(pending.getOrDefault(Constant.DISPLAY_NAME, "")), Instant.now());
  }

  private void validateIdentifier(String identifier) {
    if (ObjectUtils.isEmpty(getChannel(identifier))) {
      throw new AuthException(ErrorCode.INVALID_IDENTIFIER);
    }
  }

  private String sessionKey(String tokenId) {
    return RedisKeyConstants.AUTH_TOKEN + tokenId;
  }

  private AuthResponse issueAccessToken(User user) {
    Duration ttl = properties.getAuth().getTokenTtl();
    Instant issuedAt = Instant.now();
    String tokenId = UUID.randomUUID().toString();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(tokenId)
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(ttl))
            .claim("token_type", "access")
            .claim("role", user.getRole().name())
            .claim(
                "permissions",
                RolePermissions.permissions(user.getRole()).stream().map(Enum::name).toList())
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    redis.opsForValue().set(sessionKey(tokenId), user.getId().toString(), ttl);
    return new AuthResponse(token, "Bearer", ttl.toSeconds());
  }

  private String hashOtp(String code) {
    try {
      byte[] digest =
          MessageDigest.getInstance(Constant.SHA_256).digest(code.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
