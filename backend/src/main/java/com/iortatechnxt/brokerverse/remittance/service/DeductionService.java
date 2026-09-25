package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.remittance.domain.DeductionApplication;
import com.iortatechnxt.brokerverse.remittance.domain.DeductionApplicationRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeductionRepository;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remittance deductions on insurer confirmation (ACSL 2.9.2; workflow {@code REM_DEDUCTION}): ACSL
 * prepares a deduction (insurer, currency, source such as an AR insurer's refund, amount), submits
 * it with the insurer's written confirmation, and another user confirms it
 * (REMIT_DEDUCTION_CONFIRM). Confirmed deductions are then consumed by the next approved batches of
 * that insurer and currency ({@link DeductionPosting}). Every change is audited.
 */
@Service
@Transactional
public class DeductionService {

  /** Entity type in the workflow and the audit trail. */
  public static final String ENTITY = "RemittanceDeduction";

  /** Workflow of deductions (seeded in V890). */
  public static final String WORKFLOW = "REM_DEDUCTION";

  /** Source LOV. */
  public static final String SOURCE_LOV = "REMIT_DEDUCTION_SOURCE";

  private static final String COMPANY_ID = "companyId";

  private final RemittanceDeductionRepository deductions;
  private final DeductionApplicationRepository applications;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final PartyService parties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param deductions deductions
   * @param applications consumptions by batches
   * @param workflow deduction workflow
   * @param numbers deduction numbers
   * @param lovs source LOV
   * @param parties insurers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public DeductionService(
      RemittanceDeductionRepository deductions,
      DeductionApplicationRepository applications,
      WorkflowService workflow,
      DocumentNumberService numbers,
      LovService lovs,
      PartyService parties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.deductions = deductions;
    this.applications = applications;
    this.workflow = workflow;
    this.numbers = numbers;
    this.lovs = lovs;
    this.parties = parties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Prepares a deduction (DRAFT).
   *
   * @param companyId company
   * @param terms insurer, currency, source, amount and confirmation
   * @return the deduction
   */
  public RemittanceDeduction create(Long companyId, Terms terms) {
    validate(companyId, terms);
    RemittanceDeduction deduction =
        deductions.save(
            new RemittanceDeduction(
                companyId, numbers.next("RDN-" + LocalDate.now(clock).getYear()), terms));
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                deduction.getId().toString(),
                deduction.getDeductionNo(),
                deduction.getInsurerCode() + " - " + describe(deduction),
                "/remittance/deductions/" + deduction.getId(),
                null),
            null));
    audit.record(ENTITY, deduction.getDeductionNo(), AuditAction.CREATE, describe(deduction));
    return deduction;
  }

  /**
   * Changes a draft.
   *
   * @param id deduction
   * @param terms new terms
   * @return the deduction
   */
  public RemittanceDeduction update(Long id, Terms terms) {
    RemittanceDeduction deduction = get(id);
    validate(deduction.getCompanyId(), terms);
    deduction.update(terms);
    audit.record(ENTITY, deduction.getDeductionNo(), AuditAction.UPDATE, describe(deduction));
    return deduction;
  }

  /**
   * Submits a draft with the insurer's confirmation (ACSL 2.9.2).
   *
   * @param id deduction
   * @param comment comment, may be null
   * @return the deduction
   */
  public RemittanceDeduction submit(Long id, String comment) {
    RemittanceDeduction deduction = get(id);
    deduction.submitted(currentUser.username(), clock.instant());
    workflow.transition(ENTITY, id.toString(), "submit", TransitionNote.comment(comment));
    audit.record(ENTITY, deduction.getDeductionNo(), AuditAction.SUBMIT, describe(deduction));
    return deduction;
  }

  /**
   * Confirms a submitted deduction (REMIT_DEDUCTION_CONFIRM, four eyes): the next batch of the
   * insurer consumes it.
   *
   * @param id deduction
   * @param comment comment, may be null
   * @return the deduction
   */
  public RemittanceDeduction confirm(Long id, String comment) {
    RemittanceDeduction deduction = get(id);
    deduction.confirmed(currentUser.username(), clock.instant());
    workflow.transition(ENTITY, id.toString(), "confirm", TransitionNote.comment(comment));
    audit.record(ENTITY, deduction.getDeductionNo(), AuditAction.AUTHORIZE, describe(deduction));
    return deduction;
  }

  /**
   * A deduction.
   *
   * @param id id
   * @return deduction
   */
  @Transactional(readOnly = true)
  public RemittanceDeduction get(Long id) {
    return deductions.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Searches deductions, newest first as the page sorts them.
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param insurerCode insurer, may be null
   * @param text part of the number, source reference or invoice
   * @param pageable page
   * @return deductions
   */
  @Transactional(readOnly = true)
  public Page<RemittanceDeduction> search(
      Long companyId,
      List<DeductionStage> stages,
      String insurerCode,
      String text,
      Pageable pageable) {
    return deductions.findAll(spec(companyId, stages, insurerCode, text), pageable);
  }

  /**
   * The consumptions of a deduction by batches.
   *
   * @param id deduction
   * @return consumptions in order
   */
  @Transactional(readOnly = true)
  public List<DeductionApplication> applications(Long id) {
    return applications.findByDeductionIdOrderByIdAsc(get(id).getId());
  }

  /**
   * The consumptions of a batch.
   *
   * @param batchId batch
   * @return consumptions in order
   */
  @Transactional(readOnly = true)
  public List<DeductionApplication> applicationsOfBatch(Long batchId) {
    return applications.findByBatchIdOrderByIdAsc(batchId);
  }

  /**
   * Confirmed deductions still waiting for a batch of an insurer and currency.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param currency currency
   * @return deductions in consumption order
   */
  @Transactional(readOnly = true)
  public List<RemittanceDeduction> pending(Long companyId, String insurerCode, String currency) {
    return deductions.findByCompanyIdAndInsurerCodeAndCurrencyAndStageOrderByConfirmedAtAscIdAsc(
        companyId, insurerCode, currency, DeductionStage.CONFIRMED);
  }

  /**
   * Mirrors the workflow stage on the deduction.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (ENTITY.equals(event.entityType())) {
      deductions
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(d -> d.markStage(DeductionStage.valueOf(event.toStage())));
    }
  }

  private void validate(Long companyId, Terms terms) {
    lovs.requireValid(SOURCE_LOV, terms.sourceType(), LocalDate.now(clock));
    parties.requireActive(companyId, terms.insurerCode(), List.of(PartyType.INSURER));
  }

  private static String describe(RemittanceDeduction d) {
    return d.getSourceType()
        + " "
        + d.getSourceRef()
        + ", "
        + d.getCurrency()
        + " "
        + d.getAmount().toPlainString();
  }

  private static Specification<RemittanceDeduction> spec(
      Long companyId, List<DeductionStage> stages, String insurerCode, String text) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get(COMPANY_ID), companyId));
      if (stages != null && !stages.isEmpty()) {
        where.add(root.get("stage").in(stages));
      }
      if (insurerCode != null && !insurerCode.isBlank()) {
        where.add(cb.equal(root.get("insurerCode"), insurerCode.strip()));
      }
      if (text != null && !text.isBlank()) {
        String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("deductionNo")), like),
                cb.like(cb.lower(root.get("sourceRef")), like),
                cb.like(cb.lower(cb.coalesce(root.<String>get("invoiceNo"), "")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }
}
