package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A sign-off or threshold decision on a comparative: signatory, role, decision and remarks. */
@Entity
@Table(name = "eb_comparative_signoff")
public class EbComparativeSignoff extends BaseEntity {

  /** Sign-off by the authorised signatory. */
  public static final String SIGNOFF = "SIGNOFF";

  /** Threshold approval by Management. */
  public static final String THRESHOLD = "THRESHOLD";

  /** Approved. */
  public static final String APPROVED = "APPROVED";

  /** Returned to the AO. */
  public static final String RETURNED = "RETURNED";

  @Column(name = "comparative_id", nullable = false, updatable = false)
  private Long comparativeId;

  @Column(nullable = false, length = 20, updatable = false)
  private String role;

  @Column(nullable = false, length = 50, updatable = false)
  private String signatory;

  @Column(nullable = false, length = 20, updatable = false)
  private String decision;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "decided_at", nullable = false, updatable = false)
  private Instant decidedAt;

  protected EbComparativeSignoff() {}

  /**
   * Records a decision.
   *
   * @param comparativeId comparative
   * @param role SIGNOFF or THRESHOLD
   * @param signatory user
   * @param decision APPROVED or RETURNED
   * @param remarks remarks, may be null
   * @param decidedAt time
   */
  public EbComparativeSignoff(
      Long comparativeId,
      String role,
      String signatory,
      String decision,
      String remarks,
      Instant decidedAt) {
    this.comparativeId = comparativeId;
    this.role = role;
    this.signatory = signatory;
    this.decision = decision;
    this.remarks = remarks;
    this.decidedAt = decidedAt;
  }

  public Long getComparativeId() {
    return comparativeId;
  }

  public String getRole() {
    return role;
  }

  public String getSignatory() {
    return signatory;
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
