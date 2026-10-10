package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The approval task of one selected ManCom approver of a package request (BDOI FRS FRPM.014.01):
 * PENDING while it waits for the approver, WAITING for the President until every other approver has
 * approved, then APPROVED, RETURNED or REJECTED with the remarks; CLOSED when the round ended
 * without the approver's decision.
 */
@Entity
@Table(name = "pm_mancom_approval")
public class ManComApproval extends BaseEntity {

  /** Waits for the approver. */
  public static final String PENDING = "PENDING";

  /** The President waits for the other approvers. */
  public static final String WAITING = "WAITING";

  /** Approved. */
  public static final String APPROVED = "APPROVED";

  /** Ended without a decision of this approver. */
  public static final String CLOSED = "CLOSED";

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Column(nullable = false, length = 50, updatable = false)
  private String approver;

  @Column(nullable = false, updatable = false)
  private boolean president;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "decided_at")
  private Instant decidedAt;

  /** For JPA. */
  protected ManComApproval() {}

  /**
   * Creates the task of an approver.
   *
   * @param requestId request
   * @param roundNo approval round
   * @param approver approver
   * @param president whether the approver is the President (approves last)
   * @param waiting whether the task waits for the other approvers
   */
  public ManComApproval(
      Long requestId, int roundNo, String approver, boolean president, boolean waiting) {
    this.requestId = requestId;
    this.roundNo = roundNo;
    this.approver = approver;
    this.president = president;
    this.status = waiting ? WAITING : PENDING;
  }

  /**
   * Records the decision.
   *
   * @param decision APPROVED, RETURNED or REJECTED
   * @param text remarks
   * @param when decision time
   */
  public void decide(String decision, String text, Instant when) {
    this.status = decision;
    this.remarks = text == null || text.isBlank() ? null : text.strip();
    this.decidedAt = when;
  }

  /** The President's turn has come. */
  public void open() {
    this.status = PENDING;
  }

  /** The round ended without this approver's decision. */
  public void close() {
    this.status = CLOSED;
  }

  public Long getRequestId() {
    return requestId;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public String getApprover() {
    return approver;
  }

  public boolean isPresident() {
    return president;
  }

  public String getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
