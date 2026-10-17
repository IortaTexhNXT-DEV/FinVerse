package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A request for proposal sent to an insurer (BRID-009; FR-EB-035), numbered {@code
 * EBR-<yyyy>-nnnnnn}: the TOR version, documents and due date, sent by e-mail. It is answered by
 * the insurer's proposal, declined, or closed by the AO before the comparative.
 */
@Entity
@Table(name = "eb_insurer_request")
public class EbInsurerRequest extends EbCycleRecord {

  /** Status of a request. */
  public enum Status {
    /** Waiting for the proposal. */
    OPEN,
    /** Proposal received. */
    RESPONDED,
    /** The insurer declined to quote. */
    DECLINED,
    /** Closed by the AO without a proposal. */
    CLOSED
  }

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "tor_id", nullable = false)
  private Long torId;

  @Column(name = "tor_version", nullable = false)
  private int torVersion;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  @Column(name = "sent_by", nullable = false, length = 50)
  private String sentBy;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, length = 10)
  private String channel;

  @Column(name = "message_id")
  private Long messageId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  @Column(name = "closed_reason", length = 500)
  private String closedReason;

  protected EbInsurerRequest() {}

  /**
   * Records a request sent.
   *
   * @param cycle cycle
   * @param requestNo number
   * @param insurerCode insurer
   * @param tor released TOR version
   * @param sending time, user and due date
   */
  public EbInsurerRequest(
      EbCycle cycle, String requestNo, String insurerCode, EbTor tor, Sending sending) {
    super(cycle);
    this.requestNo = requestNo;
    this.insurerCode = insurerCode;
    this.torId = tor.getId();
    this.torVersion = tor.getVersionNo();
    this.channel = "EMAIL";
    this.sentAt = sending.at();
    this.sentBy = sending.by();
    this.dueDate = sending.dueDate();
  }

  /**
   * Keeps the outbox message.
   *
   * @param message message id
   */
  public void sentAs(Long message) {
    this.messageId = message;
  }

  /**
   * A later TOR version was sent on this open request.
   *
   * @param tor released TOR version
   */
  public void updateTor(EbTor tor) {
    this.torId = tor.getId();
    this.torVersion = tor.getVersionNo();
  }

  /** The insurer's proposal was recorded. */
  public void responded() {
    this.status = Status.RESPONDED;
  }

  /**
   * Closes the request without a proposal.
   *
   * @param declined whether the insurer declined (else closed by the AO)
   * @param reason why
   */
  public void close(boolean declined, String reason) {
    if (status != Status.OPEN) {
      throw new BusinessRuleException(
          "EB_REQUEST_NOT_OPEN", "Request " + requestNo + " is not open any more");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "EB_REQUEST_REASON_REQUIRED", "Enter why the request is closed");
    }
    this.status = declined ? Status.DECLINED : Status.CLOSED;
    this.closedReason = reason.strip();
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public Long getTorId() {
    return torId;
  }

  public int getTorVersion() {
    return torVersion;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentBy() {
    return sentBy;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getChannel() {
    return channel;
  }

  public Long getMessageId() {
    return messageId;
  }

  public Status getStatus() {
    return status;
  }

  public String getClosedReason() {
    return closedReason;
  }

  /**
   * When and by whom a request is sent.
   *
   * @param at time
   * @param by user
   * @param dueDate reply due date
   */
  public record Sending(Instant at, String by, LocalDate dueDate) {}
}
