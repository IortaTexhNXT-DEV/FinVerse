package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A production register extract of a cycle (PRCID.001-008/011/020/034): booking period, trigger,
 * file name (PRODRECON_FILE_PATTERN), repository file and checksum, number of lines, and when and
 * to whom it was sent.
 */
@Entity
@Table(name = "prc_extract")
public class ReconExtract extends BaseEntity {

  @Column(name = "cycle_id", nullable = false, updatable = false)
  private Long cycleId;

  @Column(name = "extract_no", nullable = false, length = 30, updatable = false)
  private String extractNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private ExtractTrigger trigger;

  @Column(name = "booking_from", nullable = false, updatable = false)
  private LocalDate bookingFrom;

  @Column(name = "booking_to", nullable = false, updatable = false)
  private LocalDate bookingTo;

  @Column(name = "file_name", nullable = false, length = 200)
  private String fileName;

  @Column(name = "file_id")
  private Long fileId;

  @Column(length = 64)
  private String sha256;

  @Column(name = "row_count", nullable = false)
  private int rowCount;

  @Column(name = "new_count", nullable = false)
  private int newCount;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "sent_by", length = 50)
  private String sentBy;

  @Column(name = "message_id")
  private Long messageId;

  @Column(length = 500)
  private String recipients;

  protected ReconExtract() {}

  /**
   * A new extract.
   *
   * @param cycleId cycle
   * @param extractNo extract number
   * @param trigger scheduled or manual
   * @param bookingFrom first booking date
   * @param bookingTo last booking date
   */
  public ReconExtract(
      Long cycleId,
      String extractNo,
      ExtractTrigger trigger,
      LocalDate bookingFrom,
      LocalDate bookingTo) {
    this.cycleId = cycleId;
    this.extractNo = extractNo;
    this.trigger = trigger;
    this.bookingFrom = bookingFrom;
    this.bookingTo = bookingTo;
    this.fileName = extractNo;
  }

  /**
   * Records the generated file.
   *
   * @param name file name
   * @param id repository file id
   * @param checksum SHA-256
   * @param rows lines in the register
   * @param added lines new to the cycle
   */
  public void stored(String name, Long id, String checksum, int rows, int added) {
    this.fileName = name;
    this.fileId = id;
    this.sha256 = checksum;
    this.rowCount = rows;
    this.newCount = added;
  }

  /**
   * Records a sending (PRCID.008).
   *
   * @param at time sent
   * @param by user
   * @param message outbound message id
   * @param to recipients
   */
  public void sent(Instant at, String by, Long message, String to) {
    this.sentAt = at;
    this.sentBy = by;
    this.messageId = message;
    this.recipients = to;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public String getExtractNo() {
    return extractNo;
  }

  public ExtractTrigger getTrigger() {
    return trigger;
  }

  public LocalDate getBookingFrom() {
    return bookingFrom;
  }

  public LocalDate getBookingTo() {
    return bookingTo;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getFileId() {
    return fileId;
  }

  public String getSha256() {
    return sha256;
  }

  public int getRowCount() {
    return rowCount;
  }

  public int getNewCount() {
    return newCount;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentBy() {
    return sentBy;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getRecipients() {
    return recipients;
  }
}
