package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.IncentiveStatus;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveTrigger;
import com.iortatechnxt.brokerverse.booking.service.IncentiveEvaluationService;
import com.iortatechnxt.brokerverse.booking.service.IncentiveEvaluationService.IncentiveDecided;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The incentive indicator and the invoice ledger (FR-NB-118, FR-RN-087): when a movement leaves an
 * invoice fully paid, booking evaluates the indicator of its transaction; the indicator decided is
 * copied onto the ledger invoices of the family. Failures are logged; a later movement evaluates
 * again.
 */
@Component
public class IncentiveLedgerListener {

  private static final Logger LOG = LoggerFactory.getLogger(IncentiveLedgerListener.class);

  private final OpsInvoiceRepository invoices;
  private final IncentiveEvaluationService incentives;
  private final TransactionTemplate tx;

  /**
   * Creates the listener.
   *
   * @param invoices the invoice ledger
   * @param incentives incentive evaluation
   * @param txManager transactions
   */
  public IncentiveLedgerListener(
      OpsInvoiceRepository invoices,
      IncentiveEvaluationService incentives,
      PlatformTransactionManager txManager) {
    this.invoices = invoices;
    this.incentives = incentives;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * A movement was posted: a fully paid invoice whose transaction is pending is evaluated.
   *
   * @param event movement
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(InvoiceMovementPosted event) {
    try {
      tx.executeWithoutResult(s -> evaluateWhenPaid(event.invoiceNo()));
    } catch (RuntimeException e) {
      LOG.warn("Incentive of invoice {} not evaluated: {}", event.invoiceNo(), e.getMessage());
    }
  }

  private void evaluateWhenPaid(String invoiceNo) {
    Optional<OpsInvoice> invoice = invoices.findByInvoiceNo(invoiceNo);
    if (invoice.isEmpty() || invoice.get().getPaymentStatus() != PaymentStatus.PAID) {
      return;
    }
    String root =
        invoice.get().getRootInvoiceNo() == null ? invoiceNo : invoice.get().getRootInvoiceNo();
    boolean pending =
        incentives.history(root).stream()
            .noneMatch(
                e ->
                    e.getTrigger() == IncentiveTrigger.FULL_PAYMENT
                        && e.getResult() != IncentiveStatus.PENDING);
    if (pending) {
      incentives.evaluate(root, IncentiveTrigger.FULL_PAYMENT);
    }
  }

  /**
   * The indicator of a transaction was decided: the ledger invoices of the family carry it.
   *
   * @param event indicator decided
   */
  @EventListener
  public void on(IncentiveDecided event) {
    for (String invoiceNo : event.invoiceNos()) {
      invoices.setIncentiveEligible(invoiceNo, event.eligible());
    }
  }
}
