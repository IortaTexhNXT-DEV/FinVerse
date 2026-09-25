package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRoundRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageResponseRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RoundStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Negotiation rounds and their quotation slips (BRPM.010/012, PMADD04): round 1 opens with the
 * target insurers when the TSU Head approves the request; TSU prepares the slip (insurers, notes),
 * submits it (PQS number, template version, reply date) and a TL or co-officer approves it (four
 * eyes), which sends one protected e-mail per insurer and opens a pending response for each. A slip
 * can be resent to an insurer; {@code revise_qs} closes the round and opens the next one. Rounds
 * are locked at the ManCom sign-off ({@code QS_LOCKED}).
 */
@Service
@Transactional
public class NegotiationService {

  /** Purpose code of the package quotation slip e-mails. */
  public static final String PURPOSE = "PKG_QUOTATION_SLIP";

  private static final String ROUND = "Negotiation round";
  private static final int DEFAULT_REPLY_DAYS = 5;

  private final PackageRequests requests;
  private final NegotiationRoundRepository rounds;
  private final PackageResponseRepository responses;
  private final InsurerService insurers;
  private final PackageNumbers numbers;
  private final PackageDocuments documents;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final SystemParameterService parameters;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param rounds negotiation rounds
   * @param responses insurer responses
   * @param insurers insurer panel
   * @param numbers numbering
   * @param documents quotation slip PDF
   * @param templates document templates
   * @param messages outbox
   * @param parameters business parameters
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public NegotiationService(
      PackageRequests requests,
      NegotiationRoundRepository rounds,
      PackageResponseRepository responses,
      InsurerService insurers,
      PackageNumbers numbers,
      PackageDocuments documents,
      DocTemplateService templates,
      MessageService messages,
      SystemParameterService parameters,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.rounds = rounds;
    this.responses = responses;
    this.insurers = insurers;
    this.numbers = numbers;
    this.documents = documents;
    this.templates = templates;
    this.messages = messages;
    this.parameters = parameters;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Rounds of a request, first round first.
   *
   * @param id request
   * @return rounds
   */
  @Transactional(readOnly = true)
  public List<NegotiationRound> rounds(Long id) {
    return rounds.findByRequestIdOrderByRoundNo(requests.get(id).getId());
  }

  /**
   * One round of a request.
   *
   * @param id request
   * @param roundNo round number
   * @return round
   */
  @Transactional(readOnly = true)
  public NegotiationRound round(Long id, int roundNo) {
    return rounds
        .findByRequestIdAndRoundNo(id, roundNo)
        .orElseThrow(() -> new ResourceNotFoundException(ROUND, id + "/" + roundNo));
  }

  /**
   * Opens round 1 with the request's target insurers (called when the request enters the
   * negotiation for the first time).
   *
   * @param p request
   * @param insurerCodes target insurers
   * @param notes quotation slip notes
   */
  public void openFirstRound(PackageRequest p, List<String> insurerCodes, String notes) {
    if (rounds.findFirstByRequestIdOrderByRoundNoDesc(p.getId()).isEmpty()) {
      rounds.save(new NegotiationRound(p.getId(), 1, insurerCodes, notes));
      audit.record(
          PackageRequests.ENTITY,
          p.getRequestNo(),
          AuditAction.UPDATE,
          "Negotiation round 1 opened");
    }
  }

  /**
   * Changes the insurers and notes of a round's slip before it is sent.
   *
   * @param id request
   * @param roundNo round
   * @param insurerCodes insurers to approach
   * @param notes quotation slip notes
   * @return round
   */
  public NegotiationRound prepare(Long id, int roundNo, List<String> insurerCodes, String notes) {
    PackageRequest p = negotiating(id);
    NegotiationRound r = editable(id, roundNo);
    if (r.getStatus() == RoundStatus.SENT || r.getStatus() == RoundStatus.CLOSED) {
      throw new BusinessRuleException(
          "QS_ALREADY_SENT", "Round " + roundNo + " was sent: revise the slip in a new round");
    }
    List<String> selected = insurerCodes.stream().distinct().toList();
    selected.forEach(code -> insurers.requireUsableInsurer(p.getCompanyId(), code));
    r.prepare(selected, notes);
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "Quotation slip round " + roundNo + " insurers: " + String.join(", ", selected));
    return r;
  }

  /**
   * Submits a round's slip for approval: numbers it and stamps the template version.
   *
   * @param id request
   * @param roundNo round
   * @param replyBy reply date; null for the configured delay (PKG_QS_REPLY_DAYS)
   * @return round
   */
  public NegotiationRound submitSlip(Long id, int roundNo, LocalDate replyBy) {
    PackageRequest p = negotiating(id);
    NegotiationRound r = editable(id, roundNo);
    if (r.getStatus() != RoundStatus.PREPARATION) {
      throw new BusinessRuleException(
          "QS_NOT_IN_PREPARATION", "The slip of round " + roundNo + " is not in preparation");
    }
    if (r.getInsurers().isEmpty()) {
      throw new BusinessRuleException(
          "QS_NO_INSURER", "Select at least one insurer for the quotation slip");
    }
    LocalDate today = LocalDate.now(clock);
    DocTemplate template = templates.current(PackageDocuments.QS_TEMPLATE, today);
    LocalDate reply =
        replyBy != null
            ? replyBy
            : today.plusDays(parameters.intValue("PKG_QS_REPLY_DAYS", DEFAULT_REPLY_DAYS));
    r.submit(
        r.getQsNo() == null ? numbers.quotationSlip() : r.getQsNo(),
        template.getCode() + " v" + template.getVersionNo(),
        reply,
        currentUser.username(),
        clock.instant());
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "Quotation slip " + r.getQsNo() + " submitted for approval");
    return r;
  }

  /**
   * Approves a round's slip (TL or co-officer, never the preparer) and sends it to every insurer
   * (BRPM.012): one protected e-mail each, logged in the outbox, and a pending response each.
   *
   * @param id request
   * @param roundNo round
   * @return round
   */
  public NegotiationRound approveSlip(Long id, int roundNo) {
    PackageRequest p = negotiating(id);
    NegotiationRound r = editable(id, roundNo);
    if (r.getStatus() != RoundStatus.FOR_APPROVAL) {
      throw new BusinessRuleException(
          "QS_NOT_SUBMITTED", "The slip of round " + roundNo + " is not submitted for approval");
    }
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, r.getPreparedBy())) {
      throw new BusinessRuleException(
          "QS_FOUR_EYES", "The quotation slip is approved by a TL or a co-officer");
    }
    MessageFile slip = documents.quotationSlip(p, r);
    for (String code : r.getInsurers()) {
      InsurerProfile insurer = insurers.requireUsableInsurer(p.getCompanyId(), code);
      PackageInsurerResponse response =
          responses
              .findByRoundIdAndInsurerCode(r.getId(), code)
              .orElseGet(
                  () ->
                      responses.save(
                          new PackageInsurerResponse(r.getId(), code, insurer.getName())));
      response.markSent(sendTo(p, r, insurer, slip));
    }
    r.markSent(user, clock.instant());
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "Quotation slip " + r.getQsNo() + " sent to " + String.join(", ", r.getInsurers()));
    return r;
  }

  /**
   * Sends a round's slip again to one insurer (BRPM.012: view history and resend).
   *
   * @param id request
   * @param roundNo round
   * @param insurerCode insurer
   * @return the insurer's response
   */
  public PackageInsurerResponse resend(Long id, int roundNo, String insurerCode) {
    PackageRequest p = requests.get(id);
    NegotiationRound r = round(id, roundNo);
    if (r.isLocked() || r.getStatus() != RoundStatus.SENT) {
      throw new BusinessRuleException(
          "QS_NOT_RESENDABLE", "Only the slip of an open, sent round can be resent");
    }
    PackageInsurerResponse response =
        responses
            .findByRoundIdAndInsurerCode(r.getId(), insurerCode)
            .orElseThrow(() -> new ResourceNotFoundException("Insurer response", insurerCode));
    InsurerProfile insurer = insurers.requireUsableInsurer(p.getCompanyId(), insurerCode);
    response.markSent(sendTo(p, r, insurer, documents.quotationSlip(p, r)));
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "Quotation slip " + r.getQsNo() + " resent to " + insurerCode);
    return response;
  }

  /**
   * Revises the quotation slip (PMADD04 loop): closes the sent round and opens the next one with
   * the given insurers (by default every insurer that did not decline).
   *
   * @param id request
   * @param insurerCodes insurers of the new round; empty for the default
   * @param notes notes of the new round (what changes)
   * @return the new round
   */
  public NegotiationRound revise(Long id, List<String> insurerCodes, String notes) {
    PackageRequest p = negotiating(id);
    NegotiationRound current = latest(id);
    if (current.getStatus() != RoundStatus.SENT) {
      throw new BusinessRuleException(
          "QS_NOT_SENT", "Send the slip of round " + current.getRoundNo() + " before revising it");
    }
    List<String> next =
        insurerCodes == null || insurerCodes.isEmpty()
            ? responses.findByRoundIdOrderById(current.getId()).stream()
                .filter(r -> !"DECLINED".equals(r.getOutcome()))
                .map(PackageInsurerResponse::getInsurerCode)
                .toList()
            : insurerCodes.stream().distinct().toList();
    next.forEach(code -> insurers.requireUsableInsurer(p.getCompanyId(), code));
    current.close();
    int roundNo = current.getRoundNo() + 1;
    NegotiationRound opened = rounds.save(new NegotiationRound(id, roundNo, next, notes));
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "revise_qs",
        TransitionNote.comment("Round " + roundNo + (notes == null ? "" : ": " + notes)));
    return opened;
  }

  /**
   * The latest round of a request.
   *
   * @param id request
   * @return round
   */
  @Transactional(readOnly = true)
  public NegotiationRound latest(Long id) {
    return rounds
        .findFirstByRequestIdOrderByRoundNoDesc(id)
        .orElseThrow(() -> new ResourceNotFoundException(ROUND, id + "/latest"));
  }

  /**
   * Locks every round of a request (ManCom sign-off, BRPM.012).
   *
   * @param id request
   */
  public void lockAll(Long id) {
    rounds.findByRequestIdOrderByRoundNo(id).forEach(NegotiationRound::lock);
  }

  private PackageRequest negotiating(Long id) {
    return requests.inStage(
        id,
        RequestStage.NEGOTIATION,
        "PKG_NOT_IN_NEGOTIATION",
        "Quotation slips are prepared during the negotiation");
  }

  private NegotiationRound editable(Long id, int roundNo) {
    NegotiationRound r = round(id, roundNo);
    if (r.isLocked()) {
      throw new BusinessRuleException(
          "QS_LOCKED", "The slips are locked since the ManCom sign-off");
    }
    return r;
  }

  private Long sendTo(PackageRequest p, NegotiationRound r, InsurerProfile insurer, MessageFile f) {
    List<String> to = insurer.getPlacementEmailList();
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "INSURER_NO_EMAIL", insurer.getName() + " has no placement e-mail address");
    }
    return messages
        .queueEmail(
            new OutboundEmail(
                p.getCompanyId(),
                PURPOSE,
                to,
                List.of(),
                "Package quotation slip " + r.getQsNo() + " - " + p.getTitle(),
                "Dear "
                    + insurer.getName()
                    + ",\n\nPlease find attached our package quotation slip "
                    + r.getQsNo()
                    + " (round "
                    + r.getRoundNo()
                    + ") for "
                    + p.getTitle()
                    + ". Kindly send your best terms on or before "
                    + r.getReplyDue()
                    + ". The document is password protected; the password follows separately."
                    + "\n\nBDO Insurance and Reinsurance Brokers, Inc.",
                List.of(f),
                new Protection(null, true, null),
                new RecordLink(PackageRequests.ENTITY, String.valueOf(p.getId()), r.getQsNo())))
        .messageId();
  }
}
