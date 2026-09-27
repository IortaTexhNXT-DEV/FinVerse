package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A letter of this module (BRIDSP-22): a reminder, renewal notice or renewal proposal sent by
 * e-mail, through the bank counterpart or printed in a batch for the mail house. The renewal
 * letters of handed-over records are the Renewal module's.
 */
@Entity
@Table(name = "sbm_letter")
public class SbmLetter extends BaseEntity {

  /** Queued. */
  public static final String QUEUED = "QUEUED";

  /** Generated, waiting for its print batch. */
  public static final String GENERATED = "GENERATED";

  /** Sent by e-mail. */
  public static final String SENT = "SENT";

  /** Printed in a batch. */
  public static final String PRINTED = "PRINTED";

  /** Not generated or not sent. */
  public static final String FAILED = "FAILED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "letter_no", nullable = false, updatable = false, length = 30)
  private String letterNo;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "rule_id", updatable = false)
  private Long ruleId;

  @Column(name = "letter_type", nullable = false, updatable = false, length = 30)
  private String letterType;

  @Column(nullable = false, length = 20)
  private String channel;

  @Column(nullable = false, length = 20)
  private String status = QUEUED;

  @Column(length = 300)
  private String recipient;

  @Column(name = "template_code", nullable = false, length = 40)
  private String templateCode;

  @Column(name = "template_version")
  private Integer templateVersion;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "print_batch_id")
  private Long printBatchId;

  @Column(length = 500)
  private String error;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected SbmLetter() {}

  /**
   * A letter.
   *
   * @param companyId company
   * @param letterNo number
   * @param policyId record
   * @param kind rule, type, channel and template
   */
  public SbmLetter(Long companyId, String letterNo, Long policyId, Kind kind) {
    this.companyId = companyId;
    this.letterNo = letterNo;
    this.policyId = policyId;
    this.ruleId = kind.ruleId();
    this.letterType = kind.letterType();
    this.channel = kind.channel();
    this.templateCode = kind.templateCode();
  }

  /**
   * The letter was generated.
   *
   * @param version template version
   * @param attachment attachment of the PDF on the record
   * @param fileId stored file of the PDF
   * @param to recipient
   */
  public void generated(int version, Long attachment, Long fileId, String to) {
    this.templateVersion = version;
    this.attachmentId = attachment;
    this.storedFileId = fileId;
    this.recipient = to;
    this.status = GENERATED;
  }

  /**
   * Sent by e-mail.
   *
   * @param message outbound message
   * @param at time
   */
  public void sent(Long message, Instant at) {
    this.messageId = message;
    this.sentAt = at;
    this.status = SENT;
    this.error = null;
  }

  /**
   * Printed in a batch.
   *
   * @param batch print batch
   * @param at time
   */
  public void printed(Long batch, Instant at) {
    this.printBatchId = batch;
    this.sentAt = at;
    this.status = PRINTED;
  }

  /**
   * Failed.
   *
   * @param why reason
   */
  public void failed(String why) {
    this.status = FAILED;
    this.error = why != null && why.length() > 500 ? why.substring(0, 500) : why;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLetterNo() {
    return letterNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public String getLetterType() {
    return letterType;
  }

  public String getChannel() {
    return channel;
  }

  public String getStatus() {
    return status;
  }

  public String getRecipient() {
    return recipient;
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

  public Long getStoredFileId() {
    return storedFileId;
  }

  public Long getMessageId() {
    return messageId;
  }

  public Long getPrintBatchId() {
    return printBatchId;
  }

  public String getError() {
    return error;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  /**
   * What letter it is.
   *
   * @param ruleId letter rule, null for a letter sent by hand
   * @param letterType letter type
   * @param channel channel
   * @param templateCode template
   */
  public record Kind(Long ruleId, String letterType, String channel, String templateCode) {}
}
