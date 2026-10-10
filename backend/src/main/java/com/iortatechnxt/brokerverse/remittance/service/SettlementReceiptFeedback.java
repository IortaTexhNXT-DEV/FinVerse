package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.SettlementReceiptIssued;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Records on the remittance batch the commission or incentive OR that Cashiering issued once
 * Disbursement approved the payment request of the batch (FRS.CSH.07.01.01; Appendix R, C12).
 */
@Component
public class SettlementReceiptFeedback {

  private final RemittanceBatchRepository batches;
  private final AuditTrailService audit;

  /**
   * Creates the listener.
   *
   * @param batches batches
   * @param audit audit trail
   */
  public SettlementReceiptFeedback(RemittanceBatchRepository batches, AuditTrailService audit) {
    this.batches = batches;
    this.audit = audit;
  }

  /**
   * An OR of a remittance batch was issued.
   *
   * @param event OR issued
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void on(SettlementReceiptIssued event) {
    if (!RemittanceSettings.MODULE.equals(event.sourceModule())) {
      return;
    }
    int colon = event.sourceRef().lastIndexOf(':');
    if (colon > 0) {
      String batchNo = event.sourceRef().substring(0, colon);
      String orType = event.sourceRef().substring(colon + 1);
      batches.findByBatchNo(batchNo).ifPresent(b -> issued(b, orType, event));
    }
  }

  private void issued(RemittanceBatch batch, String orType, SettlementReceiptIssued event) {
    String status = ReceiptIssuer.Status.ISSUED.name();
    if ("INCENTIVE".equals(orType)) {
      batch.incentiveReceipt(status, event.receiptNo());
    } else {
      batch.commissionReceipt(status, event.receiptNo());
    }
    batch.receiptMessage(
        ("INCENTIVE".equals(orType) ? "Incentive" : "Commission")
            + " OR "
            + event.receiptNo()
            + " issued at Disbursement approval");
    audit.record(
        BatchService.ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        "OR " + event.receiptNo() + " issued at Disbursement approval");
  }
}
