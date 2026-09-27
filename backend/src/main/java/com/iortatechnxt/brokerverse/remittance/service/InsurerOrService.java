package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine.InsurerOr;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer OR update (RMTID.012/013/016): the insurer returns the remittance schedule with its OR
 * number, date and amount per account; each record updates its batch line (only lines of approved
 * batches, not excluded, OR number and date present, an OR number never used twice for one client),
 * and is compared with the paid AR (exception status MATCHED or AMOUNT_MISMATCH). When every line
 * of a remitted batch has its OR the batch moves to OR_RECEIVED. Uploads run as feed {@code
 * INSURER_REMIT_OR} (manual upload; insurer channels parked, OQ22).
 */
@Service
@Transactional
public class InsurerOrService {

  /** Flow-in feed of the insurer OR schedules. */
  public static final String FEED = "INSURER_REMIT_OR";

  private static final Set<BatchStage> REMITTED =
      Set.of(BatchStage.PARTIALLY_REMITTED, BatchStage.FULLY_REMITTED);
  private static final Set<BatchStage> ACCEPTING =
      Set.of(
          BatchStage.APPROVED,
          BatchStage.PARTIALLY_REMITTED,
          BatchStage.FULLY_REMITTED,
          BatchStage.OR_RECEIVED);

  private final RemittanceBatchRepository batches;
  private final BatchLineRepository lines;
  private final WorkflowService workflow;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param lines batch lines
   * @param workflow batch workflow
   * @param audit audit trail
   */
  public InsurerOrService(
      RemittanceBatchRepository batches,
      BatchLineRepository lines,
      WorkflowService workflow,
      AuditTrailService audit) {
    this.batches = batches;
    this.lines = lines;
    this.workflow = workflow;
    this.audit = audit;
  }

  /**
   * Records the insurer's OR on a batch line.
   *
   * @param batchNo batch
   * @param invoiceNo invoice
   * @param or OR number, date and amount
   * @param runNo upload run
   * @return reference of the update, e.g. {@code RMB-...-000001/INV-1: MATCHED}
   */
  public String record(String batchNo, String invoiceNo, InsurerOr or, String runNo) {
    RemittanceBatch batch =
        batches
            .findByBatchNo(batchNo)
            .orElseThrow(() -> new ResourceNotFoundException("Remittance batch", batchNo));
    if (!ACCEPTING.contains(batch.getStage())) {
      throw new BusinessRuleException(
          "REMIT_OR_BATCH_STAGE",
          "Batch " + batchNo + " is " + batch.getStage() + ": not yet approved");
    }
    BatchLine line = batch.line(invoiceNo);
    if (line.isExcluded()) {
      throw new BusinessRuleException(
          "REMIT_OR_LINE_EXCLUDED", "Invoice " + invoiceNo + " was excluded from " + batchNo);
    }
    boolean usedElsewhere =
        lines.linesWithInsurerOr(line.getClientCode(), or.number()).stream()
            .anyMatch(l -> !l.getId().equals(line.getId()));
    if (usedElsewhere) {
      throw new BusinessRuleException(
          "REMIT_OR_DUPLICATE",
          "OR " + or.number() + " was already recorded for client " + line.getClientCode());
    }
    OrStatus status = line.recordInsurerOr(or, runNo);
    audit.record(
        BatchService.ENTITY,
        batchNo,
        AuditAction.UPDATE,
        "Insurer OR " + or.number() + " for " + invoiceNo + ": " + status);
    boolean complete = batch.included().stream().allMatch(l -> l.getInsurerOrNo() != null);
    if (complete && REMITTED.contains(batch.getStage())) {
      workflow.systemTransition(
          BatchService.ENTITY,
          batch.getId().toString(),
          "or_received",
          TransitionNote.comment(runNo));
    }
    return batchNo + "/" + invoiceNo + ": " + status;
  }
}
