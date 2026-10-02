package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * An administrator's request to reset a user's second factor (V1181): applied only when a holder of
 * MFA_RESET_APPROVE other than the requester approves it (four eyes); the requester may withdraw
 * it.
 */
@Entity
@Table(name = "sec_mfa_reset_request")
public class MfaResetRequest {

  /** Waiting for the approval. */
  public static final String PENDING = "PENDING";

  /** Approved and applied. */
  public static final String APPROVED = "APPROVED";

  /** Rejected by an approver. */
  public static final String REJECTED = "REJECTED";

  /** Withdrawn by the requester. */
  public static final String WITHDRAWN = "WITHDRAWN";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(nullable = false, updatable = false, length = 500)
  private String reason;

  @Column(nullable = false, length = 10)
  private String status = PENDING;

  @Column(name = "requested_by", nullable = false, updatable = false, length = 50)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 500)
  private String decisionNote;

  protected MfaResetRequest() {}

  /**
   * A new request.
   *
   * @param username user whose second factor is reset
   * @param reason why
   * @param requestedBy requester
   * @param requestedAt time
   */
  public MfaResetRequest(String username, String reason, String requestedBy, Instant requestedAt) {
    this.username = username;
    this.reason = reason;
    this.requestedBy = requestedBy;
    this.requestedAt = requestedAt;
  }

  /**
   * Records the decision.
   *
   * @param outcome APPROVED, REJECTED or WITHDRAWN
   * @param by who decided
   * @param when time
   * @param note reason of a rejection, may be null
   */
  public void decide(String outcome, String by, Instant when, String note) {
    this.status = outcome;
    this.decidedBy = by;
    this.decidedAt = when;
    this.decisionNote = note;
  }

  public boolean isPending() {
    return PENDING.equals(status);
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getReason() {
    return reason;
  }

  public String getStatus() {
    return status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionNote() {
    return decisionNote;
  }
}
