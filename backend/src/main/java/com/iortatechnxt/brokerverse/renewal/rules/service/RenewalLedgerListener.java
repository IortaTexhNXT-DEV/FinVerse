package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the checks of an open renewal again when its expiring account changes in the Operations
 * ledger (BRRN.022): an endorsement or cancellation booked on the account, or a payment or
 * adjustment posted on its expiring invoice. Failures are logged; the nightly run catches up.
 */
@Component
public class RenewalLedgerListener {

  private static final Logger LOG = LoggerFactory.getLogger(RenewalLedgerListener.class);

  private final RenewalCandidateRepository candidates;
  private final ReevaluationService reevaluation;
  private final TransactionTemplate tx;

  /**
   * Creates the listener.
   *
   * @param candidates renewals
   * @param reevaluation checks
   * @param txManager transactions
   */
  public RenewalLedgerListener(
      RenewalCandidateRepository candidates,
      ReevaluationService reevaluation,
      PlatformTransactionManager txManager) {
    this.candidates = candidates;
    this.reevaluation = reevaluation;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * An invoice was booked on an account: its open renewals are evaluated again.
   *
   * @param event invoice copied into the ledger
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OpsInvoiceBooked event) {
    if (event.arn() == null) {
      return;
    }
    run(event.invoiceNo(), () -> candidates.findByExpiringArn(event.arn()));
  }

  /**
   * A movement was posted on an invoice: the renewal of that expiring invoice is evaluated again.
   *
   * @param event movement
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(InvoiceMovementPosted event) {
    run(
        event.invoiceNo(),
        () ->
            candidates
                .findByCompanyIdAndExpiringInvoiceNo(event.companyId(), event.invoiceNo())
                .stream()
                .toList());
  }

  private void run(String invoiceNo, java.util.function.Supplier<List<RenewalCandidate>> find) {
    try {
      tx.executeWithoutResult(
          s ->
              find.get().stream()
                  .filter(c -> c.getStage().isOpen())
                  .forEach(c -> reevaluation.reevaluate(c, CheckTrigger.EVENT)));
    } catch (RuntimeException e) {
      LOG.warn("Renewal checks after invoice {} not run: {}", invoiceNo, e.getMessage());
    }
  }
}
