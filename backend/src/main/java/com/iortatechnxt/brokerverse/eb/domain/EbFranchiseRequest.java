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
 * A franchise request to an insurer (BRID-026, 027, 029; FR-EB-032, 033), numbered {@code
 * EBF-<yyyy>-nnnnnn}, with its {@code EB_FRANCHISE} work case whose stage it mirrors. Without the
 * partner portal the AO records the insurer's decision with the insurer's reply as evidence; the
 * client is then advised. Only an approved franchise lets the insurer receive the TOR.
 */
@Entity
@Table(name = "eb_franchise_request")
public class EbFranchiseRequest extends EbCycleRecord {

  /** Stage of the request (mirror of {@code EB_FRANCHISE}). */
  public enum Status {
    /** Created, not sent. */
    DRAFT,
    /** Sent, waiting for the insurer's decision. */
    SUBMITTED,
    /** Approved by the insurer. */
    APPROVED,
    /** Rejected by the insurer. */
    REJECTED,
    /** No decision within the TAT. */
    EXPIRED,
    /** Client advised of the outcome. */
    ADVISED
  }

  @Column(name = "franchise_no", nullable = false, length = 30, updatable = false)
  private String franchiseNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Status decision;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "decided_on")
  private LocalDate decidedOn;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "reason_code", length = 40)
  private String reasonCode;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "evidence_attachment_id")
  private Long evidenceAttachmentId;

  @Column(name = "advice_due_date")
  private LocalDate adviceDueDate;

  @Column(name = "advised_at")
  private Instant advisedAt;

  @Column(name = "advised_by", length = 50)
  private String advisedBy;

  protected EbFranchiseRequest() {}

  /**
   * Creates a draft request.
   *
   * @param cycle cycle
   * @param franchiseNo number
   * @param insurerCode insurer
   */
  public EbFranchiseRequest(EbCycle cycle, String franchiseNo, String insurerCode) {
    super(cycle);
    this.franchiseNo = franchiseNo;
    this.insurerCode = insurerCode;
  }

  /**
   * Records the sending to the insurer.
   *
   * @param at time sent
   * @param due decision due date
   * @param message outbox message, may be null
   */
  public void submitted(Instant at, LocalDate due, Long message) {
    this.submittedAt = at;
    this.dueDate = due;
    this.messageId = message;
  }

  /**
   * Records the insurer's decision.
   *
   * @param outcome APPROVED or REJECTED
   * @param decision date, user, reason (rejection), remarks and evidence
   * @param adviceDue date by which the client is advised
   */
  public void decide(Status outcome, Decision decision, LocalDate adviceDue) {
    if (status != Status.SUBMITTED) {
      throw new BusinessRuleException(
          "EB_FRANCHISE_NOT_WAITING", "Franchise request " + franchiseNo + " is not waiting");
    }
    this.decision = outcome;
    this.decidedOn = decision.decidedOn();
    this.decidedBy = decision.by();
    this.reasonCode = decision.reasonCode();
    this.remarks = decision.remarks();
    this.evidenceAttachmentId = decision.evidenceAttachmentId();
    this.adviceDueDate = adviceDue;
  }

  /**
   * Records the advice to the client.
   *
   * @param at time
   * @param by user
   */
  public void advised(Instant at, String by) {
    this.advisedAt = at;
    this.advisedBy = by;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param stage new stage
   */
  public void mirror(Status stage) {
    this.status = stage;
  }

  /**
   * Whether the insurer approved the franchise (also after the client was advised).
   *
   * @return true when approved
   */
  public boolean isApproved() {
    return decision == Status.APPROVED;
  }

  public String getFranchiseNo() {
    return franchiseNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public Status getStatus() {
    return status;
  }

  public Status getDecision() {
    return decision;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Long getMessageId() {
    return messageId;
  }

  public LocalDate getDecidedOn() {
    return decidedOn;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getEvidenceAttachmentId() {
    return evidenceAttachmentId;
  }

  public LocalDate getAdviceDueDate() {
    return adviceDueDate;
  }

  public Instant getAdvisedAt() {
    return advisedAt;
  }

  public String getAdvisedBy() {
    return advisedBy;
  }

  /**
   * The insurer's decision as recorded by the AO.
   *
   * @param decidedOn date of the insurer's reply
   * @param by user recording it
   * @param reasonCode rejection reason (list EB_FRANCHISE_REJECT_REASON), null on approval
   * @param remarks remarks, may be null
   * @param evidenceAttachmentId the stored reply of the insurer
   */
  public record Decision(
      LocalDate decidedOn, String by, String reasonCode, String remarks, Long evidenceAttachmentId) {}
}
