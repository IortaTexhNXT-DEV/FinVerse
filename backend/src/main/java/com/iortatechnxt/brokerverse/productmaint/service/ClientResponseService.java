package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalAcceptanceService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.productmaint.domain.ClientResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.ClientResponseRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsRecord;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client response to a proposal (BDOI FRS FRPM.010.01): the Account Officer records Accepted
 * (with the client's acceptance e-mail attached; the request goes on to deployment), Rejected or
 * Return for Revision (remarks required; the request goes back to the comparative table so the
 * Final Terms can be revised and the proposal generated again). The response date, the remarks and
 * who recorded it are kept, and the TSU officer and the requestor are told.
 */
@Service
@Transactional
public class ClientResponseService {

  /** Event of the response. */
  public static final String EVENT = "PM_CLIENT_RESPONSE";

  /** Accepted. */
  public static final String ACCEPTED = "ACCEPTED";

  /** Rejected. */
  public static final String REJECTED = "REJECTED";

  /** Return for revision. */
  public static final String RETURNED = "RETURNED";

  private static final Map<String, String> NAMES =
      Map.of(ACCEPTED, "Accepted", REJECTED, "Rejected", RETURNED, "Return for Revision");

  private final ProposalService proposals;
  private final ProposalAcceptanceService acceptance;
  private final ClientResponseRepository responses;
  private final WorkflowService workflow;
  private final NoticeDelivery delivery;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals quotation requests
   * @param acceptance acceptance of the proposal
   * @param responses recorded responses
   * @param workflow workflow
   * @param delivery notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ClientResponseService(
      ProposalService proposals,
      ProposalAcceptanceService acceptance,
      ClientResponseRepository responses,
      WorkflowService workflow,
      NoticeDelivery delivery,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.acceptance = acceptance;
    this.responses = responses;
    this.workflow = workflow;
    this.delivery = delivery;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The name of a response.
   *
   * @param response code
   * @return name
   */
  public static String name(String response) {
    return NAMES.getOrDefault(response, response);
  }

  /**
   * Records the client response to the proposal of a quotation request.
   *
   * @param id quotation request
   * @param response ACCEPTED, REJECTED or RETURNED
   * @param details response date, client remarks, insurers accepted
   * @return the response
   */
  public ClientResponse record(Long id, String response, ClientResponse.Details details) {
    ProposalRequest p = proposals.get(id);
    if (p.getStatus() != ProposalStatus.SENT_TO_CLIENT) {
      throw new BusinessRuleException(
          "PM_CLIENT_RESPONSE_STAGE", "The client responds once the proposal was sent");
    }
    ClientResponse.Details d = checked(response, details);
    String entityId = String.valueOf(id);
    switch (response) {
      case ACCEPTED -> acceptance.accept(id, List.of(), d.remarks());
      case REJECTED ->
          workflow.transition(
              ProposalService.ENTITY, entityId, "decline", TransitionNote.comment(d.remarks()));
      default ->
          workflow.transition(
              ProposalService.ENTITY,
              entityId,
              "client_return",
              TransitionNote.comment(d.remarks()));
    }
    ClientResponse saved =
        responses.save(
            new ClientResponse(
                new TermsRecord(TermsRecord.QUOTATION, id),
                response,
                d,
                currentUser.username(),
                clock.instant()));
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Client response: " + name(response) + (d.remarks() == null ? "" : " - " + d.remarks()));
    tell(p, response, d.remarks());
    return saved;
  }

  private ClientResponse.Details checked(String response, ClientResponse.Details details) {
    if (!NAMES.containsKey(response)) {
      throw new BusinessRuleException(
          "PM_CLIENT_RESPONSE", "Choose Accepted, Rejected or Return for Revision");
    }
    ClientResponse.Details d =
        details == null ? new ClientResponse.Details(null, null, null) : details;
    String remarks = d.remarks() == null || d.remarks().isBlank() ? null : d.remarks().strip();
    if (remarks == null && !ACCEPTED.equals(response)) {
      throw new BusinessRuleException(
          "PM_CLIENT_REMARKS", "Enter the client's remarks for " + name(response));
    }
    return new ClientResponse.Details(responseDate(d.responseDate()), remarks, d.insurers());
  }

  private LocalDate responseDate(LocalDate given) {
    LocalDate today = BusinessClock.today(clock);
    LocalDate date = given == null ? today : given;
    if (date.isAfter(today)) {
      throw new BusinessRuleException(
          "PM_CLIENT_RESPONSE_DATE", "The response date cannot be in the future");
    }
    return date;
  }

  private void tell(ProposalRequest p, String response, String remarks) {
    Set<String> people = new LinkedHashSet<>();
    if (p.getPsSubmittedBy() != null) {
      people.add(p.getPsSubmittedBy());
    }
    people.add(p.getCreatedBy());
    people.remove(currentUser.username());
    Notice notice =
        new Notice(
            (p.getArn() == null ? p.getPrfNo() : p.getArn())
                + ": client response "
                + name(response),
            p.getClientName() + (remarks == null ? "" : " - " + remarks),
            "/proposals/" + p.getId(),
            ProposalService.ENTITY,
            String.valueOf(p.getId()));
    people.forEach(u -> delivery.toUser(u, notice, EVENT, false));
  }

  /**
   * The responses recorded, newest first.
   *
   * @param id quotation request
   * @return responses
   */
  @Transactional(readOnly = true)
  public List<ClientResponse> responses(Long id) {
    return responses.findByRecordTypeAndRecordIdOrderByIdDesc(TermsRecord.QUOTATION, id);
  }
}
