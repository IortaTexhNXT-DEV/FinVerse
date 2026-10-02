package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationWorkflow;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.Resubmission;
import com.iortatechnxt.brokerverse.migration.load.domain.ResubmissionRepository;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resubmissions of corrected rejected rows (workflow MIG_RESUBMISSION; DATA_MIGRATION_DESIGN
 * section 15.1; FR-DM-125): the maker puts the corrected rows in a resubmission file (next
 * sequence, same as-of date) that is received as an extract; the checker, never the maker, approves
 * it in the console, and an approved resubmission becomes a RERUN batch of the parent. In
 * production resubmissions are approved until {@code MIG_RESUBMIT_DEADLINE}.
 */
@Service
@Transactional
public class ResubmissionService {

  private final BatchPlanService plans;
  private final MigExtractRepository extracts;
  private final ResubmissionRepository resubmissions;
  private final DocumentNumberService numbers;
  private final MigrationWorkflow workflow;
  private final MigrationParameters parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans batches
   * @param extracts extracts
   * @param resubmissions resubmissions
   * @param numbers document numbers
   * @param workflow workflow cases
   * @param parameters deadline
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ResubmissionService(
      BatchPlanService plans,
      MigExtractRepository extracts,
      ResubmissionRepository resubmissions,
      DocumentNumberService numbers,
      MigrationWorkflow workflow,
      MigrationParameters parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.plans = plans;
    this.extracts = extracts;
    this.resubmissions = resubmissions;
    this.numbers = numbers;
    this.workflow = workflow;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Prepares a resubmission of a batch with a corrected extract.
   *
   * @param batchNo parent batch
   * @param extractNo corrected extract (staged)
   * @return the resubmission
   */
  public Resubmission prepare(String batchNo, String extractNo) {
    MigBatch parent = plans.get(batchNo);
    MigExtract extract =
        extracts
            .findByExtractNo(extractNo)
            .orElseThrow(
                () -> new ResourceNotFoundException(MigrationCodes.ENTITY_EXTRACT, extractNo));
    if (!extract.getObjectCode().equals(parent.getObjectCode())
        || extract.getStatus() != ExtractStatus.STAGED) {
      throw new BusinessRuleException(
          "MIG_RESUBMISSION_EXTRACT",
          "Extract " + extractNo + " is not a staged extract of object " + parent.getObjectCode());
    }
    Resubmission r =
        resubmissions.save(
            new Resubmission(
                parent.getCompanyId(),
                numbers.next("MGS-" + BusinessClock.today(clock).getYear()),
                parent,
                extract.getId(),
                extract.getStagedRows(),
                currentUser.username(),
                clock.instant()));
    workflow.open(
        parent.getCompanyId(),
        MigrationCodes.WF_RESUBMISSION,
        new CaseRecord(
            MigrationCodes.ENTITY_RESUBMISSION,
            String.valueOf(r.getId()),
            r.getResubmissionNo(),
            extract.getStagedRows() + " corrected rows of " + parent.getObjectCode(),
            "/migration/batches?batch=" + batchNo,
            null));
    audit.record(
        MigrationCodes.ENTITY_RESUBMISSION,
        r.getResubmissionNo(),
        AuditAction.SUBMIT,
        "Prepared with " + extractNo);
    return r;
  }

  /**
   * The checker approves or returns a resubmission; an approved one becomes a RERUN batch.
   *
   * @param resubmissionNo resubmission
   * @param approve approve or return
   * @param note note (mandatory on a return)
   * @return the resubmission
   */
  public Resubmission decide(String resubmissionNo, boolean approve, String note) {
    Resubmission r =
        resubmissions
            .findByResubmissionNo(resubmissionNo)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        MigrationCodes.ENTITY_RESUBMISSION, resubmissionNo));
    if (approve && parameters.production()) {
      LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), BusinessClock.zone());
      if (now.isAfter(parameters.resubmitDeadline())) {
        throw new BusinessRuleException(
            "MIG_RESUBMIT_DEADLINE",
            "Resubmissions were accepted until " + parameters.resubmitDeadline());
      }
    }
    r.decide(approve, currentUser.username(), note, clock.instant());
    if (approve) {
      MigBatch parent = plans.get(batchNoOf(r));
      MigBatch rerun = plans.rerun(parent.getBatchNo(), r.getExtractId());
      r.setRerunBatchId(rerun.getId());
    }
    workflow.move(
        MigrationCodes.ENTITY_RESUBMISSION,
        String.valueOf(r.getId()),
        approve ? "approve" : "return",
        note);
    audit.record(
        MigrationCodes.ENTITY_RESUBMISSION,
        resubmissionNo,
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        approve ? "Approved" : "Returned: " + note);
    return r;
  }

  private String batchNoOf(Resubmission r) {
    return plans.byId(r.getParentBatchId()).getBatchNo();
  }

  /**
   * Resubmissions of a company.
   *
   * @param companyId company
   * @return resubmissions, newest first
   */
  @Transactional(readOnly = true)
  public List<Resubmission> list(Long companyId) {
    return resubmissions.findByCompanyIdOrderByIdDesc(companyId);
  }
}
