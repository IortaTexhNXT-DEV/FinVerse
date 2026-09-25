package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Numbers, approver notifications and audit entries of the payment reversal requests (ACSL
 * 2.6.0-2.6.1), kept apart from the posting logic of {@link CashieringPaymentReversals}.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class PaymentReversalSupport {

  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param numbers document numbers
   * @param notifications notifications
   * @param audit audit trail
   * @param clock clock
   */
  public PaymentReversalSupport(
      DocumentNumberService numbers,
      NotificationService notifications,
      AuditTrailService audit,
      Clock clock) {
    this.numbers = numbers;
    this.notifications = notifications;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The next request number.
   *
   * @return {@code PRV-<yyyy>-nnnnnn}
   */
  public String nextNumber() {
    return numbers.next("PRV-" + LocalDate.now(clock).getYear());
  }

  /**
   * Notifies the cashiering approvers ({@code CASH_APPROVE}).
   *
   * @param notice content
   */
  public void notifyApprovers(Notice notice) {
    notifications.notifyPermission("CASH_APPROVE", notice);
  }

  /**
   * Records an audit entry of a request.
   *
   * @param r request
   * @param action action
   * @param detail detail
   */
  public void audit(PaymentReversal r, AuditAction action, String detail) {
    audit.record(CashieringPaymentReversals.ENTITY, r.getRequestNo(), action, detail);
  }
}
