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
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.LetterSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.port.RecipientPolicy;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecord;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecordRepository;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintHandOver;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintRequest;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintedLetter;
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
  private final PrintChannel print;
  private final LetterDelivery delivery;

  /**
   * The mail house of the letters of submitted policies without an e-mail (wave R3).
   *
   * @param mailHouse print hand-over of Submitted Policies
   * @param handOffs terms of the submitted policies handed over (mailing address)
   */
  public record PrintChannel(
      MailHouseGateway mailHouse, SubmittedHandOffRecordRepository handOffs) {}

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
   * @param mailHouse print hand-over of Submitted Policies
   * @param handOffs terms of the submitted policies handed over
   * @param delivery file name, password and CCM
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
      Clock clock,
      MailHouseGateway mailHouse,
      SubmittedHandOffRecordRepository handOffs,
      LetterDelivery delivery) {
    this.letters = letters;
    this.content = content;
    this.documents = documents;
    this.messages = messages;
    this.recipients = recipients;
    this.accounts = accounts;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
    this.print = new PrintChannel(mailHouse, handOffs);
    this.delivery = delivery;
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
    Long fileId = store(c, kind.type(), delivery.fileName(c, letter), rendered.pdf());
    letter.stored(fileId);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        label(kind)
            + " "
            + no
            + " generated (template version "
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
    if ((email == null || email.isBlank()) && c.getSource() == CandidateSource.SUBMITTED_POLICY) {
      return printed(c, letter);
    }
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
    if (delivery.ccm()) {
      return toCcm(
          c, letter, new LetterDelivery.Mail(email.strip(), rendered.subject(), rendered.body()));
    }
    QueuedEmail queued = mail(c, letter, email.strip(), rendered);
    letter.queued(queued.messageId(), email.strip(), true);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        letter.getLetterNo() + " sent to " + email.strip() + " (protected)");
    return Optional.empty();
  }

  private Optional<String> toCcm(
      RenewalCandidate c, RenewalLetter letter, LetterDelivery.Mail mail) {
    LetterDelivery.Handoff handoff = delivery.toCcm(c, letter, mail);
    letter.queued(null, mail.to(), true);
    if (handoff.failure().isPresent()) {
      letter.delivered(false, handoff.failure().get(), clock.instant());
      return handoff.failure();
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        letter.getLetterNo()
            + " submitted to CCM for "
            + mail.to()
            + " ("
            + handoff.messageNo()
            + ")");
    return Optional.empty();
  }

  private QueuedEmail mail(
      RenewalCandidate c, RenewalLetter letter, String email, LetterContent.Rendered rendered) {
    return messages.queueEmail(
        new OutboundEmail(
            c.getCompanyId(),
            RenewalCodes.PURPOSE_LETTER,
            List.of(email),
            List.of(),
            rendered.subject(),
            rendered.body(),
            List.of(new MessageFile(delivery.fileName(c, letter), PDF, rendered.pdf())),
            delivery.protection(
                c.getSnapshot().client() == null ? null : c.getSnapshot().client().clientId()),
            new RecordLink(RenewalCodes.ENTITY, c.getId().toString(), c.getRenewalRef())));
  }

  /**
   * Sends a letter again (delivery failed or the client asks for another copy): a new transmission
   * of the same stored letter; the letter and its attachment stay as they are, the resend is in the
   * audit trail (FRRN.022.01, FRRN.023.02).
   *
   * @param c renewal
   * @param letter letter already sent or failed
   * @return the refusal, empty when queued
   */
  public Optional<String> resend(RenewalCandidate c, RenewalLetter letter) {
    String email = c.getSnapshot().client() == null ? null : c.getSnapshot().client().email();
    if (email == null || email.isBlank()) {
      return Optional.of("The client of " + c.getRenewalRef() + " has no registered e-mail");
    }
    LetterContent.Rendered rendered =
        content.render(
            c,
            letter.getType(),
            letter.getNotice(),
            letter.getLetterNo(),
            BusinessClock.dateOf(letter.getGeneratedAt()));
    String channel;
    if (delivery.ccm()) {
      LetterDelivery.Handoff handoff =
          delivery.toCcm(
              c,
              letter,
              new LetterDelivery.Mail(email.strip(), rendered.subject(), rendered.body()));
      if (handoff.failure().isPresent()) {
        return handoff.failure();
      }
      channel = "CCM " + handoff.messageNo();
    } else {
      channel = "e-mail " + mail(c, letter, email.strip(), rendered).messageId();
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        letter.getLetterNo() + " resent to " + email.strip() + " by " + channel);
    return Optional.empty();
  }

  /**
   * Hands the letter of a submitted policy without an e-mail to the mail house (print batch of
   * Submitted Policies, wave R3).
   */
  private Optional<String> printed(RenewalCandidate c, RenewalLetter letter) {
    Long storedFileId =
        letter.getAttachmentId() == null
            ? null
            : documents.downloadable(letter.getAttachmentId()).storedFileId();
    if (storedFileId == null) {
      String reason = "The letter " + letter.getLetterNo() + " has no stored file to print";
      letter.refused(reason);
      return Optional.of(reason);
    }
    String address =
        print
            .handOffs()
            .findByCandidateId(c.getId())
            .map(SubmittedHandOffRecord::getMailingAddress)
            .orElse(null);
    String addressee =
        c.getSnapshot().client() == null ? null : c.getSnapshot().client().assuredName();
    PrintHandOver batch =
        print
            .mailHouse()
            .handOver(
                new PrintRequest(
                    c.getCompanyId(),
                    "RENEWAL",
                    letter.getType().name(),
                    BusinessClock.today(clock),
                    List.of(
                        new PrintedLetter(
                            letter.getLetterNo(), addressee, address, storedFileId))));
    letter.printed(batch.batchNo(), clock.instant());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        letter.getLetterNo() + " printed in mail house batch " + batch.batchNo());
    return Optional.empty();
  }

  private Long store(RenewalCandidate c, LetterType type, String fileName, byte[] pdf) {
    String no = fileName.substring(0, fileName.length() - ".pdf".length());
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
                List.of(new UploadedFile(fileName, pdf)),
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
