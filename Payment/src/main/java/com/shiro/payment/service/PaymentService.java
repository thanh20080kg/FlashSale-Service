package com.shiro.payment.service;

import com.shiro.payment.constants.PaymentConstants;
import com.shiro.payment.domain.Account;
import com.shiro.payment.domain.PaymentTransaction;
import com.shiro.payment.domain.TransactionStatus;
import com.shiro.payment.domain.TransactionType;
import com.shiro.payment.messaging.PaymentDtos;
import com.shiro.payment.repository.AccountRepository;
import com.shiro.payment.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private final AccountRepository accounts;
  private final PaymentTransactionRepository transactions;

  @Value("${app.payment.default-accounts.payer-id}")
  private UUID defaultPayerAccountId;

  @Value("${app.payment.default-accounts.payee-id}")
  private UUID defaultPayeeAccountId;

  @Transactional
  public PaymentDtos.Response pending(
      UUID purchaseId, UUID payerAccountId, UUID payeeAccountId, BigDecimal amount) {
    // Demo account because we don't have a real payment service
    payerAccountId = ObjectUtils.getIfNull(defaultPayerAccountId, payerAccountId);
    payeeAccountId = ObjectUtils.getIfNull(defaultPayeeAccountId, payeeAccountId);

    validate(purchaseId, payerAccountId, payeeAccountId, amount);
    var existing = transactions.findByPurchaseIdAndType(purchaseId, TransactionType.PAYMENT);
    if (existing.isPresent()) {
      return response(existing.get(), existing.get().getStatus() != TransactionStatus.FAILED);
    }

    if (payerAccountId.equals(payeeAccountId)) {
      return failure(purchaseId, PaymentConstants.SAME_PAYMENT_ACCOUNT);
    }
    if (accounts.countActive(payeeAccountId.toString()) != 1) {
      return failure(purchaseId, PaymentConstants.PAYEE_ACCOUNT_NOT_FOUND_OR_INACTIVE);
    }
    Instant now = Instant.now();
    if (accounts.hold(payerAccountId.toString(), amount, now) != 1) {
      return failure(purchaseId, PaymentConstants.PAYER_ACCOUNT_INACTIVE_OR_INSUFFICIENT_FUNDS);
    }

    Account account = accounts.getReferenceById(payerAccountId);
    Account payee = accounts.getReferenceById(payeeAccountId);
    PaymentTransaction transaction =
        transactions.save(
            new PaymentTransaction(
                purchaseId,
                account,
                payee,
                amount,
                TransactionType.PAYMENT,
                null,
                TransactionStatus.PENDING,
                now));
    return response(transaction, true);
  }

  @Transactional
  public PaymentDtos.Response confirm(UUID purchaseId) {
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    PaymentTransaction transaction = paymentForUpdate(purchaseId);
    if (transaction.getStatus() == TransactionStatus.COMPLETE) {
      return response(transaction, true);
    }
    if (transaction.getStatus() != TransactionStatus.PENDING) {
      return response(transaction, false);
    }
    Instant now = Instant.now();
    if (accounts.capture(
            transaction.getPayerAccount().getId().toString(), transaction.getAmount(), now)
        != 1) {
      throw new PaymentIntegrityException(PaymentConstants.PAYER_CAPTURE_FAILED);
    }
    if (accounts.credit(
            transaction.getPayeeAccount().getId().toString(), transaction.getAmount(), now)
        != 1) {
      throw new PaymentIntegrityException(PaymentConstants.PAYEE_CREDIT_FAILED);
    }
    if (transactions.updateStatus(
            transaction.getId().toString(),
            TransactionStatus.PENDING.name(),
            TransactionStatus.COMPLETE.name(),
            now)
        != 1) {
      throw new PaymentIntegrityException(PaymentConstants.TRANSACTION_COMPLETE_FAILED);
    }
    return response(transaction, TransactionStatus.COMPLETE, true);
  }

  @Transactional
  public PaymentDtos.Response cancel(UUID purchaseId) {
    PaymentTransaction transaction = paymentForUpdate(purchaseId);
    if (transaction.getStatus() == TransactionStatus.CANCELLED) {
      return response(transaction, true);
    }
    if (transaction.getStatus() == TransactionStatus.COMPLETE) {
      return response(transaction, false);
    }
    if (transaction.getStatus() != TransactionStatus.PENDING) {
      return response(transaction, false);
    }
    Instant now = Instant.now();
    if (accounts.release(
            transaction.getPayerAccount().getId().toString(), transaction.getAmount(), now)
        != 1) {
      throw new PaymentIntegrityException(PaymentConstants.PAYER_RELEASE_FAILED);
    }
    Account account = transaction.getPayerAccount();
    if (transactions.updateStatus(
            transaction.getId().toString(),
            TransactionStatus.PENDING.name(),
            TransactionStatus.CANCELLED.name(),
            now)
        != 1) {
      throw new PaymentIntegrityException(PaymentConstants.TRANSACTION_CANCEL_FAILED);
    }
    return response(transaction, TransactionStatus.CANCELLED, true);
  }

  @Transactional(readOnly = true)
  public PaymentDtos.Response status(UUID purchaseId) {
    return transactions
        .findByPurchaseIdAndType(purchaseId, TransactionType.PAYMENT)
        .map(
            transaction ->
                response(transaction, transaction.getStatus() != TransactionStatus.FAILED))
        .orElseGet(() -> failure(purchaseId, PaymentConstants.TRANSACTION_NOT_FOUND));
  }

  private PaymentTransaction payment(UUID purchaseId) {
    return transactions
        .findByPurchaseIdAndType(purchaseId, TransactionType.PAYMENT)
        .orElseThrow(() -> new IllegalArgumentException(PaymentConstants.TRANSACTION_NOT_FOUND));
  }

  private PaymentTransaction paymentForUpdate(UUID purchaseId) {
    return transactions
        .findForUpdate(purchaseId, TransactionType.PAYMENT)
        .orElseThrow(() -> new IllegalArgumentException(PaymentConstants.TRANSACTION_NOT_FOUND));
  }

  private void validate(
      UUID purchaseId, UUID payerAccountId, UUID payeeAccountId, BigDecimal amount) {
    if (purchaseId == null
        || payerAccountId == null
        || payeeAccountId == null
        || amount == null
        || amount.signum() <= 0) {
      throw new IllegalArgumentException(PaymentConstants.INVALID_REQUEST);
    }
  }

  private PaymentDtos.Response response(PaymentTransaction tx, boolean success) {
    return response(tx, tx.getStatus(), success);
  }

  private PaymentDtos.Response response(
      PaymentTransaction tx, TransactionStatus status, boolean success) {
    return new PaymentDtos.Response(success, tx.getPurchaseId(), tx.getId(), status.name(), null);
  }

  private PaymentDtos.Response failure(UUID purchaseId, String message) {
    return new PaymentDtos.Response(
        false, purchaseId, null, TransactionStatus.FAILED.name(), message);
  }

  private static class PaymentIntegrityException extends RuntimeException {
    PaymentIntegrityException(String message) {
      super(message);
    }
  }
}
