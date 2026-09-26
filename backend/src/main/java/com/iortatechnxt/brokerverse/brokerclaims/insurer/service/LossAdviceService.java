package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The loss advice to the insurers (process p.24-25, NFR 15.14, BRCLM.041; FR-CM-024): the PLA /
 * formal loss advice composed from template {@code BCL_LOSS_ADVICE} for each insurer of the claim,
 * e-mailed to the recipients the officer confirms (the insurer's placement mailboxes are proposed:
 * the insurer master has no claims mailbox yet, CLQ11 / CLQ22) with a send log on the claim, and
 * the generated PDF stored with the claim as document type {@code CLAIM_REPORT}, linked to the
 * account and the client so the contact centre finds it (decision D3, BRCSF-009).
 */
@Service
@Transactional
public class LossAdviceService {

  /** Purpose of the loss advice e-mails in the outbox. */
  public static final String PURPOSE = "BCL_LOSS_ADVICE";

  /** Document type of the stored advice (decision D3). */
  public static final String DOCUMENT_TYPE = "CLAIM_REPORT";

  private final InsurerClaimService lines;
  private final LossAdviceDocument document;
  private final InsurerService insurers;
  private final CoverService covers;
  private final DocumentService documents;
  private final MessageService messages;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lines insurer lines
   * @param document advice composer
   * @param insurers insurer names and mailboxes
   * @param covers account and client of the cover
   * @param documents claim documents
   * @param messages e-mail outbox
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LossAdviceService(
      InsurerClaimService lines,
      LossAdviceDocument document,
      InsurerService insurers,
      CoverService covers,
      DocumentService documents,
      MessageService messages,
      AuditTrailService audit,
      Clock clock) {
    this.lines = lines;
    this.document = document;
    this.insurers = insurers;
    this.covers = covers;
    this.documents = documents;
    this.messages = messages;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The advice of each insurer of the claim, for review before sending.
   *
   * @param claim claim
   * @return one draft per insurer
   */
  @Transactional(readOnly = true)
  public List<Draft> drafts(Claim claim) {
    List<InsurerClaim> all = lines.ofClaim(claim.getId());
    List<Draft> drafts = new ArrayList<>();
    for (String code : insurerCodes(all)) {
      InsurerProfile insurer = insurers.requireInsurer(claim.getCompanyId(), code);
      LossAdviceDocument.Advice advice =
          document.compose(claim, insurer, all, LocalDate.now(clock));
      drafts.add(
          new Draft(
              code,
              insurer.getName(),
              insurer.getPlacementEmailList(),
              advice.subject(),
              advice.body()));
    }
    return drafts;
  }

  /**
   * E-mails the advice to the chosen insurers, one e-mail each, and stores each generated advice as
   * a claims report (R2).
   *
   * @param claim claim
   * @param recipients insurers with their recipients
   * @return what was sent
   */
  public List<Sent> send(Claim claim, List<Recipient> recipients) {
    if (recipients.isEmpty()) {
      throw new BusinessRuleException("BCL_LOSS_ADVICE_INSURER", "Select at least one insurer");
    }
    List<InsurerClaim> all = lines.ofClaim(claim.getId());
    Set<String> onClaim = insurerCodes(all);
    for (Recipient r : recipients) {
      if (!onClaim.contains(r.insurerCode())) {
        throw new BusinessRuleException(
            "BCL_LOSS_ADVICE_INSURER", "Insurer " + r.insurerCode() + " is not on this claim");
      }
      if (r.to().isEmpty()) {
        throw new BusinessRuleException(
            "BCL_RECIPIENT_REQUIRED", "Enter the recipient address for " + r.insurerCode());
      }
    }
    var account = covers.account(claim.getCompanyId(), claim.getCover().getArn());
    List<Sent> sent = new ArrayList<>();
    for (Recipient r : recipients) {
      sent.add(sendOne(claim, all, r, account.getId(), account.getClientId()));
    }
    return sent;
  }

  private Sent sendOne(
      Claim claim, List<InsurerClaim> all, Recipient r, Long accountId, Long clientId) {
    InsurerProfile insurer = insurers.requireInsurer(claim.getCompanyId(), r.insurerCode());
    LossAdviceDocument.Advice advice = document.compose(claim, insurer, all, LocalDate.now(clock));
    byte[] pdf = document.pdf(claim, advice);
    String fileName = claim.getClaimNo() + "_LOSS_ADVICE_" + r.insurerCode() + ".pdf";
    Attachment stored =
        documents
            .upload(
                new AttachmentTarget(ClaimCodes.ENTITY_TYPE, String.valueOf(claim.getId())),
                List.of(new UploadedFile(fileName, pdf)),
                new UploadOptions(
                    DOCUMENT_TYPE, true, claim.getClaimNo(), "Loss advice to " + insurer.getName()))
            .get(0);
    List<AttachmentTarget> links = new ArrayList<>();
    if (accountId != null) {
      links.add(new AttachmentTarget("Account", String.valueOf(accountId)));
    }
    if (clientId != null) {
      links.add(new AttachmentTarget("Client", String.valueOf(clientId)));
    }
    documents.link(stored.getId(), links);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                claim.getCompanyId(),
                PURPOSE,
                r.to(),
                r.cc(),
                r.subject() == null || r.subject().isBlank() ? advice.subject() : r.subject(),
                r.body() == null || r.body().isBlank() ? advice.body() : r.body(),
                List.of(new MessageFile(stored.getFileName(), "application/pdf", pdf)),
                null,
                new RecordLink(
                    ClaimCodes.ENTITY_TYPE, String.valueOf(claim.getId()), claim.getClaimNo())));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Loss advice to "
            + r.insurerCode()
            + " ("
            + String.join(", ", r.to())
            + ") as "
            + stored.getFileName());
    return new Sent(r.insurerCode(), queued.messageId(), stored.getId(), stored.getFileName());
  }

  private static Set<String> insurerCodes(List<InsurerClaim> all) {
    Set<String> codes = new LinkedHashSet<>();
    all.forEach(l -> codes.add(l.getInsurerCode()));
    return codes;
  }

  /**
   * The loss advice of one insurer.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param suggestedTo proposed recipients (the insurer's mailboxes), may be empty
   * @param subject subject
   * @param body body
   */
  public record Draft(
      String insurerCode,
      String insurerName,
      List<String> suggestedTo,
      String subject,
      String body) {

    /** Defensive copy. */
    public Draft {
      suggestedTo = List.copyOf(suggestedTo);
    }
  }

  /**
   * The recipients of one insurer's advice.
   *
   * @param insurerCode insurer
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject, the template's when empty
   * @param body body, the template's when empty
   */
  public record Recipient(
      String insurerCode, List<String> to, List<String> cc, String subject, String body) {

    /** Defensive copies. */
    public Recipient {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }

  /**
   * One advice sent.
   *
   * @param insurerCode insurer
   * @param messageId outbox message
   * @param attachmentId stored claims report
   * @param fileName file name of the advice
   */
  public record Sent(String insurerCode, Long messageId, Long attachmentId, String fileName) {}
}
