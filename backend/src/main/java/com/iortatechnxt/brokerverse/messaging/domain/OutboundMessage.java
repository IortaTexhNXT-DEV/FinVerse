package com.iortatechnxt.brokerverse.messaging.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An e-mail in the outbox. The row is the send log: recipients, subject, body, attempts, outcome
 * and time (BRNB.008). It is linked to the business record it concerns ({@code entityType}, {@code
 * entityId}, {@code reference}) so every module can show what was sent for a record.
 */
@Entity
@Table(name = "msg_outbound")
public class OutboundMessage extends BaseEntity {

  private static final int MAX_ERROR = 1000;

  @Column(name = "company_id")
  private Long companyId;

  @Column(nullable = false, length = 40)
  private String purpose;

  @Column(nullable = false, length = 1000)
  private String recipients;

  @Column(length = 1000)
  private String cc;

  @Column(nullable = false, length = 300)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageStatus status = MessageStatus.QUEUED;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error", length = MAX_ERROR)
  private String lastError;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(nullable = false)
  private boolean simulated;

  @Column(name = "entity_type", length = 60)
  private String entityType;

  @Column(name = "entity_id", length = 60)
  private String entityId;

  @Column(length = 60)
  private String reference;

  @Column(name = "password_for_id")
  private Long passwordForId;

  protected OutboundMessage() {}

  /**
   * Creates a queued e-mail.
   *
   * @param companyId company (may be null for platform messages)
   * @param purpose purpose code, e.g. {@code EPOLICY}
   * @param addressing recipients, copy, subject and body
   * @param link business record the message concerns
   */
  public OutboundMessage(Long companyId, String purpose, Addressing addressing, RecordLink link) {
    this.companyId = companyId;
    this.purpose = purpose;
    this.recipients = addressing.recipients();
    this.cc = addressing.cc();
    this.subject = addressing.subject();
    this.body = addressing.body();
    if (link != null) {
      this.entityType = link.entityType();
      this.entityId = link.entityId();
      this.reference = link.reference();
    }
  }

  /**
   * Marks the message as carrying the password of another message.
   *
   * @param documentMessageId message with the protected documents
   */
  public void carryPasswordFor(Long documentMessageId) {
    this.passwordForId = documentMessageId;
  }

  /**
   * Records a successful delivery.
   *
   * @param when delivery time
   * @param wasSimulated true when no real mail server was used
   */
  public void markSent(Instant when, boolean wasSimulated) {
    attempts++;
    this.status = MessageStatus.SENT;
    this.sentAt = when;
    this.simulated = wasSimulated;
    this.lastError = null;
  }

  /**
   * Records a failed attempt; the message fails for good after the last allowed attempt.
   *
   * @param error reason
   * @param maxAttempts allowed attempts
   */
  public void markAttemptFailed(String error, int maxAttempts) {
    attempts++;
    this.lastError = error == null ? "Unknown error" : truncate(error);
    if (attempts >= maxAttempts) {
      this.status = MessageStatus.FAILED;
    }
  }

  /** Queues a failed message again (user retry). */
  public void requeue() {
    if (status != MessageStatus.FAILED) {
      throw new BusinessRuleException("MESSAGE_NOT_FAILED", "Only a failed message can be resent");
    }
    this.status = MessageStatus.QUEUED;
    this.attempts = 0;
  }

  private static String truncate(String text) {
    return text.length() <= MAX_ERROR ? text : text.substring(0, MAX_ERROR);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getRecipients() {
    return recipients;
  }

  public String getCc() {
    return cc;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public MessageStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public boolean isSimulated() {
    return simulated;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getReference() {
    return reference;
  }

  public Long getPasswordForId() {
    return passwordForId;
  }

  /**
   * Who receives the message and what it says.
   *
   * @param recipients comma separated addresses
   * @param cc comma separated copy addresses, may be null
   * @param subject subject
   * @param body plain-text body
   */
  public record Addressing(String recipients, String cc, String subject, String body) {}

  /**
   * The business record a message concerns.
   *
   * @param entityType entity type, e.g. "Account"
   * @param entityId entity id
   * @param reference business reference shown in logs and reports (ARN, slip number...)
   */
  public record RecordLink(String entityType, String entityId, String reference) {}
}
