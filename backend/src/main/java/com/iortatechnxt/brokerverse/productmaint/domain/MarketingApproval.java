package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One Marketing approval of a package request (BDOI FRS FRPM.011.02: "Each approver shall approve
 * or reject independently. Approval history shall be maintained."): the level (1 Team Leader, 2
 * Team Head, 3 Unit Head), the approver, the decision and the remarks.
 */
@Entity
@Table(name = "pm_marketing_approval")
public class MarketingApproval {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "level_no", nullable = false, updatable = false)
  private int levelNo;

  @Column(nullable = false, length = 50, updatable = false)
  private String approver;

  @Column(nullable = false, length = 20, updatable = false)
  private String decision;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "decided_at", nullable = false, updatable = false)
  private Instant decidedAt;

  /** For JPA. */
  protected MarketingApproval() {}

  /**
   * Records an approval.
   *
   * @param requestId request
   * @param levelNo level approved
   * @param approver approver
   * @param remarks remarks, may be null
   * @param decidedAt when
   */
  public MarketingApproval(
      Long requestId, int levelNo, String approver, String remarks, Instant decidedAt) {
    this.requestId = requestId;
    this.levelNo = levelNo;
    this.approver = approver;
    this.decision = "APPROVED";
    this.remarks = remarks;
    this.decidedAt = decidedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getRequestId() {
    return requestId;
  }

  public int getLevelNo() {
    return levelNo;
  }

  public String getApprover() {
    return approver;
  }

  public String getDecision() {
    return decision;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
