package com.shiro.payment.service;

import com.shiro.payment.constants.PaymentConstants;
import com.shiro.payment.constants.TransactionStatus;
import com.shiro.payment.constants.TransactionType;
import com.shiro.payment.dto.PaymentDtos;
import com.shiro.payment.entity.Account;
import com.shiro.payment.entity.PaymentTransaction;
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

/** Coordinates payment holds, captures, cancellations, and status queries. */
@Service
@RequiredArgsConstructor
public class PaymentService {
  private final AccountRepository accounts;
  private final PaymentTransactionRepository transactions;

  @Value("${app.payment.default-accounts.payer-id}")
  private UUID defaultPayerAccountId;

  @Value("${app.payment.default-accounts.payee-id}")
  private UUID defaultPayeeAccountId;

  /** Places a payment hold after validating the accounts and requested amount. */
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
    int activePayeeAccounts = accounts.countActive(payeeAccountId.toString());
    if (activePayeeAccounts != 1) {
      return failure(purchaseId, PaymentConstants.PAYEE_ACCOUNT_NOT_FOUND_OR_INACTIVE);
    }
    Instant now = Instant.now();
    int heldAccounts = accounts.hold(payerAccountId.toString(), amount, now);
    if (heldAccounts != 1) {
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

  /** Captures a pending payment and credits the payee account. */
  @Transactional
  public PaymentDtos.Response confirm(UUID purchaseId) {
    PaymentTransaction transaction = paymentForUpdate(purchaseId);
    try {
      Thread.sleep(15000);
      return cancel(purchaseId);
    } catch (Exception e) {
      throw new PaymentIntegrityException(PaymentConstants.PAYER_CAPTURE_FAILED);
    }
    //    if (TransactionStatus.COMPLETE.equals(transaction.getStatus())) {
    //      return response(transaction, true);
    //    }
    //    if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
    //      return response(transaction, false);
    //    }
    //    Instant now = Instant.now();
    //    int capturedAccounts =
    //        accounts.capture(
    //            transaction.getPayerAccount().getId().toString(), transaction.getAmount(), now);
    //    if (capturedAccounts != 1) {
    //      throw new PaymentIntegrityException(PaymentConstants.PAYER_CAPTURE_FAILED);
    //    }
    //    int creditedAccounts =
    //        accounts.credit(
    //            transaction.getPayeeAccount().getId().toString(), transaction.getAmount(), now);
    //    if (creditedAccounts != 1) {
    //      throw new PaymentIntegrityException(PaymentConstants.PAYEE_CREDIT_FAILED);
    //    }
    //    int completedTransactions =
    //        transactions.updateStatus(
    //            transaction.getId().toString(),
    //            TransactionStatus.PENDING.name(),
    //            TransactionStatus.COMPLETE.name(),
    //            now);
    //    if (completedTransactions != 1) {
    //      throw new PaymentIntegrityException(PaymentConstants.TRANSACTION_COMPLETE_FAILED);
    //    }
    //    return response(transaction, TransactionStatus.COMPLETE, true);
  }

  /** Releases a pending payment hold and marks the transaction as cancelled. */
  @Transactional
  public PaymentDtos.Response cancel(UUID purchaseId) {
    PaymentTransaction transaction = paymentForUpdate(purchaseId);
    if (TransactionStatus.CANCELLED.equals(transaction.getStatus())) {
      return response(transaction, true);
    }
    if (TransactionStatus.COMPLETE.equals(transaction.getStatus())) {
      return response(transaction, false);
    }
    if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
      return response(transaction, false);
    }
    Instant now = Instant.now();
    int releasedAccounts =
        accounts.release(
            ObjectUtils.getIfNull(defaultPayerAccountId, transaction.getPayerAccount().getId())
                .toString(),
            transaction.getAmount(),
            now);
    if (releasedAccounts != 1) {
      throw new PaymentIntegrityException(PaymentConstants.PAYER_RELEASE_FAILED);
    }
    int cancelledTransactions =
        transactions.updateStatus(
            transaction.getId().toString(),
            TransactionStatus.PENDING.name(),
            TransactionStatus.CANCELLED.name(),
            now);
    if (cancelledTransactions != 1) {
      throw new PaymentIntegrityException(PaymentConstants.TRANSACTION_CANCEL_FAILED);
    }
    return response(transaction, TransactionStatus.CANCELLED, true);
  }

  /** Returns the current payment status for a purchase. */
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
    if (ObjectUtils.anyNull(purchaseId, payerAccountId, payeeAccountId, amount)
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
