package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.service.RemittancePostings.Posting;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restores a batch whose DV was cancelled in Disbursement (DIS 2.20.0;
 * ACCOUNTING_DISBURSEMENT_DESIGN 6 row 11 and 12.2), so it can be sent again:
 *
 * <ol>
 *   <li>the postings of the cycle are reversed with the opposite sign ({@code <ref>:CANCEL}):
 *       {@code OPS_REMITTANCE} per line, the incentives and the deductions (given back to their
 *       deductions);
 *   <li>the REMITTED ledger movements are undone (negative REMITTED), the invoices are back in
 *       remittance review and locked by remittance;
 *   <li>the batch returns to REVIEW_IN_PROCESS (system action {@code dv_cancelled}) with the next
 *       send cycle, so its next approval sends a new payment request ({@code <batch>/R<n>}); the
 *       processor may also exclude lines or return the batch.
 * </ol>
 *
 * <p>An invoice that another team locked in the meantime cannot be restored: the batch is then left
 * as it is and the caller notifies the processors.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchCancellation {

  private static final Set<BatchStage> RESTORABLE =
      Set.of(BatchStage.APPROVED, BatchStage.PARTIALLY_REMITTED, BatchStage.FULLY_REMITTED);

  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final BatchLedger batchLedger;
  private final RemittancePostings postings;
  private final BatchIncentives incentives;
  private final DeductionPosting deductions;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param ledger ledger reads
   * @param writer ledger movements
   * @param batchLedger ledger status and lock of lines
   * @param postings accounting events
   * @param incentives incentive postings
   * @param deductions deduction postings
   * @param workflow batch workflow
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BatchCancellation(
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      BatchLedger batchLedger,
      RemittancePostings postings,
      BatchIncentives incentives,
      DeductionPosting deductions,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.ledger = ledger;
    this.writer = writer;
    this.batchLedger = batchLedger;
    this.postings = postings;
    this.incentives = incentives;
    this.deductions = deductions;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Whether a batch in this stage is restored when its DV is cancelled.
   *
   * @param batch batch
   * @return true once approved and before the insurer OR
   */
  public static boolean restorable(RemittanceBatch batch) {
    return RESTORABLE.contains(batch.getStage());
  }

  /**
   * The invoices of the batch another team locked since, which block the restoration.
   *
   * @param batch batch
   * @return invoice numbers with the lock owner
   */
  public List<String> blockers(RemittanceBatch batch) {
    List<String> blocked = new ArrayList<>();
    for (BatchLine line : batch.included()) {
      String owner = ledger.require(line.getInvoiceNo()).getLockOwner();
      if (owner != null && !RemittanceSettings.MODULE.equals(owner)) {
        blocked.add(line.getInvoiceNo() + " (locked by " + owner + ")");
      }
    }
    return blocked;
  }

  /**
   * Reverses the cycle and returns the batch to review.
   *
   * @param batch batch whose DV was cancelled (restorable, no blockers)
   * @param dvNo cancelled DV
   * @param reason cancellation reason
   */
  public void restore(RemittanceBatch batch, String dvNo, String reason) {
    LocalDate today = LocalDate.now(clock);
    Long branchId = null;
    for (BatchLine line : batch.included()) {
      OpsInvoice invoice = ledger.require(line.getInvoiceNo());
      branchId = branchId == null ? invoice.getBranchId() : branchId;
      reverseLine(batch, line, invoice, today);
    }
    incentives.reverse(batch, branchId, today);
    deductions.restore(batch, branchId, today);
    String dv = dvNo == null ? batch.getDvNo() : dvNo;
    batch.dvCancelled(dv, reason, clock.instant());
    workflow.systemTransition(
        BatchService.ENTITY,
        batch.getId().toString(),
        "dv_cancelled",
        TransitionNote.comment("DV " + dv + " cancelled" + (reason == null ? "" : ": " + reason)));
    audit.record(
        BatchService.ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        "DV "
            + dv
            + " cancelled; postings reversed, batch back to review as "
            + batch.cycleReference());
  }

  private void reverseLine(
      RemittanceBatch batch, BatchLine line, OpsInvoice invoice, LocalDate today) {
    String ref =
        RemittancePostings.sourceRef(batch, line.getInvoiceNo()) + RemittancePostings.CANCEL;
    String journal =
        postings.publish(
            new Posting(
                RemittancePostings.REMITTANCE_EVENT,
                batch,
                invoice.getBranchId(),
                today,
                ref,
                invoice,
                RemittancePostings.lineAmounts(line.getAmounts(), -1),
                "Cancelled DV - remittance " + line.getInvoiceNo() + " " + batch.getBatchNo()));
    RemittanceAmounts a = line.getAmounts();
    Map<LedgerComponent, BigDecimal> moved = new EnumMap<>(LedgerComponent.class);
    moved.put(LedgerComponent.DTIP, a.dtip().negate());
    moved.put(LedgerComponent.COMMISSION, a.commission().negate());
    moved.put(LedgerComponent.COMMISSION_VAT, a.commissionVat().negate());
    moved.put(LedgerComponent.WTAX, a.wtax().negate());
    moved.values().removeIf(v -> v.signum() == 0);
    if (!moved.isEmpty()) {
      writer.post(
          new MovementRequest(
              line.getInvoiceNo(),
              MovementType.REMITTED,
              RemittanceSettings.MODULE,
              RemittancePostings.movementRef(batch) + RemittancePostings.CANCEL,
              today,
              moved,
              new DocumentRefs(null, null, batch.getBatchNo(), journal),
              "DV of " + batch.getBatchNo() + " cancelled: remittance undone"));
    }
    batchLedger.take(line.getInvoiceNo(), batch.getBatchNo());
    line.remitted(null);
  }
}
