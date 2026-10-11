package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The renewal advice of a renewal cycle (BRID-001, 002; design 4.2, 8.1): when and to whom it was
 * sent, by the job {@code EB_RENEWAL_ADVICE} or the AO, the outbox message, the stored {@code
 * RENEWAL_ADVICE} document, the reminders sent and the time the client's feedback stopped them. One
 * per cycle.
 */
@Entity
@Table(name = "eb_renewal_advice")
public class EbRenewalAdvice extends BaseEntity {

  /** Separator of the stored recipients. */
  private static final String SEPARATOR = ", ";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", nullable = false, updatable = false)
  private Long cycleId;

  @Column(name = "expiry_date", nullable = false, updatable = false)
  private LocalDate expiryDate;

  @Column(name = "sent_at", nullable = false, updatable = false)
  private Instant sentAt;

  @Column(name = "sent_by", nullable = false, length = 60, updatable = false)
  private String sentBy;

  @Column(name = "trigger_kind", nullable = false, length = 10, updatable = false)
  private String triggerKind;

  @Column(nullable = false, length = 1000, updatable = false)
  private String recipients;

  @Column(name = "message_id", updatable = false)
  private Long messageId;

  @Column(name = "attachment_id", updatable = false)
  private Long attachmentId;

  @Column(name = "reminders_sent", nullable = false)
  private int remindersSent;

  @Column(name = "last_reminder_at")
  private Instant lastReminderAt;

  @Column(name = "feedback_at")
  private Instant feedbackAt;

  protected EbRenewalAdvice() {}

  /**
   * Records a renewal advice that was sent.
   *
   * @param cycle the renewal cycle
   * @param expiryDate expiry of the programme lines it announces
   * @param sending when, by whom, how and to whom it was sent
   * @param messageId outbox message
   * @param attachmentId stored RENEWAL_ADVICE document
   */
  public EbRenewalAdvice(
      EbCycle cycle, LocalDate expiryDate, Sending sending, Long messageId, Long attachmentId) {
    this.companyId = cycle.getCompanyId();
    this.programmeId = cycle.getProgrammeId();
    this.cycleId = cycle.getId();
    this.expiryDate = expiryDate;
    this.sentAt = sending.at();
    this.sentBy = sending.by();
    this.triggerKind = sending.manual() ? "MANUAL" : "JOB";
    this.recipients = String.join(SEPARATOR, sending.recipients());
    this.messageId = messageId;
    this.attachmentId = attachmentId;
  }

  /**
   * Counts a reminder sent.
   *
   * @param when time sent
   */
  public void remind(Instant when) {
    this.remindersSent++;
    this.lastReminderAt = when;
  }

  /**
   * The client's feedback arrived: no further reminder (FR-EB-023).
   *
   * @param when time recorded
   */
  public void stopReminders(Instant when) {
    if (feedbackAt == null) {
      this.feedbackAt = when;
    }
  }

  /**
   * Whether reminders are still due.
   *
   * @return true until feedback is recorded
   */
  public boolean awaitingFeedback() {
    return feedbackAt == null;
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

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentBy() {
    return sentBy;
  }

  public boolean isManual() {
    return "MANUAL".equals(triggerKind);
  }

  /**
   * The recipients' e-mail addresses.
   *
   * @return addresses
   */
  public List<String> getRecipients() {
    return List.of(recipients.split(SEPARATOR));
  }

  public Long getMessageId() {
    return messageId;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public int getRemindersSent() {
    return remindersSent;
  }

  public Instant getLastReminderAt() {
    return lastReminderAt;
  }

  public Instant getFeedbackAt() {
    return feedbackAt;
  }

  /**
   * How a renewal advice was sent.
   *
   * @param at time sent
   * @param by user, or SYSTEM for the job
   * @param manual sent by the AO rather than the job
   * @param recipients e-mail addresses
   */
  public record Sending(Instant at, String by, boolean manual, List<String> recipients) {

    /** Defensive copy. */
    public Sending {
      recipients = List.copyOf(recipients);
    }
  }
}
