package com.iortatechnxt.brokerverse.payrequest.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A validation task of a refund of a cancelled policy (MKT 1.11.0, ACSL 2.5.5): ACSL checks the
 * cancelled premium and the insurer's return, Cashiering confirms the reinstatement to the
 * unapplied list with a new AR number. One task per validator and refund line and per round; the
 * source reference {@code <request>#<round>:<line>} is the idempotency key of the port.
 */
@Entity
@Table(name = "prq_validation")
public class RefundValidation extends BaseEntity {

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(nullable = false, length = 20, updatable = false)
  private String validator;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ValidationStatus status;

  @Column(name = "ticket_ref", length = 40)
  private String ticketRef;

  @Column(length = 500)
  private String message;

  @Column(name = "new_ar_no", length = 40)
  private String newArNo;

  @Column(length = 500)
  private String remarks;

  @Column(name = "completed_by", length = 50)
  private String completedBy;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected RefundValidation() {}

  /**
   * Opens a task.
   *
   * @param requestId request
   * @param roundNo validation round
   * @param lineNo refund line
   * @param validator ACSL or CASHIERING
   * @param sourceRef idempotency key sent to the validator
   */
  public RefundValidation(
      Long requestId, int roundNo, int lineNo, String validator, String sourceRef) {
    this.requestId = requestId;
    this.roundNo = roundNo;
    this.lineNo = lineNo;
    this.validator = validator;
    this.sourceRef = sourceRef;
    this.status = ValidationStatus.OPEN;
  }

  /**
   * Records the validator's answer to the opening.
   *
   * @param deferred true when handed over
   * @param reference ACSL case, cashiering reference or hand-off
   * @param text message
   */
  public void opened(boolean deferred, String reference, String text) {
    this.status = deferred ? ValidationStatus.DEFERRED : ValidationStatus.OPEN;
    this.ticketRef = reference;
    this.message = text;
  }

  /**
   * Records the result.
   *
   * @param confirmed true when confirmed
   * @param arNo new AR number (Cashiering reinstatement), may be null
   * @param text remarks
   * @param user who recorded it (the validator, or the user entering a handed-over result)
   * @param at when
   */
  public void complete(boolean confirmed, String arNo, String text, String user, Instant at) {
    this.status = confirmed ? ValidationStatus.CONFIRMED : ValidationStatus.REJECTED;
    this.newArNo = arNo;
    this.remarks = text;
    this.completedBy = user;
    this.completedAt = at;
  }

  public Long getRequestId() {
    return requestId;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getValidator() {
    return validator;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public ValidationStatus getStatus() {
    return status;
  }

  public String getTicketRef() {
    return ticketRef;
  }

  public String getMessage() {
    return message;
  }

  public String getNewArNo() {
    return newArNo;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getCompletedBy() {
    return completedBy;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
