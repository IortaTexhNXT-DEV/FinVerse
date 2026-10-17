package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Drives {@link RenewalProgression} from the events of the renewal and account workflows and of
 * booking: acceptance (fast track), ready for placement (automatic slip), policy issued (booking
 * queue, in the issuing transaction) and invoice booked (RENEWED). Steps after a commit run in
 * their own transaction; a failure is recorded on the renewal for Processing.
 */
@Component
public class RenewalProgressionListener {

  private static final Logger LOG = LoggerFactory.getLogger(RenewalProgressionListener.class);
  private static final String ACCOUNT_WORKFLOW = "NB_ACCOUNT";

  private final RenewalProgression progression;
  private final TransactionTemplate tx;

  /**
   * Creates the listener.
   *
   * @param progression progression
   * @param txManager transactions
   */
  public RenewalProgressionListener(
      RenewalProgression progression, PlatformTransactionManager txManager) {
    this.progression = progression;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * After a committed stage change: fast track of an accepted renewal, automatic placement.
   *
   * @param event stage change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void afterCommit(WorkCaseTransitioned event) {
    if (RenewalCodes.WORKFLOW.equals(event.workflowCode())
        && RenewalStage.ACCEPTED.name().equals(event.toStage())) {
      Long id = Long.valueOf(event.entityId());
      run(() -> progression.fastTrack(id), id, "Fast track of the renewal account failed: ");
    } else if (ACCOUNT_WORKFLOW.equals(event.workflowCode())
        && "READY_FOR_PLACEMENT".equals(event.toStage())) {
      Long accountId = Long.valueOf(event.entityId());
      try {
        tx.executeWithoutResult(s -> progression.place(accountId));
      } catch (RuntimeException e) {
        LOG.warn("Automatic placement of account {} failed: {}", accountId, e.getMessage());
      }
    }
  }

  /**
   * In the issuing transaction: queues an issued renewal account for booking.
   *
   * @param event stage change
   */
  @EventListener
  public void inTransaction(WorkCaseTransitioned event) {
    if (ACCOUNT_WORKFLOW.equals(event.workflowCode()) && "POLICY_ISSUED".equals(event.toStage())) {
      progression.issued(Long.valueOf(event.entityId()));
    }
  }

  /**
   * A committed booking closes the renewal it renews.
   *
   * @param event booked invoice
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(InvoiceBooked event) {
    if (event.kind() != InvoiceKind.BOOKING || event.arn() == null) {
      return;
    }
    try {
      tx.executeWithoutResult(s -> progression.booked(event.arn(), event.invoiceNo()));
    } catch (RuntimeException e) {
      LOG.warn("Renewal of account {} not closed: {}", event.arn(), e.getMessage());
    }
  }

  private void run(Runnable step, Long candidateId, String failure) {
    try {
      tx.executeWithoutResult(s -> step.run());
    } catch (RuntimeException e) {
      LOG.warn("Renewal {}: {}{}", candidateId, failure, e.getMessage());
      try {
        tx.executeWithoutResult(s -> progression.failed(candidateId, failure + e.getMessage()));
      } catch (RuntimeException ignored) {
        LOG.warn("Renewal {}: failure not recorded", candidateId);
      }
    }
  }
}
