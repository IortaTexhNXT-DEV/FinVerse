package com.iortatechnxt.brokerverse.eb.cycle.service;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycles of a programme (BRID-001-004, 022.01; FR-EB-021): one per policy year with its business
 * type NEW_BUSINESS or RENEWAL, numbered {@code EBC-<yyyy>-nnnnnn}, each with its {@code EB_CYCLE}
 * work case. The AO opens a new-business cycle (or a renewal cycle of a programme flagged for
 * renewal; the renewal advice job opens them too) and moves it through the requirement stages:
 * start (new business), stay with the incumbent (renewal) or go to market, which needs a validated
 * Broker on Record (BRID-008). Closing as lost or not renewed is a generic action of the workflow
 * panel with a reason; {@link CycleOutcomeListener} records its outcome.
 */
@Service
@Transactional
public class CycleService {

  private final EbCycleRepository cycles;
  private final EbRecords records;
  private final BorGate borGate;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cycles cycles
   * @param records programme and cycle look-up
   * @param borGate the validated BOR check
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CycleService(
      EbCycleRepository cycles,
      EbRecords records,
      BorGate borGate,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.cycles = cycles;
    this.records = records;
    this.borGate = borGate;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Opens a cycle for a policy year.
   *
   * @param companyId company
   * @param programmeId programme
   * @param request business type, policy year and target inception
   * @return the cycle in stage OPEN
   */
  public EbCycle open(Long companyId, Long programmeId, OpenCycle request) {
    EbProgramme programme = records.programme(companyId, programmeId);
    if (request.businessType() == BusinessType.RENEWAL && !programme.isRenewalEligible()) {
      throw new BusinessRuleException(
          "EB_NOT_RENEWAL_ELIGIBLE",
          "Programme " + programme.getProgrammeNo() + " is not flagged for renewal");
    }
    return open(programme, request);
  }

  /**
   * Opens a cycle of a programme (also used by the renewal advice).
   *
   * @param programme programme
   * @param request business type, policy year and target inception
   * @return the cycle in stage OPEN
   */
  public EbCycle open(EbProgramme programme, OpenCycle request) {
    if (programme.getStatus() == EbProgrammeStatus.INACTIVE) {
      throw new BusinessRuleException(
          "EB_PROGRAMME_INACTIVE", "Programme " + programme.getProgrammeNo() + " is inactive");
    }
    int year = request.policyYear() == null ? defaultYear(request) : request.policyYear();
    if (cycles.findOpen(programme.getId(), year).isPresent()) {
      throw new BusinessRuleException(
          "EB_CYCLE_OPEN_EXISTS",
          "Programme " + programme.getProgrammeNo() + " already has an open cycle for " + year);
    }
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_CYCLE, BusinessClock.currentYear(clock).getValue()));
    EbCycle cycle =
        cycles.save(
            new EbCycle(
                programme.getCompanyId(),
                number,
                programme.getId(),
                request.businessType(),
                year,
                request.targetInception()));
    workflow.start(
        new StartCase(
            programme.getCompanyId(),
            EbCodes.WORKFLOW_CYCLE,
            new CaseRecord(
                EbCodes.ENTITY_CYCLE,
                cycle.getId().toString(),
                number,
                title(programme, cycle),
                EbCodes.PROGRAMME_LINK + programme.getId(),
                programme.getTeamCode()),
            null));
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.OPEN,
        "Cycle " + number + " opened: " + request.businessType() + " " + year);
    return cycle;
  }

  private int defaultYear(OpenCycle request) {
    return request.targetInception() == null
        ? BusinessClock.currentYear(clock).getValue()
        : request.targetInception().getYear();
  }

  private static String title(EbProgramme programme, EbCycle cycle) {
    String kind = cycle.getBusinessType() == BusinessType.RENEWAL ? "Renewal" : "New business";
    return kind + " " + cycle.getPolicyYear() + " - " + programme.getName();
  }

  /**
   * Starts the requirements of a new-business cycle (OPEN to REQUIREMENTS).
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the cycle
   */
  public EbCycle start(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getBusinessType() == BusinessType.RENEWAL) {
      throw new BusinessRuleException(
          "EB_RENEWAL_STARTS_WITH_RA", "A renewal cycle starts with the renewal advice");
    }
    return move(cycle, EbCycleStage.OPEN, "start", "Requirements started");
  }

  /**
   * The client renews with the incumbent without remarketing (REQUIREMENTS to INCUMBENT_TERMS).
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the cycle
   */
  public EbCycle stayWithIncumbent(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getBusinessType() != BusinessType.RENEWAL) {
      throw new BusinessRuleException(
          "EB_NOT_A_RENEWAL", "Only a renewal cycle can stay with the incumbent");
    }
    return move(cycle, EbCycleStage.REQUIREMENTS, "stay_with_incumbent", "Stays with incumbent");
  }

  /**
   * Goes to market (REQUIREMENTS to FRANCHISE): a renewal is remarketed, a new business is
   * marketed; both need a validated Broker on Record (BRID-008).
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the cycle
   */
  public EbCycle remarket(Long companyId, Long cycleId) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    requireStage(cycle, EbCycleStage.REQUIREMENTS);
    borGate.requireValidated(cycle, BusinessClock.today(clock));
    cycle.markRemarketing();
    return move(cycle, EbCycleStage.REQUIREMENTS, "remarket", "Goes to market");
  }

  private EbCycle move(EbCycle cycle, EbCycleStage from, String action, String summary) {
    requireStage(cycle, from);
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE, cycle.getId().toString(), action, TransitionNote.NONE);
    audit.record(EbCodes.ENTITY_CYCLE, cycle.getCycleNo(), AuditAction.UPDATE, summary);
    return cycle;
  }

  private static void requireStage(EbCycle cycle, EbCycleStage stage) {
    if (cycle.getStage() != stage) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE",
          "Cycle " + cycle.getCycleNo() + " is not in stage " + label(stage) + " any more");
    }
  }

  private static String label(EbCycleStage stage) {
    String text = stage.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /**
   * A cycle to open.
   *
   * @param businessType NEW_BUSINESS or RENEWAL, required
   * @param policyYear policy year; the target inception's year (or this year) when null
   * @param targetInception target inception, may be null
   */
  public record OpenCycle(
      BusinessType businessType, Integer policyYear, LocalDate targetInception) {}
}
