package com.shiro.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.shiro.payment.repository.AccountRepository;
import com.shiro.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PaymentServiceIntegrationTests {
  private static final UUID PAYER = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID PAYEE = UUID.fromString("11111111-0000-0000-0000-000000000002");

  @Autowired PaymentService paymentService;
  @Autowired AccountRepository accounts;
  @Autowired JdbcTemplate jdbc;

  @Test
  void holdAndConfirmMoveMoneyAtomically() {
    BigDecimal amount = new BigDecimal("125000.00");
    UUID purchaseId = UUID.randomUUID();

    assertThat(jdbc.queryForList("SELECT id FROM accounts", String.class))
        .contains(PAYER.toString(), PAYEE.toString());

    var pending = paymentService.pending(purchaseId, PAYER, PAYEE, amount);
    assertThat(pending.isSuccess()).as(pending.getMessage()).isTrue();
    assertThat(accounts.findById(PAYER).orElseThrow().getCurrentAmount())
        .isEqualByComparingTo("100000000.00");
    assertThat(accounts.findById(PAYER).orElseThrow().getAvailableAmount())
        .isEqualByComparingTo("99875000.00");

    var complete = paymentService.confirm(purchaseId);
    assertThat(complete.isSuccess()).isTrue();
    assertThat(accounts.findById(PAYER).orElseThrow().getCurrentAmount())
        .isEqualByComparingTo("99875000.00");
    assertThat(accounts.findById(PAYEE).orElseThrow().getCurrentAmount())
        .isEqualByComparingTo("125000.00");
    assertThat(accounts.findById(PAYEE).orElseThrow().getAvailableAmount())
        .isEqualByComparingTo("125000.00");
  }
}
