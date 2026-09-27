package com.iortatechnxt.brokerverse.eb.franchise.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.cycle.service.BorGate;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Franchise requests of a cycle that goes to market (BRID-026, 027, 029; FR-EB-032, 033): the AO
 * selects the insurers; each request (EBF number, {@code EB_FRANCHISE} work case) is e-mailed to the
 * insurer's placement mailboxes with the Broker on Record and the documents required for the
 * process FRANCHISE, due {@code EB_FRANCHISE_TAT_DAYS} working days later. The AO records the
 * insurer's decision with its reply as evidence and advises the client. Only an approved insurer
 * receives the TOR.
 */
@Service
@Transactional
public class FranchiseService {

  private final EbFranchiseRequestRepository requests;
  private final EbRecords records;
  private final BorGate borGate;
  private final FranchiseMail mail;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final EbWorkingDays workingDays;
  private final EbParameters parameters;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests franchise requests
   * @param records cycle look-up
   * @param borGate validated BOR check
   * @param mail e-mails and documents of the requests
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param workingDays working-day calendar
   * @param parameters EB parameters
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public FranchiseService(
      EbFranchiseRequestRepository requests,
      EbRecords records,
      BorGate borGate,
      FranchiseMail mail,
      WorkflowService workflow,
      DocumentNumberService numbers,
      EbWorkingDays workingDays,
      EbParameters parameters,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.records = records;
    this.borGate = borGate;
    this.mail = mail;
    this.workflow = workflow;
    this.numbers = numbers;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sends franchise requests to insurers.
   *
   * @param companyId company
   * @param cycleId cycle in stage FRANCHISE
   * @param insurerCodes insurers, at least one
   * @return the requests sent
   */
  public List<EbFranchiseRequest> request(Long companyId, Long cycleId, List<String> insurerCodes) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (cycle.getStage() != EbCycleStage.FRANCHISE) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " is not at the franchise stage");
    }
    LocalDate today = BusinessClock.today(clock);
    borGate.requireValidated(cycle, today);
    Set<String> codes = distinct(insurerCodes);
    EbProgramme programme = records.programmeOf(cycle);
    List<Long> files = mail.requestFiles(programme, cycle, today);
    LocalDate due = workingDays.plus(companyId, today, parameters.franchiseTatDays());
    List<EbFranchiseRequest> sent = new ArrayList<>();
    for (String code : codes) {
      sent.add(send(programme, cycle, mail.insurer(companyId, code), files, due));
    }
    return sent;
  }

  private static Set<String> distinct(List<String> insurerCodes) {
    Set<String> codes = new LinkedHashSet<>();
    if (insurerCodes != null) {
      insurerCodes.stream().filter(c -> c != null && !c.isBlank()).map(String::strip).forEach(codes::add);
    }
    if (codes.isEmpty()) {
      throw new BusinessRuleException("EB_INSURER_REQUIRED", "Select the insurers");
    }
    return codes;
  }

  private EbFranchiseRequest send(
      EbProgramme programme,
      EbCycle cycle,
      InsurerProfile insurer,
      List<Long> files,
      LocalDate due) {
    boolean live =
        requests.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
            .anyMatch(r -> r.getInsurerCode().equals(insurer.getPartyCode()) && isLive(r));
    if (live) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_EXISTS",
          insurer.getName() + " already has a franchise request on cycle " + cycle.getCycleNo());
    }
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_FRANCHISE, BusinessClock.currentYear(clock).getValue()));
    EbFranchiseRequest request =
        requests.save(new EbFranchiseRequest(cycle, number, insurer.getPartyCode()));
    workflow.start(
        new StartCase(
            cycle.getCompanyId(),
            EbCodes.WORKFLOW_FRANCHISE,
            new CaseRecord(
                EbCodes.ENTITY_FRANCHISE,
                request.getId().toString(),
                number,
                "Franchise " + insurer.getName() + " - " + programme.getName(),
                EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=franchise",
                programme.getTeamCode()),
            null));
    Long message = mail.sendRequest(programme, cycle, request, insurer, files, due);
    request.submitted(clock.instant(), due, message);
    transition(request, "submit", TransitionNote.NONE);
    String by = currentUser.username();
    activity.done(cycle, TatActivity.FRANCHISE_SUBMIT, number, by, insurer.getName());
    activity.received(cycle, TatActivity.FRANCHISE_DECISION, number, insurer.getPartyCode());
    audit.record(
        EbCodes.ENTITY_FRANCHISE,
        number,
        AuditAction.SUBMIT,
        "Franchise requested from " + insurer.getName() + ", due " + due);
    return request;
  }

  private static boolean isLive(EbFranchiseRequest r) {
    return r.getStatus() == EbFranchiseRequest.Status.DRAFT
        || r.getStatus() == EbFranchiseRequest.Status.SUBMITTED
        || r.isApproved();
  }

  /**
   * Records the insurer's decision with its reply as evidence.
   *
   * @param companyId company
   * @param requestId request
   * @param input approval or rejection, date, reason, remarks and the reply
   * @return the request
   */
  public EbFranchiseRequest decide(Long companyId, Long requestId, DecisionInput input) {
    EbFranchiseRequest request = require(companyId, requestId);
    EbCycle cycle = records.cycle(companyId, request.getCycleId());
    LocalDate today = BusinessClock.today(clock);
    LocalDate on = input.decidedOn() == null ? today : input.decidedOn();
    if (on.isAfter(today)) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_DATE_FUTURE", "The decision date cannot be after today");
    }
    if (input.evidence() == null) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_EVIDENCE_REQUIRED", "Attach the insurer's reply");
    }
    if (!input.approve() && (input.reasonCode() == null || input.reasonCode().isBlank())) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for 'reject'");
    }
    Long evidence = mail.storeReply(cycle, request, input.evidence());
    EbFranchiseRequest.Status outcome =
        input.approve() ? EbFranchiseRequest.Status.APPROVED : EbFranchiseRequest.Status.REJECTED;
    request.decide(
        outcome,
        new EbFranchiseRequest.Decision(
            on,
            currentUser.username(),
            input.approve() ? null : input.reasonCode().strip(),
            input.remarks(),
            evidence),
        workingDays.plus(companyId, today, parameters.franchiseAdviceDays()));
    transition(
        request,
        input.approve() ? "approve" : "reject",
        new TransitionNote(request.getReasonCode(), input.remarks()));
    activity.released(TatActivity.FRANCHISE_DECISION, request.getFranchiseNo(), outcome.name());
    mail.tellAo(records.programmeOf(cycle), request, outcome);
    audit.record(
        EbCodes.ENTITY_FRANCHISE,
        request.getFranchiseNo(),
        AuditAction.UPDATE,
        "Insurer decision recorded: " + outcome + " on " + on);
    return request;
  }

  /**
   * Advises the client of the insurer's decision by e-mail.
   *
   * @param companyId company
   * @param requestId request, APPROVED or REJECTED
   * @return the request, ADVISED
   */
  public EbFranchiseRequest advise(Long companyId, Long requestId) {
    EbFranchiseRequest request = require(companyId, requestId);
    if (request.getStatus() != EbFranchiseRequest.Status.APPROVED
        && request.getStatus() != EbFranchiseRequest.Status.REJECTED) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_NOT_DECIDED",
          "Franchise request " + request.getFranchiseNo() + " has no decision to advise");
    }
    EbCycle cycle = records.cycle(companyId, request.getCycleId());
    mail.sendAdvice(records.programmeOf(cycle), cycle, request);
    request.advised(clock.instant(), currentUser.username());
    transition(request, "advise", TransitionNote.NONE);
    audit.record(
        EbCodes.ENTITY_FRANCHISE,
        request.getFranchiseNo(),
        AuditAction.UPDATE,
        "Client advised of the " + request.getDecision().name().toLowerCase(java.util.Locale.ROOT));
    return request;
  }

  /**
   * Expires a request left without a decision after the TAT and the grace period (job).
   *
   * @param request submitted request past its due date and grace
   */
  public void expire(EbFranchiseRequest request) {
    transition(request, "expire", TransitionNote.comment("No decision after the franchise TAT"));
    activity.released(TatActivity.FRANCHISE_DECISION, request.getFranchiseNo(), "EXPIRED");
    audit.record(
        EbCodes.ENTITY_FRANCHISE, request.getFranchiseNo(), AuditAction.UPDATE, "Expired");
  }

  /**
   * The requests of a programme (Franchise tab).
   *
   * @param companyId company
   * @param programmeId programme
   * @return requests, latest first
   */
  @Transactional(readOnly = true)
  public List<EbFranchiseRequest> ofProgramme(Long companyId, Long programmeId) {
    return requests.findByProgrammeIdOrderByIdDesc(
        records.programme(companyId, programmeId).getId());
  }

  /**
   * A request of a company.
   *
   * @param companyId company
   * @param requestId request
   * @return request
   */
  @Transactional(readOnly = true)
  public EbFranchiseRequest require(Long companyId, Long requestId) {
    return requests
        .findById(requestId)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_FRANCHISE, requestId));
  }

  private void transition(EbFranchiseRequest request, String action, TransitionNote note) {
    workflow.systemTransition(EbCodes.ENTITY_FRANCHISE, request.getId().toString(), action, note);
  }

  /**
   * The insurer's decision as the AO records it.
   *
   * @param approve true to record an approval, false for a rejection
   * @param decidedOn date of the insurer's reply; today when null
   * @param reasonCode rejection reason (list EB_FRANCHISE_REJECT_REASON)
   * @param remarks remarks, may be null
   * @param evidence the insurer's reply (e-mail or letter), required
   */
  public record DecisionInput(
      boolean approve,
      LocalDate decidedOn,
      String reasonCode,
      String remarks,
      UploadedFile evidence) {}
}
