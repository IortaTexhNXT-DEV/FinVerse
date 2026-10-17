package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A batch of For Renewal accounts sent to one insurer (BRD 3.009, 4.007): the insurer, the expiry
 * range, the extract file (attachment of type INSURER_RENEWAL_FILE), the protected e-mail and the
 * reply date (alert RNW_INSURER_OVERDUE after it).
 */
@Entity
@Table(name = "rnw_insurer_batch")
public class InsurerBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "expiry_from", nullable = false, updatable = false)
  private LocalDate expiryFrom;

  @Column(name = "expiry_to", nullable = false, updatable = false)
  private LocalDate expiryTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private InsurerBatchStatus status = InsurerBatchStatus.DRAFT;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "message_id")
  private Long messageId;

  @Column(length = 500)
  private String recipients;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "sent_by", length = 50)
  private String sentBy;

  @Column(name = "reply_due")
  private LocalDate replyDue;

  protected InsurerBatch() {}

  /**
   * Creates a draft batch.
   *
   * @param companyId company
   * @param batchNo batch number (RIB-yyyy-nnnnnn)
   * @param insurerCode insurer
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   */
  public InsurerBatch(
      Long companyId,
      String batchNo,
      String insurerCode,
      LocalDate expiryFrom,
      LocalDate expiryTo) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.insurerCode = insurerCode;
    this.expiryFrom = expiryFrom;
    this.expiryTo = expiryTo;
  }

  /**
   * Records the number of accounts and the extract file.
   *
   * @param lines number of accounts
   * @param fileId attachment of the extract
   */
  public void built(int lines, Long fileId) {
    this.lineCount = lines;
    this.attachmentId = fileId;
  }

  /**
   * Records the sending.
   *
   * @param message outbox message
   * @param to recipients
   * @param user sender
   * @param at time
   * @param due reply date
   */
  public void sent(Long message, String to, String user, Instant at, LocalDate due) {
    this.messageId = message;
    this.recipients = to;
    this.sentBy = user;
    this.sentAt = at;
    this.replyDue = due;
    this.status = InsurerBatchStatus.SENT;
  }

  /**
   * Updates the status after responses.
   *
   * @param allAnswered every account answered
   */
  public void responded(boolean allAnswered) {
    this.status = allAnswered ? InsurerBatchStatus.CLOSED : InsurerBatchStatus.PARTIALLY_RESPONDED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public LocalDate getExpiryFrom() {
    return expiryFrom;
  }

  public LocalDate getExpiryTo() {
    return expiryTo;
  }

  public InsurerBatchStatus getStatus() {
    return status;
  }

  public int getLineCount() {
    return lineCount;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getRecipients() {
    return recipients;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentBy() {
    return sentBy;
  }

  public LocalDate getReplyDue() {
    return replyDue;
  }
}
