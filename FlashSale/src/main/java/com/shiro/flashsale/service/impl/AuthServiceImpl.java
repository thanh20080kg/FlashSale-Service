package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.AuthChannel;
import com.shiro.flashsale.constants.NotificationTemplateCode;
import com.shiro.flashsale.constants.OtpPurpose;
import com.shiro.flashsale.dto.AuthDtos;
import com.shiro.flashsale.entity.Customer;
import com.shiro.flashsale.entity.OtpChallenge;
import com.shiro.flashsale.entity.User;
import com.shiro.flashsale.exception.AuthException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.CustomerRepository;
import com.shiro.flashsale.repository.OtpChallengeRepository;
import com.shiro.flashsale.repository.UserRepository;
import com.shiro.flashsale.security.AuthenticatedPrincipal;
import com.shiro.flashsale.security.RateLimiter;
import com.shiro.flashsale.security.RolePermissions;
import com.shiro.flashsale.service.AuthService;
import com.shiro.flashsale.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final Pattern PHONE = Pattern.compile("^\\+?\\d{8,15}$");

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

  public AuthServiceImpl(
      UserRepository users,
      CustomerRepository customers,
      OtpChallengeRepository otps,
      PasswordEncoder encoder,
      StringRedisTemplate redis,
      NotificationService notifications,
      JwtEncoder jwtEncoder,
      JwtDecoder jwtDecoder,
      RateLimiter rateLimiter,
      AppProperties properties) {
    this.users = users;
    this.customers = customers;
    this.otps = otps;
    this.encoder = encoder;
    this.redis = redis;
    this.notifications = notifications;
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.rateLimiter = rateLimiter;
    this.properties = properties;
  }

  // ---------------------------------------------------------------- registration

  public static String normalize(String value) {
    return value.trim().toLowerCase();
  }

  public static AuthChannel channel(String value) {
    if (EMAIL.matcher(value).matches()) return AuthChannel.EMAIL;
    if (PHONE.matcher(value).matches()) return AuthChannel.PHONE;
    throw new AuthException(ErrorCode.INVALID_IDENTIFIER);
  }

  // ---------------------------------------------------------------- session

  private static String sessionKey(String tokenId) {
    return "auth:token:" + tokenId;
  }

  private static String registrationKey(String identifier) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(identifier.getBytes(StandardCharsets.UTF_8));
      return "auth:registration:" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  /**
   * Registration does not create a user row. The pending signup is parked in Redis under a hashed
   * key until the OTP proves the identifier belongs to the caller, which means an attacker cannot
   * squat identifiers or probe which ones exist by watching the database.
   */
  @Override
  public void register(AuthDtos.RegisterRequest request) {
    String identifier = normalize(request.identifier());
    AuthChannel channel = channel(identifier);
    rateLimiter.consume("auth-register-identifier", identifier);

    if (users.existsByIdentifier(identifier))
      throw new AuthException(ErrorCode.IDENTIFIER_ALREADY_REGISTERED);

    Map<String, String> pending = new HashMap<>();
    pending.put("identifier", identifier);
    pending.put("channel", channel.name());
    pending.put("passwordHash", encoder.encode(request.password()));
    pending.put("displayName", request.displayName() == null ? "" : request.displayName());

    String key = registrationKey(identifier);
    redis.opsForHash().putAll(key, pending);
    redis.expire(key, properties.getAuth().getRegistrationTtl());

    issueOtp(channel, identifier);
  }

  // ---------------------------------------------------------------- helpers

  @Override
  @Transactional
  public void verifyOtp(AuthDtos.VerifyOtpRequest request) {
    String identifier = normalize(request.identifier());
    channel(identifier);
    rateLimiter.consume("auth-otp-identifier", identifier);

    OtpChallenge challenge =
        otps.findTopByIdentifierAndPurposeAndConsumedFalseOrderByExpiresAtDesc(
                identifier, OtpPurpose.REGISTER)
            .orElseThrow(() -> new AuthException(ErrorCode.OTP_INVALID));

    if (challenge.getAttempts() >= properties.getAuth().getOtpMaxAttempts()) {
      challenge.consume();
      otps.save(challenge);
      throw new AuthException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
    }
    if (challenge.getExpiresAt().isBefore(Instant.now())
        || !MessageDigest.isEqual(
            hashOtp(request.otp()).getBytes(StandardCharsets.UTF_8),
            challenge.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
      // Persisting the failure is what actually bounds brute force; the Redis limiter above only
      // slows it down.
      challenge.registerFailedAttempt();
      otps.save(challenge);
      throw new AuthException(ErrorCode.OTP_INVALID);
    }

    Map<Object, Object> pending = redis.opsForHash().entries(registrationKey(identifier));
    if (pending.isEmpty()) throw new AuthException(ErrorCode.REGISTRATION_EXPIRED);
    if (!identifier.equals(String.valueOf(pending.get("identifier"))))
      throw new AuthException(ErrorCode.REGISTRATION_INVALID);

    AuthChannel channel;
    try {
      channel = AuthChannel.valueOf(String.valueOf(pending.get("channel")));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AuthException(ErrorCode.REGISTRATION_INVALID);
    }

    challenge.consume();
    otps.save(challenge);

    User user = new User(channel, identifier, String.valueOf(pending.get("passwordHash")));
    user.verify();
    try {
      users.saveAndFlush(user);
    } catch (DataIntegrityViolationException concurrentVerification) {
      // Two verify calls raced; the unique index on identifier settled it.
      throw new AuthException(ErrorCode.IDENTIFIER_ALREADY_REGISTERED);
    }
    customers.save(
        new Customer(user, String.valueOf(pending.getOrDefault("displayName", "")), Instant.now()));
    redis.delete(registrationKey(identifier));
  }

  @Override
  public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
    String identifier = normalize(request.identifier());
    channel(identifier);
    rateLimiter.consume("auth-login-identifier", identifier);

    Optional<User> found = users.findByIdentifier(identifier);
    if (found.isEmpty()) {
      // Spend comparable CPU on a miss so response time does not reveal whether the account exists.
      encoder.matches(
          request.password(), "$2a$10$7EqJtq98hPqEX7fNZaFWoOa1u8Nn5Wl2h9r3Z6oQyH1x9k7bC1sJK");
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    User user = found.get();
    if (!encoder.matches(request.password(), user.getPasswordHash()))
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    if (!user.isVerified()) throw new AuthException(ErrorCode.IDENTIFIER_NOT_VERIFIED);

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

    // The JWT is only accepted while its id is present in Redis, so logout is instant and the
    // session is shared by every instance rather than pinned to one.
    redis.opsForValue().set(sessionKey(tokenId), user.getId().toString(), ttl);
    return new AuthDtos.AuthResponse(token, "Bearer", ttl.toSeconds());
  }

  @Override
  public void logout(String token) {
    if (token == null || token.isBlank()) return;
    try {
      Jwt jwt = jwtDecoder.decode(token);
      if (jwt.getId() != null) redis.delete(sessionKey(jwt.getId()));
    } catch (RuntimeException ignored) {
      // Logout stays idempotent for invalid or already-expired tokens.
    }
  }

  @Override
  public Optional<AuthenticatedPrincipal> authenticate(String token) {
    if (token == null || token.isBlank()) return Optional.empty();
    try {
      Jwt jwt = jwtDecoder.decode(token);
      String tokenId = jwt.getId();
      if (tokenId == null) return Optional.empty();

      String userId = redis.opsForValue().get(sessionKey(tokenId));
      if (userId == null || !userId.equals(jwt.getSubject())) return Optional.empty();

      List<GrantedAuthority> authorities = new ArrayList<>();
      String role = jwt.getClaimAsString("role");
      if (role != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      List<String> permissions = jwt.getClaimAsStringList("permissions");
      if (permissions != null)
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));

      return Optional.of(
          new AuthenticatedPrincipal(UUID.fromString(userId), tokenId, List.copyOf(authorities)));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  private void issueOtp(AuthChannel channel, String identifier) {
    // Any earlier code for this identifier stops working the moment a new one is issued.
    otps.consumeOpenChallenges(identifier, OtpPurpose.REGISTER);

    String code = String.format("%06d", random.nextInt(1_000_000));
    otps.save(
        new OtpChallenge(
            channel,
            identifier,
            OtpPurpose.REGISTER,
            hashOtp(code),
            Instant.now().plus(properties.getAuth().getOtpTtl())));

    String template =
        channel == AuthChannel.EMAIL
            ? NotificationTemplateCode.REGISTER_EMAIL_OTP
            : NotificationTemplateCode.REGISTER_SMS_OTP;
    if (channel == AuthChannel.EMAIL)
      notifications.sendEmail(template, identifier, Map.of("otp", code));
    else notifications.sendSms(template, identifier, Map.of("otp", code));
  }

  /**
   * A 6-digit code has only a million values, so a slow hash buys nothing an attempt counter does
   * not already provide - and BCrypt on the OTP path would cost ~100ms of CPU per registration.
   * SHA-256 keeps the code out of the database in the clear at negligible cost.
   */
  private String hashOtp(String code) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
