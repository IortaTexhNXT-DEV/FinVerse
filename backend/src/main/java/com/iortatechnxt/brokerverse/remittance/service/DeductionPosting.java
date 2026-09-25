package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.remittance.domain.DeductionApplication;
import com.iortatechnxt.brokerverse.remittance.domain.DeductionApplicationRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeductionRepository;
import com.iortatechnxt.brokerverse.remittance.service.RemittancePostings.Posting;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes the confirmed remittance deductions of an insurer when one of its batches is approved,
 * and gives them back when the batch's DV is cancelled (ACSL 2.9.2, DIS 2.20.0;
 * ACCOUNTING_DISBURSEMENT_DESIGN 6 row 17):
 *
 * <ul>
 *   <li>the deductions of the batch's insurer and currency are taken oldest first, never more than
 *       the batch pays (net due less the incentives): the total is capped at the amount payable,
 *       and what is left of a deduction waits for the next batch;
 *   <li>each part is posted as {@code OPS_REMIT_DEDUCTION} ({@code RMB:<ref>:<deduction no>}; due
 *       to insurer for disbursement against AR insurer's refund) and recorded as a consumption; a
 *       fully consumed deduction moves to APPLIED (system action {@code apply});
 *   <li>on a cancelled DV the consumptions of the cycle are reversed ({@code :CANCEL}), the amounts
 *       go back to their deductions and an APPLIED deduction is restored to CONFIRMED.
 * </ul>
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class DeductionPosting {

  private static final String AMOUNT = "AMOUNT";

  private final RemittanceDeductionRepository deductions;
  private final DeductionApplicationRepository applications;
  private final DeductionService service;
  private final RemittancePostings postings;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param deductions deductions
   * @param applications consumptions
   * @param service deductions to consume
   * @param postings accounting events
   * @param workflow deduction workflow
   * @param audit audit trail
   * @param clock clock
   */
  public DeductionPosting(
      RemittanceDeductionRepository deductions,
      DeductionApplicationRepository applications,
      DeductionService service,
      RemittancePostings postings,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.deductions = deductions;
    this.applications = applications;
    this.service = service;
    this.postings = postings;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Applies the insurer's confirmed deductions to an approved batch.
   *
   * @param batch approved batch
   * @param branchId branch of the postings
   * @param today value date
   * @return total deducted, at most the amount payable
   */
  public BigDecimal consume(RemittanceBatch batch, Long branchId, LocalDate today) {
    BigDecimal cap = batch.getTotals().payable();
    BigDecimal total = Money.zero();
    for (RemittanceDeduction d :
        service.pending(batch.getCompanyId(), batch.getInsurerCode(), batch.getCurrency())) {
      BigDecimal left = cap.subtract(total);
      if (left.signum() <= 0) {
        break;
      }
      BigDecimal taken = d.consume(left);
      if (taken.signum() > 0) {
        total = total.add(taken);
        record(batch, d, taken, branchId, today);
      }
    }
    batch.deductionsApplied(total);
    return total;
  }

  private void record(
      RemittanceBatch batch,
      RemittanceDeduction d,
      BigDecimal taken,
      Long branchId,
      LocalDate today) {
    DeductionApplication app = applications.save(new DeductionApplication(d.getId(), batch, taken));
    app.posted(
        postings.publish(
            new Posting(
                RemittancePostings.DEDUCTION_EVENT,
                batch,
                branchId,
                today,
                RemittancePostings.sourceRef(batch, d.getDeductionNo()),
                null,
                Map.of(AMOUNT, taken),
                "Deduction " + d.getDeductionNo() + " from " + batch.getBatchNo())));
    if (d.remaining().signum() == 0) {
      workflow.systemTransition(
          DeductionService.ENTITY,
          d.getId().toString(),
          "apply",
          TransitionNote.comment("Applied to " + batch.getBatchNo()));
    }
    audit.record(
        DeductionService.ENTITY,
        d.getDeductionNo(),
        AuditAction.UPDATE,
        "Applied " + taken.toPlainString() + " to " + batch.cycleReference());
  }

  /**
   * Gives back the deductions consumed in the batch's current cycle (its DV was cancelled).
   *
   * @param batch batch
   * @param branchId branch of the postings
   * @param today value date
   */
  public void restore(RemittanceBatch batch, Long branchId, LocalDate today) {
    int cycle = batch.getSettlement().getSendCycle();
    for (DeductionApplication app : applications.findByBatchIdOrderByIdAsc(batch.getId())) {
      if (app.isReversed() || app.getSendCycle() != cycle) {
        continue;
      }
      RemittanceDeduction d = deductions.findById(app.getDeductionId()).orElseThrow();
      postings.publish(
          new Posting(
              RemittancePostings.DEDUCTION_EVENT,
              batch,
              branchId,
              today,
              RemittancePostings.sourceRef(batch, d.getDeductionNo()) + RemittancePostings.CANCEL,
              null,
              Map.of(AMOUNT, app.getAmount().negate()),
              "Cancelled DV - deduction " + d.getDeductionNo() + " given back"));
      app.reverse(clock.instant());
      d.giveBack(app.getAmount());
      if (d.getStage() == DeductionStage.APPLIED) {
        workflow.systemTransition(
            DeductionService.ENTITY,
            d.getId().toString(),
            "restore",
            TransitionNote.comment("DV of " + batch.getBatchNo() + " cancelled"));
      }
      audit.record(
          DeductionService.ENTITY,
          d.getDeductionNo(),
          AuditAction.UPDATE,
          "Given back " + app.getAmount().toPlainString() + " from " + batch.cycleReference());
    }
  }
}
