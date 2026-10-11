package com.iortatechnxt.brokerverse.renewal.channel.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A message of a delivery channel (CCM to clients, MFT with insurers, or e-mail): the document it
 * carries, its recipients, the external transaction reference and its delivery status.
 */
@Entity
@Table(name = "rnw_channel_message")
public class ChannelMessage extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "message_no", nullable = false, length = 30, updatable = false)
  private String messageNo;

  @Column(nullable = false, length = 10, updatable = false)
  private String channel;

  @Column(nullable = false, length = 10, updatable = false)
  private String direction;

  @Column(name = "doc_kind", nullable = false, length = 30, updatable = false)
  private String docKind;

  @Column(name = "doc_ref", length = 60, updatable = false)
  private String docRef;

  @Column(name = "candidate_id", updatable = false)
  private Long candidateId;

  @Column(name = "renewal_ref", length = 30, updatable = false)
  private String renewalRef;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "recipients_to", length = 1000)
  private String recipientsTo;

  @Column(name = "recipients_cc", length = 1000)
  private String recipientsCc;

  @Column(length = 300)
  private String subject;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "client_id")
  private Long clientId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ChannelStatus status;

  @Column(name = "external_ref", length = 80)
  private String externalRef;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  protected ChannelMessage() {}

  /**
   * A new outbound or inbound message.
   *
   * @param companyId company
   * @param messageNo number
   * @param route channel and direction
   * @param document what the message carries
   * @param address recipients and subject
   */
  public ChannelMessage(
      Long companyId, String messageNo, Route route, Document document, Address address) {
    this.companyId = companyId;
    this.messageNo = messageNo;
    this.channel = route.channel();
    this.direction = route.direction();
    this.docKind = document.kind();
    this.docRef = document.reference();
    this.candidateId = document.candidateId();
    this.renewalRef = document.renewalRef();
    this.fileName = document.fileName();
    this.attachmentId = document.attachmentId();
    this.recipientsTo = address.to();
    this.recipientsCc = address.cc();
    this.subject = address.subject();
    this.body = address.body();
    this.clientId = address.clientId();
    this.status =
        "INBOUND".equals(route.direction())
            ? ChannelStatus.RECEIVED
            : ChannelStatus.PENDING_TRANSMISSION;
  }

  /**
   * Records an attempt of the transmission.
   *
   * @param accepted whether the channel accepted the message
   * @param reference external transaction reference, when accepted
   * @param error error, when refused
   * @param at time
   */
  public void attempted(boolean accepted, String reference, String error, Instant at) {
    attempts++;
    if (accepted) {
      this.status = ChannelStatus.SUBMITTED;
      this.externalRef = reference;
      this.submittedAt = at;
      this.lastError = null;
    } else {
      this.lastError = error;
    }
  }

  /**
   * Moves the message to a status reported by the channel.
   *
   * @param next status
   * @param error error of a failure, may be null
   * @param at time
   */
  public void moveTo(ChannelStatus next, String error, Instant at) {
    this.status = next;
    if (next == ChannelStatus.SENT && sentAt == null) {
      this.sentAt = at;
    }
    if (next == ChannelStatus.DELIVERED) {
      this.deliveredAt = at;
      if (sentAt == null) {
        this.sentAt = at;
      }
    }
    if (error != null) {
      this.lastError = error;
    }
  }

  /** Puts a failed message back for a new transmission (resend), the same document. */
  public void requeue() {
    if (status != ChannelStatus.FAILED
        && status != ChannelStatus.DELIVERED
        && status != ChannelStatus.SENT) {
      throw new BusinessRuleException(
          "RNW_CHANNEL_RESEND", "Message " + messageNo + " is " + status.label());
    }
    this.status = ChannelStatus.PENDING_TRANSMISSION;
    this.attempts = 0;
    this.externalRef = null;
  }

  /** Withdraws a message not yet transmitted. */
  public void cancel() {
    if (status != ChannelStatus.PENDING_TRANSMISSION && status != ChannelStatus.FAILED) {
      throw new BusinessRuleException(
          "RNW_CHANNEL_CANCEL", "Message " + messageNo + " is " + status.label());
    }
    this.status = ChannelStatus.CANCELLED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getMessageNo() {
    return messageNo;
  }

  public String getChannel() {
    return channel;
  }

  public String getDirection() {
    return direction;
  }

  public String getDocKind() {
    return docKind;
  }

  public String getDocRef() {
    return docRef;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getRenewalRef() {
    return renewalRef;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getRecipientsTo() {
    return recipientsTo;
  }

  public String getRecipientsCc() {
    return recipientsCc;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public Long getClientId() {
    return clientId;
  }

  public ChannelStatus getStatus() {
    return status;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  /**
   * Channel and direction.
   *
   * @param channel CCM, MFT or EMAIL
   * @param direction OUTBOUND or INBOUND
   */
  public record Route(String channel, String direction) {}

  /**
   * What a message carries.
   *
   * @param kind kind of document (RA, NRL, PROPOSAL, IA, EPOLICY, PLACEMENT...)
   * @param reference document reference (letter number...)
   * @param candidateId renewal account, may be null
   * @param renewalRef renewal reference, may be null
   * @param fileName file name
   * @param attachmentId stored file, may be null
   */
  public record Document(
      String kind,
      String reference,
      Long candidateId,
      String renewalRef,
      String fileName,
      Long attachmentId) {}

  /**
   * Recipients, subject and text, and the client whose password protects the file.
   *
   * @param to recipients, comma-separated (the insurer for MFT)
   * @param cc copy recipients, comma-separated, may be null
   * @param subject subject
   * @param body text of the communication, may be null
   * @param clientId client of the password, may be null
   */
  public record Address(String to, String cc, String subject, String body, Long clientId) {}
}
