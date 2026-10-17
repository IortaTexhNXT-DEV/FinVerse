package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * The client's answer and the second approval of a renewal account (FRRN.019.01, FRRN.025.01,
 * FRRN.26.01): the client acceptance status with its remarks, and the approval of the accepted
 * account (Submitted for Approval, approved, or returned for correction) with the client's payment
 * confirmation.
 */
@Embeddable
public class CandidateDecision {

  /** Client status: waiting for the client. */
  public static final String PENDING = "PENDING";

  /** Client status: accepted. */
  public static final String ACCEPTED = "ACCEPTED";

  /** Client status: rejected. */
  public static final String REJECTED = "REJECTED";

  /** Client status: the client asked for a revision. */
  public static final String REVISION = "REVISION";

  /** Client status: no answer needed (CBG Home and FFY accounts). */
  public static final String NOT_APPLICABLE = "NOT_APPLICABLE";

  /** Approval: submitted for approval. */
  public static final String SUBMITTED = "SUBMITTED";

  /** Approval: approved. */
  public static final String APPROVED = "APPROVED";

  /** Approval: returned for correction. */
  public static final String RETURNED = "RETURNED";

  @Column(name = "client_status", length = 20)
  private String clientStatus;

  @Column(name = "client_status_remarks", length = 1000)
  private String clientRemarks;

  @Column(name = "client_status_at")
  private Instant clientAt;

  @Column(name = "client_status_by", length = 50)
  private String clientBy;

  @Column(name = "approval_status", length = 20)
  private String approvalStatus;

  @Column(name = "approval_by", length = 50)
  private String approvalBy;

  @Column(name = "approval_at")
  private Instant approvalAt;

  @Column(name = "approval_remarks", length = 1000)
  private String approvalRemarks;

  @Column(name = "payment_confirmation", length = 40)
  private String paymentConfirmation;

  /**
   * Records the client's answer.
   *
   * @param status status
   * @param remarks remarks
   * @param user user
   * @param at time
   */
  public void client(String status, String remarks, String user, Instant at) {
    this.clientStatus = status;
    this.clientRemarks = remarks;
    this.clientBy = user;
    this.clientAt = at;
  }

  /**
   * Records an approval step.
   *
   * @param status SUBMITTED, APPROVED or RETURNED
   * @param remarks remarks
   * @param user user
   * @param at time
   */
  public void approval(String status, String remarks, String user, Instant at) {
    this.approvalStatus = status;
    this.approvalRemarks = remarks;
    this.approvalBy = user;
    this.approvalAt = at;
  }

  /**
   * Records the client's payment confirmation.
   *
   * @param confirmation how the client confirmed
   */
  public void paymentConfirmation(String confirmation) {
    this.paymentConfirmation = confirmation;
  }

  public String getClientStatus() {
    return clientStatus;
  }

  public String getClientRemarks() {
    return clientRemarks;
  }

  public Instant getClientAt() {
    return clientAt;
  }

  public String getClientBy() {
    return clientBy;
  }

  public String getApprovalStatus() {
    return approvalStatus;
  }

  public String getApprovalBy() {
    return approvalBy;
  }

  public Instant getApprovalAt() {
    return approvalAt;
  }

  public String getApprovalRemarks() {
    return approvalRemarks;
  }

  public String getPaymentConfirmation() {
    return paymentConfirmation;
  }
}
