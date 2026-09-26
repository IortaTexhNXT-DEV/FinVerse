package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Proposal Slip (BRNB.013/014/017): TSU chooses the insurer (the recommended one by default)
 * and submits the slip, numbered PS-yyyy and built from the chosen insurer's terms; each version is
 * archived on the PRF as a PROPOSAL_SLIP document. A second TSU officer approves it, which releases
 * it to Marketing, and Marketing sends it to the client with the comparative table, password
 * protected.
 */
@Service
@Transactional
public class ProposalSlipService {

  /** Document type of the archived proposal slips. */
  public static final String SLIP_DOCUMENT = "PROPOSAL_SLIP";

  /** Purpose code of the proposal e-mails. */
  public static final String PURPOSE = "PROPOSAL";

  private final ProposalService proposals;
  private final InsurerResponseRepository responses;
  private final ProposalNumbers numbers;
  private final ProposalDocuments slips;
  private final DocumentService documents;
  private final MessageService messages;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals PRF reads
   * @param responses insurer responses
   * @param numbers numbering
   * @param slips proposal slip and comparative table
   * @param documents documents of the PRF (archive)
   * @param messages outbox
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ProposalSlipService(
      ProposalService proposals,
      InsurerResponseRepository responses,
      ProposalNumbers numbers,
      ProposalDocuments slips,
      DocumentService documents,
      MessageService messages,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.responses = responses;
    this.numbers = numbers;
    this.slips = slips;
    this.documents = documents;
    this.messages = messages;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits the proposal slip for approval with the chosen insurer's terms; a new version is
   * generated and archived on the PRF.
   *
   * @param id PRF
   * @param insurerCode chosen insurer; null for the recommended one
   * @param comment comment
   * @return the PRF, now PS_FOR_APPROVAL
   */
  public ProposalRequest submit(Long id, String insurerCode, String comment) {
    ProposalRequest p = proposals.get(id);
    InsurerResponse chosen = chosen(id, insurerCode);
    workflow.transition(
        ProposalService.ENTITY, String.valueOf(id), "submit_ps", TransitionNote.comment(comment));
    int version =
        p.prepareProposalSlip(
            p.getPsNo() == null ? numbers.proposalSlip() : p.getPsNo(),
            chosen.getInsurerCode(),
            currentUser.username());
    MessageFile slip = slips.proposalSlip(p, chosen);
    documents.upload(
        new AttachmentTarget(ProposalService.ENTITY, String.valueOf(id)),
        List.of(new UploadedFile(slip.fileName(), slip.content())),
        new UploadOptions(SLIP_DOCUMENT, false, p.getPsNo(), "Proposal slip version " + version));
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Proposal slip " + p.getPsNo() + " v" + version + " with " + chosen.getInsurerName());
    return p;
  }

  private InsurerResponse chosen(Long id, String insurerCode) {
    List<InsurerResponse> all = responses.findByProposalIdOrderById(id);
    InsurerResponse chosen =
        all.stream()
            .filter(
                r ->
                    insurerCode == null || insurerCode.isBlank()
                        ? r.isRecommended()
                        : r.getInsurerCode().equals(insurerCode))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PS_INSURER_REQUIRED",
                        "Choose the insurer (or flag the recommended one) for the proposal slip"));
    if (chosen.getStatus() != ResponseStatus.RECEIVED) {
      throw new BusinessRuleException(
          "PS_TERMS_MISSING", chosen.getInsurerName() + " has not quoted terms");
    }
    return chosen;
  }

  /**
   * Approves the proposal slip (four eyes) and releases it to Marketing.
   *
   * @param id PRF
   * @param comment comment
   * @return the PRF, now PS_RELEASED
   */
  public ProposalRequest approve(Long id, String comment) {
    ProposalRequest p = proposals.get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getPsSubmittedBy())) {
      throw new BusinessRuleException(
          "PS_FOUR_EYES", "The proposal slip is approved by another TSU officer");
    }
    workflow.transition(
        ProposalService.ENTITY, String.valueOf(id), "approve_ps", TransitionNote.comment(comment));
    p.markProposalSlipApproved(user);
    return p;
  }

  /**
   * Sends the released proposal slip and the comparative table to the client, password protected
   * with the password in a separate e-mail (BRNB.013).
   *
   * @param id PRF
   * @param email recipients and message
   * @return the PRF, now SENT_TO_CLIENT
   */
  public ProposalRequest sendToClient(Long id, ClientEmail email) {
    ProposalRequest p = proposals.get(id);
    workflow.transition(
        ProposalService.ENTITY,
        String.valueOf(id),
        "send_to_client",
        TransitionNote.comment("Sent to " + String.join(", ", email.to())));
    messages.queueEmail(
        new OutboundEmail(
            p.getCompanyId(),
            PURPOSE,
            email.to(),
            email.cc(),
            email.subject(),
            email.body(),
            List.of(slip(p), slips.comparativePdf(p, responses.findByProposalIdOrderById(id))),
            new Protection(null, true, email.passwordHint()),
            new RecordLink(ProposalService.ENTITY, String.valueOf(id), p.getPrfNo())));
    p.markSent(clock.instant());
    return p;
  }

  /**
   * The current proposal slip.
   *
   * @param id PRF
   * @return PDF
   */
  @Transactional(readOnly = true)
  public MessageFile pdf(Long id) {
    return slip(proposals.get(id));
  }

  private MessageFile slip(ProposalRequest p) {
    if (p.getChosenInsurer() == null) {
      throw new BusinessRuleException("PS_NOT_PREPARED", "No proposal slip has been prepared yet");
    }
    InsurerResponse chosen =
        responses
            .findByProposalIdAndInsurerCode(p.getId(), p.getChosenInsurer())
            .orElseThrow(
                () -> new BusinessRuleException("PS_TERMS_MISSING", "The chosen terms are gone"));
    return slips.proposalSlip(p, chosen);
  }

  /**
   * An e-mail to the client.
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body message
   * @param passwordHint how the password is built, may be null
   */
  public record ClientEmail(
      List<String> to, List<String> cc, String subject, String body, String passwordHint) {

    /** Defensive copies. */
    public ClientEmail {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
