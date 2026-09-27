package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A renewal letter (RENEWAL_DESIGN section 4.5): Renewal Advice (first or second notice), No Advice
 * Letter, Not for Renewal Letter, NRNS reminder or non-acceptance letter. It records the template
 * code and version, the stored document (RENEWAL_ADVICE or RENEWAL_LETTER), the protected e-mail
 * and its delivery status. A Renewal Advice sent by hand before go-live is recorded with source
 * LEGACY_MANUAL and the tracker reference and date; it is never sent again.
 */
@Entity
@Table(name = "rnw_letter")
public class RenewalLetter extends BaseEntity {

  private static final int FAILURE_LENGTH = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "letter_no", nullable = false, length = 30, updatable = false)
  private String letterNo;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "letter_type", nullable = false, length = 20, updatable = false)
  private LetterType type;

  @Enumerated(EnumType.STRING)
  @Column(length = 10, updatable = false)
  private RaNotice notice;

  @Column(name = "template_code", length = 40, updatable = false)
  private String templateCode;

  @Column(name = "template_version", updatable = false)
  private Integer templateVersion;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private LetterSource source;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LetterStatus status = LetterStatus.GENERATED;

  @Column(name = "message_id")
  private Long messageId;

  @Column(length = 500)
  private String recipients;

  @Column(name = "protected", nullable = false)
  private boolean protectedFile;

  @Column(name = "batch_id")
  private Long batchId;

  @Column(name = "generated_at", nullable = false, updatable = false)
  private Instant generatedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(length = FAILURE_LENGTH)
  private String failure;

  @Column(name = "min_notice_confirmed_by", length = 50, updatable = false)
  private String minNoticeConfirmedBy;

  @Column(name = "legacy_ref", length = 60, updatable = false)
  private String legacyRef;

  @Column(name = "legacy_date", updatable = false)
  private LocalDate legacyDate;

  @Column(length = 20, updatable = false)
  private String channel;

  protected RenewalLetter() {}

  /**
   * Records a generated letter.
   *
   * @param candidate renewal
   * @param letterNo letter number
   * @param kind type and notice
   * @param template template code and version
   * @param generation source, batch, late-notice confirmation and time
   */
  public RenewalLetter(
      RenewalCandidate candidate,
      String letterNo,
      LetterBatch.Kind kind,
      Template template,
      Generation generation) {
    this.companyId = candidate.getCompanyId();
    this.candidateId = candidate.getId();
    this.letterNo = letterNo;
    this.type = kind.type();
    this.notice = kind.notice();
    this.templateCode = template == null ? null : template.code();
    this.templateVersion = template == null ? null : template.version();
    this.source = generation.source();
    this.batchId = generation.batchId();
    this.minNoticeConfirmedBy = generation.lateConfirmedBy();
    this.generatedAt = generation.at();
  }

  /**
   * A Renewal Advice sent by hand before go-live (DMQ37): recorded as SENT with the tracker data.
   *
   * @param candidate renewal
   * @param letterNo letter number
   * @param legacy tracker reference, date, channel and recipient
   * @param at time recorded
   * @return the letter
   */
  public static RenewalLetter legacySent(
      RenewalCandidate candidate, String letterNo, Legacy legacy, Instant at) {
    RenewalLetter letter =
        new RenewalLetter(
            candidate,
            letterNo,
            new LetterBatch.Kind(LetterType.RA, RaNotice.FIRST),
            null,
            new Generation(LetterSource.LEGACY_MANUAL, null, null, at));
    letter.legacyRef = legacy.reference();
    letter.legacyDate = legacy.sentOn();
    letter.channel = legacy.channel();
    letter.recipients = legacy.recipient();
    letter.status = LetterStatus.SENT;
    letter.sentAt = at;
    return letter;
  }

  /**
   * Records the stored document.
   *
   * @param fileId attachment
   */
  public void stored(Long fileId) {
    this.attachmentId = fileId;
  }

  /**
   * Records the queued e-mail.
   *
   * @param message outbox message
   * @param to recipients
   * @param isProtected whether the file was protected
   */
  public void queued(Long message, String to, boolean isProtected) {
    requireStatus(LetterStatus.GENERATED, LetterStatus.FAILED);
    this.messageId = message;
    this.recipients = to;
    this.protectedFile = isProtected;
    this.status = LetterStatus.QUEUED;
    this.failure = null;
  }

  /**
   * Records the delivery outcome read from the outbox.
   *
   * @param delivered sent
   * @param error failure text when not delivered
   * @param at time
   */
  public void delivered(boolean delivered, String error, Instant at) {
    this.status = delivered ? LetterStatus.SENT : LetterStatus.FAILED;
    this.sentAt = delivered ? at : null;
    this.failure =
        error == null || error.length() <= FAILURE_LENGTH
            ? error
            : error.substring(0, FAILURE_LENGTH);
  }

  /**
   * Records a refusal before sending (recipient policy, no address).
   *
   * @param reason reason
   */
  public void refused(String reason) {
    this.failure = reason;
  }

  /** Cancels the letter (RA cancellation by override). */
  public void cancel() {
    this.status = LetterStatus.CANCELLED;
  }

  private void requireStatus(LetterStatus... allowed) {
    for (LetterStatus s : allowed) {
      if (status == s) {
        return;
      }
    }
    throw new BusinessRuleException(
        "RNW_LETTER_STATUS", "Letter " + letterNo + " is " + status + " and cannot be sent");
  }

  /**
   * Whether the letter was sent or is on its way.
   *
   * @return true when queued or sent
   */
  public boolean isOut() {
    return status == LetterStatus.QUEUED || status == LetterStatus.SENT;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLetterNo() {
    return letterNo;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public LetterType getType() {
    return type;
  }

  public RaNotice getNotice() {
    return notice;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public LetterSource getSource() {
    return source;
  }

  public LetterStatus getStatus() {
    return status;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getRecipients() {
    return recipients;
  }

  public boolean isProtectedFile() {
    return protectedFile;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getFailure() {
    return failure;
  }

  public String getMinNoticeConfirmedBy() {
    return minNoticeConfirmedBy;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public LocalDate getLegacyDate() {
    return legacyDate;
  }

  public String getChannel() {
    return channel;
  }

  /**
   * Template of a letter.
   *
   * @param code template code
   * @param version version
   */
  public record Template(String code, int version) {}

  /**
   * How a letter was generated.
   *
   * @param source system, user or legacy
   * @param batchId letter batch, may be null
   * @param lateConfirmedBy user who confirmed a late Renewal Advice, may be null
   * @param at time
   */
  public record Generation(LetterSource source, Long batchId, String lateConfirmedBy, Instant at) {}

  /**
   * A Renewal Advice sent by hand before go-live.
   *
   * @param reference RA reference of the tracker
   * @param sentOn date sent
   * @param channel channel
   * @param recipient recipient
   */
  public record Legacy(String reference, LocalDate sentOn, String channel, String recipient) {}
}
