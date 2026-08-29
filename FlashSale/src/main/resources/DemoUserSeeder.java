package com.shiro.flashsale.bootstrap;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.AuthChannel;
import com.shiro.flashsale.constants.Role;
import com.shiro.flashsale.entity.Customer;
import com.shiro.flashsale.entity.User;
import com.shiro.flashsale.repository.CustomerRepository;
import com.shiro.flashsale.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a handful of ready-to-use accounts so the service can be exercised straight after
 * {@code docker compose up}.
 *
 * <p>Accounts are seeded in code rather than in Liquibase because their password hashes must be
 * produced by the configured {@link PasswordEncoder} - committing a fixed BCrypt hash would pin the
 * cost factor and put a real credential in version control. It is off unless
 * {@code app.seed.demo-users} is enabled, and it is idempotent.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-users", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

  private record DemoAccount(String identifier, AuthChannel channel, Role role, String displayName) {}

  private static final List<DemoAccount> ACCOUNTS =
      List.of(
          new DemoAccount("admin@flashsale.local", AuthChannel.EMAIL, Role.ADMIN, "Platform Admin"),
          new DemoAccount("seller@flashsale.local", AuthChannel.EMAIL, Role.SELLER, "Demo Seller"),
          new DemoAccount("buyer1@flashsale.local", AuthChannel.EMAIL, Role.BUYER, "Buyer One"),
          new DemoAccount("buyer2@flashsale.local", AuthChannel.EMAIL, Role.BUYER, "Buyer Two"),
          new DemoAccount("+84900000001", AuthChannel.PHONE, Role.BUYER, "Phone Buyer"));

  private final UserRepository users;
  private final CustomerRepository customers;
  private final PasswordEncoder encoder;
  private final AppProperties properties;
  private final Clock clock;

  public DemoUserSeeder(
      UserRepository users,
      CustomerRepository customers,
      PasswordEncoder encoder,
      AppProperties properties) {
    this.users = users;
    this.customers = customers;
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String passwordHash = encoder.encode(properties.getSeed().getDemoUserPassword());
    BigDecimal balance = properties.getSeed().getDemoUserBalance();

    for (DemoAccount account : ACCOUNTS) {
      if (users.existsByIdentifier(account.identifier())) continue;

      User user = new User(account.channel(), account.identifier(), passwordHash);
      user.verify();
      user.assignRole(account.role());
      users.save(user);

      Customer customer =
          customers.save(new Customer(user, account.displayName(), clock.instant()));
      customers.credit(customer.getId(), balance);
      log.info("Seeded demo account {} with role {}", account.identifier(), account.role());
    }
  }
}
