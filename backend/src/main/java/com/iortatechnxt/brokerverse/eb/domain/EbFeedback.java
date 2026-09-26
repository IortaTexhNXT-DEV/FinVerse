package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The client's feedback on a cycle (BRID-002, 003; FR-EB-023): channel, date received, the text and
 * / or the files (stored as {@code EB_CLIENT_FEEDBACK} documents of the cycle). Never changed after
 * it is recorded.
 */
@Entity
@Table(name = "eb_feedback")
public class EbFeedback extends BaseEntity {

  /** Longest feedback text. */
  public static final int MAX_TEXT = 4000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", nullable = false, updatable = false)
  private Long cycleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private EbFeedbackChannel channel;

  @Column(name = "received_on", nullable = false, updatable = false)
  private LocalDate receivedOn;

  @Column(name = "feedback_text", length = MAX_TEXT, updatable = false)
  private String text;

  @Column(name = "file_count", nullable = false, updatable = false)
  private int fileCount;

  protected EbFeedback() {}

  /**
   * Records feedback; text or at least one file is needed.
   *
   * @param cycle cycle
   * @param channel how it arrived
   * @param receivedOn date received
   * @param text feedback text, may be blank when files are given
   * @param fileCount number of files attached
   */
  public EbFeedback(
      EbCycle cycle, EbFeedbackChannel channel, LocalDate receivedOn, String text, int fileCount) {
    String trimmed = text == null || text.isBlank() ? null : text.strip();
    if (trimmed == null && fileCount == 0) {
      throw new BusinessRuleException("EB_FEEDBACK_EMPTY", "Enter the feedback or attach a file");
    }
    if (trimmed != null && trimmed.length() > MAX_TEXT) {
      throw new BusinessRuleException(
          "EB_FEEDBACK_TOO_LONG", "The feedback can have at most " + MAX_TEXT + " characters");
    }
    this.companyId = cycle.getCompanyId();
    this.programmeId = cycle.getProgrammeId();
    this.cycleId = cycle.getId();
    this.channel = channel;
    this.receivedOn = receivedOn;
    this.text = trimmed;
    this.fileCount = fileCount;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public EbFeedbackChannel getChannel() {
    return channel;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public String getText() {
    return text;
  }

  public int getFileCount() {
    return fileCount;
  }
}
