package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.LetterSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.port.RecipientPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Generation and dispatch of one renewal letter: numbered by type (RA, NAL, NFR, RNL for NRNS
 * reminders, NAC for non-acceptance), rendered from the template of its type, stored as a document
 * (type RENEWAL_ADVICE for an RA, RENEWAL_LETTER otherwise) linked to the renewal, the renewal
 * account and the client, and e-mailed to the client protected with a password sent separately.
 */
@Component
public class LetterWriter {

  private static final String PDF = "application/pdf";

  private final RenewalLetterRepository letters;
  private final LetterContent content;
  private final DocumentService documents;
  private final MessageService messages;
  private final RecipientPolicy recipients;
  private final AccountRepository accounts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the writer.
   *
   * @param letters letters
   * @param content content
   * @param documents documents
   * @param messages e-mail
   * @param recipients recipient policy
   * @param accounts renewal accounts (document links)
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LetterWriter(
      RenewalLetterRepository letters,
      LetterContent content,
      DocumentService documents,
      MessageService messages,
      RecipientPolicy recipients,
      AccountRepository accounts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.letters = letters;
    this.content = content;
    this.documents = documents;
    this.messages = messages;
    this.recipients = recipients;
    this.accounts = accounts;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Generates and stores a letter.
   *
   * @param c renewal
   * @param kind type and notice
   * @param source SYSTEM or USER
   * @param batchId letter batch, may be null
   * @param lateConfirmedBy user who confirmed a late RA, may be null
   * @return the letter
   */
  public RenewalLetter generate(
      RenewalCandidate c,
      LetterBatch.Kind kind,
      LetterSource source,
      Long batchId,
      String lateConfirmedBy) {
    LocalDate today = BusinessClock.today(clock);
    String no = numbers.next(prefix(kind.type()) + "-" + today.getYear());
    LetterContent.Rendered rendered = content.render(c, kind.type(), kind.notice(), no, today);
    RenewalLetter letter =
        letters.save(
            new RenewalLetter(
                c,
                no,
                kind,
                new RenewalLetter.Template(rendered.templateCode(), rendered.templateVersion()),
                new RenewalLetter.Generation(source, batchId, lateConfirmedBy, clock.instant())));
    Long fileId = store(c, kind.type(), no, rendered.pdf());
    letter.stored(fileId);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        label(kind)
            + " "
            + no
            + " generated (template "
            + rendered.templateCode()
            + " v"
            + rendered.templateVersion()
            + ")");
    return letter;
  }

  /**
   * E-mails a generated letter to the client, protected; a recipient refused by the policy keeps
   * the letter generated with the reason.
   *
   * @param c renewal
   * @param letter letter
   * @return the refusal, empty when queued
   */
  public Optional<String> send(RenewalCandidate c, RenewalLetter letter) {
    String email = c.getSnapshot().client() == null ? null : c.getSnapshot().client().email();
    Optional<String> refusal =
        email == null || email.isBlank()
            ? Optional.of("The client of " + c.getRenewalRef() + " has no registered e-mail")
            : recipients.refusal(email);
    if (refusal.isPresent()) {
      letter.refused(refusal.get());
      return refusal;
    }
    LetterContent.Rendered rendered =
        content.render(
            c,
            letter.getType(),
            letter.getNotice(),
            letter.getLetterNo(),
            BusinessClock.today(clock));
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                c.getCompanyId(),
                RenewalCodes.PURPOSE_LETTER,
                List.of(email.strip()),
                List.of(),
                rendered.subject(),
                rendered.body(),
                List.of(new MessageFile(letter.getLetterNo() + ".pdf", PDF, rendered.pdf())),
                new OutboundEmail.Protection(null, true, null),
                new RecordLink(RenewalCodes.ENTITY, c.getId().toString(), c.getRenewalRef())));
    letter.queued(queued.messageId(), email.strip(), true);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        letter.getLetterNo() + " sent to " + email.strip() + " (protected)");
    return Optional.empty();
  }

  private Long store(RenewalCandidate c, LetterType type, String no, byte[] pdf) {
    List<AttachmentTarget> targets = new ArrayList<>();
    if (c.getRenewalArn() != null) {
      accounts
          .findByArn(c.getRenewalArn())
          .ifPresent(a -> targets.add(new AttachmentTarget("Account", a.getId().toString())));
    }
    if (c.getSnapshot().client() != null && c.getSnapshot().client().clientId() != null) {
      targets.add(new AttachmentTarget("Client", c.getSnapshot().client().clientId().toString()));
    }
    Long id =
        documents
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
                List.of(new UploadedFile(no + ".pdf", pdf)),
                new UploadOptions(
                    type == LetterType.RA
                        ? RenewalCodes.DOC_RENEWAL_ADVICE
                        : RenewalCodes.DOC_RENEWAL_LETTER,
                    false,
                    no,
                    no,
                    null))
            .get(0)
            .getId();
    if (!targets.isEmpty()) {
      documents.link(id, targets);
    }
    return id;
  }

  private static String prefix(LetterType type) {
    return switch (type) {
      case RA -> "RA";
      case NAL -> "NAL";
      case NFR -> "NFR";
      case NRNS_REMINDER -> "RNL";
      case NON_ACCEPTANCE -> "NAC";
    };
  }

  /**
   * The name of a letter kind.
   *
   * @param kind type and notice
   * @return label
   */
  public static String label(LetterBatch.Kind kind) {
    return switch (kind.type()) {
      case RA ->
          kind.notice() == RaNotice.SECOND ? "Renewal Advice (second notice)" : "Renewal Advice";
      case NAL -> "No Advice Letter";
      case NFR -> "Not for Renewal letter";
      case NRNS_REMINDER -> "Renewal reminder";
      case NON_ACCEPTANCE -> "Non-acceptance letter";
    };
  }
}
