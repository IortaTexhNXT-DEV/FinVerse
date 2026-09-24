package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.NegativeAdjustmentPending;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
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
 *       processors (re-sending waits for the Disbursement BRD, OQ02).
 *   <li>A negative adjustment raised on an invoice (RMTID.020/035) is notified to the Remittance
 *       Team, with the batch the invoice is in.
 * </ul>
 */
@Component
public class DisbursementFeedback {

  private final RemittanceBatchRepository batches;
  private final BatchLineRepository lines;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final AuditTrailService audit;

  /**
   * Creates the listener.
   *
   * @param batches batches
   * @param lines batch lines
   * @param ledger ledger reads
   * @param writer ledger statuses and locks
   * @param workflow batch workflow
   * @param notifications notifications
   * @param audit audit trail
   */
  public DisbursementFeedback(
      RemittanceBatchRepository batches,
      BatchLineRepository lines,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      WorkflowService workflow,
      NotificationService notifications,
      AuditTrailService audit) {
    this.batches = batches;
    this.lines = lines;
    this.ledger = ledger;
    this.writer = writer;
    this.workflow = workflow;
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
      batches.findByBatchNo(event.sourceRef()).ifPresent(b -> changed(b, event));
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
      remitted(batch, event.dvNo());
    } else if (event.status() == DisbursementRequest.Status.RETURNED) {
      notifications.notifyPermission(
          ExtractionService.PROCESSORS,
          new Notice(
              "Payment request of " + batch.getBatchNo() + " returned",
              event.reason(),
              "/remittance/batches/" + batch.getId(),
              BatchService.ENTITY,
              batch.getId().toString()),
          "REMIT_BATCH_DECIDED");
    }
  }

  private void remitted(RemittanceBatch batch, String dvNo) {
    boolean full = true;
    for (BatchLine line : batch.included()) {
      OpsInvoice invoice = ledger.require(line.getInvoiceNo());
      boolean cleared = invoice.component(LedgerComponent.DTIP).getBalance().signum() <= 0;
      RemittanceStatus status =
          cleared ? RemittanceStatus.FULLY_REMITTED : RemittanceStatus.PARTIALLY_REMITTED;
      full &= cleared;
      String why = "DV " + dvNo + " of " + batch.getBatchNo();
      writer.setRemittanceStatus(line.getInvoiceNo(), status, RemittanceSettings.MODULE, why);
      writer.unlock(line.getInvoiceNo(), RemittanceSettings.MODULE, why);
      line.remitted(status);
    }
    workflow.systemTransition(
        BatchService.ENTITY,
        batch.getId().toString(),
        full ? "dv_full" : "dv_partial",
        TransitionNote.comment("DV " + dvNo));
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
