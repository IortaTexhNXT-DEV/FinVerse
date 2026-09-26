package com.iortatechnxt.brokerverse.storage.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A request to place or release the legal hold of a stored file (DOCUMENT_STORAGE_DECISION,
 * decision 3): raised by a holder of {@code FILE_LEGAL_HOLD_REQUEST} with a reason, applied only
 * when another user holding {@code FILE_LEGAL_HOLD_APPROVE} (the DOA roles) approves it.
 */
@Entity
@Table(name = "sto_legal_hold_request")
public class LegalHoldRequest extends BaseEntity {

  @Column(name = "stored_file_id", nullable = false, updatable = false)
  private Long storedFileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private HoldAction action;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private HoldRequestStatus status;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", length = 500)
  private String decisionNote;

  protected LegalHoldRequest() {}

  /**
   * Raises a request.
   *
   * @param storedFileId file
   * @param action place or release
   * @param reason reason
   * @param requestedBy requester
   * @param requestedAt time
   */
  public LegalHoldRequest(
      Long storedFileId,
      HoldAction action,
      String reason,
      String requestedBy,
      Instant requestedAt) {
    this.storedFileId = storedFileId;
    this.action = action;
    this.reason = reason;
    this.status = HoldRequestStatus.PENDING;
    this.requestedBy = requestedBy;
    this.requestedAt = requestedAt;
  }

  /**
   * Decides the request (four eyes: the requester cannot decide).
   *
   * @param approve true to approve
   * @param user approver
   * @param note decision note
   * @param at time
   */
  public void decide(boolean approve, String user, String note, Instant at) {
    if (status != HoldRequestStatus.PENDING) {
      throw new BusinessRuleException("HOLD_REQUEST_DECIDED", "The request was already decided");
    }
    if (CurrentUser.sameUser(user, requestedBy)) {
      throw new BusinessRuleException(
          "HOLD_REQUEST_OWN", "A legal hold request must be decided by another user");
    }
    this.status = approve ? HoldRequestStatus.APPROVED : HoldRequestStatus.REJECTED;
    this.decidedBy = user;
    this.decidedAt = at;
    this.decisionNote = note;
  }

  public Long getStoredFileId() {
    return storedFileId;
  }

  public HoldAction getAction() {
    return action;
  }

  public String getReason() {
    return reason;
  }

  public HoldRequestStatus getStatus() {
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
