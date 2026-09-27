package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.domain.EbTorRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
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
 * Requests for proposal to insurers (BRID-004, 009; FR-EB-035): only insurers with an approved
 * franchise for the cycle receive the released TOR; one request per insurer (EBR number), due
 * {@code EB_PROPOSAL_REPLY_DAYS} working days later. The first requests move the cycle from
 * FRANCHISE to PROPOSALS ({@code release_tor}). The AO closes a request an insurer does not answer
 * before building the comparative.
 */
@Service
@Transactional
public class InsurerRequestService {

  private final EbInsurerRequestRepository requests;
  private final EbFranchiseRequestRepository franchises;
  private final EbTorRepository tors;
  private final EbRecords records;
  private final EbParties parties;
  private final RequestMail mail;
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
   * @param requests insurer requests
   * @param franchises franchise requests
   * @param tors TOR versions
   * @param records cycle look-up
   * @param parties insurers
   * @param mail request e-mails
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
  public InsurerRequestService(
      EbInsurerRequestRepository requests,
      EbFranchiseRequestRepository franchises,
      EbTorRepository tors,
      EbRecords records,
      EbParties parties,
      RequestMail mail,
      WorkflowService workflow,
      DocumentNumberService numbers,
      EbWorkingDays workingDays,
      EbParameters parameters,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.franchises = franchises;
    this.tors = tors;
    this.records = records;
    this.parties = parties;
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
   * Sends the released TOR to insurers with an approved franchise.
   *
   * @param companyId company
   * @param cycleId cycle in stage FRANCHISE or PROPOSALS
   * @param insurerCodes insurers, at least one
   * @return the requests sent
   */
  public List<EbInsurerRequest> send(Long companyId, Long cycleId, List<String> insurerCodes) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    boolean first = cycle.getStage() == EbCycleStage.FRANCHISE;
    if (!first && cycle.getStage() != EbCycleStage.PROPOSALS) {
      throw new BusinessRuleException(
          "EB_CYCLE_STAGE", "Cycle " + cycle.getCycleNo() + " is not at the market stage");
    }
    EbTor tor =
        tors.findFirstByCycleIdAndStatusOrderByVersionNoDesc(cycle.getId(), EbTor.Status.RELEASED)
            .orElseThrow(
                () -> new BusinessRuleException("EB_TOR_NOT_RELEASED", "Release the TOR first"));
    Set<String> codes = distinct(insurerCodes);
    EbProgramme programme = records.programmeOf(cycle);
    List<InsurerProfile> insurers = new ArrayList<>();
    for (String code : codes) {
      insurers.add(eligible(cycle, parties.insurer(companyId, code)));
    }
    List<Long> files = mail.files(programme, cycle, tor);
    LocalDate due =
        workingDays.plus(companyId, BusinessClock.today(clock), parameters.proposalReplyDays());
    List<EbInsurerRequest> sent = new ArrayList<>();
    for (InsurerProfile insurer : insurers) {
      sent.add(send(programme, cycle, tor, insurer, new Batch(files, due)));
    }
    if (first) {
      workflow.systemTransition(
          EbCodes.ENTITY_CYCLE,
          cycle.getId().toString(),
          "release_tor",
          TransitionNote.comment(sent.size() + " request(s) for proposal sent"));
    }
    return sent;
  }

  private static Set<String> distinct(List<String> insurerCodes) {
    Set<String> codes = new LinkedHashSet<>();
    if (insurerCodes != null) {
      insurerCodes.stream()
          .filter(c -> c != null && !c.isBlank())
          .map(String::strip)
          .forEach(codes::add);
    }
    if (codes.isEmpty()) {
      throw new BusinessRuleException("EB_INSURER_REQUIRED", "Select the insurers");
    }
    return codes;
  }

  private InsurerProfile eligible(EbCycle cycle, InsurerProfile insurer) {
    boolean approved =
        franchises.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
            .anyMatch(f -> f.getInsurerCode().equals(insurer.getPartyCode()) && f.isApproved());
    if (!approved) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_NOT_APPROVED",
          "Insurer " + insurer.getName() + " has no approved franchise for this cycle");
    }
    if (requests
        .findFirstByCycleIdAndInsurerCodeAndStatus(
            cycle.getId(), insurer.getPartyCode(), EbInsurerRequest.Status.OPEN)
        .isPresent()) {
      throw new BusinessRuleException(
          "EB_REQUEST_OPEN", insurer.getName() + " already has an open request on this cycle");
    }
    return insurer;
  }

  private EbInsurerRequest send(
      EbProgramme programme, EbCycle cycle, EbTor tor, InsurerProfile insurer, Batch batch) {
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_REQUEST, BusinessClock.currentYear(clock).getValue()));
    String by = currentUser.username();
    EbInsurerRequest request =
        requests.save(
            new EbInsurerRequest(
                cycle,
                number,
                insurer.getPartyCode(),
                tor,
                new EbInsurerRequest.Sending(clock.instant(), by, batch.due())));
    request.sentAs(mail.send(programme, cycle, request, insurer, batch.files()));
    activity.done(cycle, TatActivity.RFQ_SUBMIT, number, by, insurer.getName());
    activity.received(cycle, TatActivity.QUOTATION, number, insurer.getPartyCode());
    audit.record(
        EbCodes.ENTITY_REQUEST,
        number,
        AuditAction.SUBMIT,
        "Request for proposal sent to "
            + insurer.getName()
            + " with TOR version "
            + tor.getVersionNo()
            + ", due "
            + batch.due());
    return request;
  }

  /**
   * Sends a newly released TOR on the open requests of a cycle.
   *
   * @param cycle cycle
   * @param tor released TOR
   * @return requests updated
   */
  int resend(EbCycle cycle, EbTor tor) {
    List<EbInsurerRequest> open =
        requests.findByCycleIdAndStatus(cycle.getId(), EbInsurerRequest.Status.OPEN);
    if (open.isEmpty()) {
      return 0;
    }
    EbProgramme programme = records.programmeOf(cycle);
    List<Long> files = mail.files(programme, cycle, tor);
    for (EbInsurerRequest request : open) {
      request.updateTor(tor);
      mail.send(
          programme,
          cycle,
          request,
          parties.insurer(cycle.getCompanyId(), request.getInsurerCode()),
          files);
    }
    return open.size();
  }

  /**
   * Closes an open request without a proposal.
   *
   * @param companyId company
   * @param requestId request
   * @param declined whether the insurer declined to quote
   * @param reason why
   * @return the request
   */
  public EbInsurerRequest close(Long companyId, Long requestId, boolean declined, String reason) {
    EbInsurerRequest request = require(companyId, requestId);
    request.close(declined, reason);
    activity.released(TatActivity.QUOTATION, request.getRequestNo(), request.getStatus().name());
    audit.record(
        EbCodes.ENTITY_REQUEST,
        request.getRequestNo(),
        AuditAction.UPDATE,
        (declined ? "Declined by the insurer: " : "Closed: ") + request.getClosedReason());
    return request;
  }

  /**
   * The requests of a cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<EbInsurerRequest> ofCycle(Long companyId, Long cycleId) {
    return requests.findByCycleIdOrderByIdAsc(records.cycle(companyId, cycleId).getId());
  }

  /**
   * A request of a company.
   *
   * @param companyId company
   * @param requestId request
   * @return request
   */
  @Transactional(readOnly = true)
  public EbInsurerRequest require(Long companyId, Long requestId) {
    return requests
        .findById(requestId)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_REQUEST, requestId));
  }

  /**
   * Whether an insurer has an approved franchise on a cycle (the TOR may be sent to it).
   *
   * @param cycleId cycle
   * @param insurerCode insurer
   * @return true when approved
   */
  @Transactional(readOnly = true)
  public boolean franchiseApproved(Long cycleId, String insurerCode) {
    return franchises.findByCycleIdOrderByIdAsc(cycleId).stream()
        .filter(f -> f.getInsurerCode().equals(insurerCode))
        .anyMatch(EbFranchiseRequest::isApproved);
  }

  private record Batch(List<Long> files, LocalDate due) {}
}
