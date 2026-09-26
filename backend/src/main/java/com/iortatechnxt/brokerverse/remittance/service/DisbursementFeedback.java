package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.NegativeAdjustmentPending;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.BatchSettlement;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to the other Operations teams through the ledger events, after their commit and in a new
 * transaction:
 *
 * <ul>
 *   <li>Disbursement status of a remittance payment request (RMTID.019/034): with the DV number the
 *       invoices become PARTIALLY_REMITTED or FULLY_REMITTED (DTIP balance left or not), their
 *       remittance lock is released and the batch moves on; a returned request is notified to the
 *       processors.
 *   <li>A cancelled DV (DIS 2.20.0; the event's reason is the cancellation reason): the postings of
 *       the batch's cycle are reversed and the batch returns to review, to be sent again ({@link
 *       BatchCancellation}); when another team locked one of its invoices since, the batch is left
 *       as it is and the processors are told why.
 *   <li>A negative adjustment raised on an invoice (RMTID.020/035) is notified to the Remittance
 *       Team, with the batch the invoice is in.
 * </ul>
 *
 * <p>Only the events of the batch's current payment request count: after a cancelled DV the next
 * request is sent as {@code <batch>/R<n>}, and events of the earlier request are ignored.
 */
@Component
public class DisbursementFeedback {

  private static final String DECIDED = "REMIT_BATCH_DECIDED";
  private static final String BATCHES = "/remittance/batches/";
  private static final String DV_OF = "DV of ";

  private final RemittanceBatchRepository batches;
  private final BatchLineRepository lines;
  private final BatchRemittance remittance;
  private final BatchCancellation cancellation;
  private final NotificationService notifications;
  private final AuditTrailService audit;

  /**
   * Creates the listener.
   *
   * @param batches batches
   * @param lines batch lines
   * @param remittance DV assigned
   * @param cancellation DV cancelled
   * @param notifications notifications
   * @param audit audit trail
   */
  public DisbursementFeedback(
      RemittanceBatchRepository batches,
      BatchLineRepository lines,
      BatchRemittance remittance,
      BatchCancellation cancellation,
      NotificationService notifications,
      AuditTrailService audit) {
    this.batches = batches;
    this.lines = lines;
    this.remittance = remittance;
    this.cancellation = cancellation;
    this.notifications = notifications;
    this.audit = audit;
  }

  /**
   * A remittance payment request changed status in Disbursement.
   *
   * @param event status change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void on(DisbursementStatusChanged event) {
    if (RemittanceSettings.MODULE.equals(event.sourceModule())) {
      batches
          .findByBatchNo(BatchSettlement.batchNoOf(event.sourceRef()))
          .filter(b -> event.sourceRef().equals(b.cycleReference()))
          .ifPresent(b -> changed(b, event));
    }
  }

  private void changed(RemittanceBatch batch, DisbursementStatusChanged event) {
    batch.disbursement(event.status().name(), event.dvNo());
    audit.record(
        BatchService.ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        "Disbursement " + event.requestNo() + " " + event.status());
    if (event.status() == DisbursementRequest.Status.DV_ASSIGNED
        && batch.getStage() == BatchStage.APPROVED) {
      remittance.remitted(batch, "DV " + event.dvNo());
    } else if (event.status() == DisbursementRequest.Status.RETURNED) {
      notifyProcessors(batch, "Payment request of " + batch.getBatchNo() + " returned", event);
    } else if (event.status() == DisbursementRequest.Status.CANCELLED
        && BatchCancellation.restorable(batch)) {
      cancelled(batch, event);
    }
  }

  private void cancelled(RemittanceBatch batch, DisbursementStatusChanged event) {
    List<String> blockers = cancellation.blockers(batch);
    if (blockers.isEmpty()) {
      cancellation.restore(batch, event.dvNo(), event.reason());
      notifyProcessors(
          batch, DV_OF + batch.getBatchNo() + " cancelled - batch back to review", event);
    } else {
      notifications.notifyPermission(
          ExtractionService.PROCESSORS,
          new Notice(
              DV_OF + batch.getBatchNo() + " cancelled - restore blocked",
              "Invoices locked by another team: " + String.join(", ", blockers),
              BATCHES + batch.getId(),
              BatchService.ENTITY,
              batch.getId().toString()),
          DECIDED);
    }
  }

  private void notifyProcessors(
      RemittanceBatch batch, String title, DisbursementStatusChanged event) {
    notifications.notifyPermission(
        ExtractionService.PROCESSORS,
        new Notice(
            title,
            event.reason(),
            BATCHES + batch.getId(),
            BatchService.ENTITY,
            batch.getId().toString()),
        DECIDED);
  }

  /**
   * A negative adjustment was raised on an invoice (RMTID.035).
   *
   * @param event pending negative adjustment
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void on(NegativeAdjustmentPending event) {
    if (!event.pending()) {
      return;
    }
    String batchNo =
        lines.findByInvoiceNo(event.invoiceNo()).stream()
            .map(BatchLine::getBatch)
            .filter(b -> b.getStage().isOpen())
            .map(RemittanceBatch::getBatchNo)
            .findFirst()
            .orElse(null);
    notifications.notifyPermission(
        ExtractionService.PROCESSORS,
        new Notice(
            "Negative adjustment pending on " + event.invoiceNo(),
            "Request "
                + event.requestNo()
                + " by "
                + event.requestedBy()
                + (batchNo == null ? "" : "; invoice is in batch " + batchNo),
            "/operations/invoices/" + event.invoiceNo(),
            "OpsInvoice",
            event.invoiceNo()),
        "REMIT_NEG_ADJ_PENDING");
  }
}
