package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One amendment of an insurer reserve (BRCLM.023/024; CLAIMS_BROKING_DESIGN 5.2): previous and new
 * amount, reason, user and time. Insert-only; the reserve is information only and produces no
 * journal (FR-CM-032 R2).
 */
@Entity
@Table(name = "bcl_reserve_change")
public class InsurerReserveChange extends BaseEntity {

  @Column(name = "insurer_claim_id", nullable = false, updatable = false)
  private Long insurerClaimId;

  @Column(name = "previous_amount", precision = 19, scale = 2, updatable = false)
  private BigDecimal previousAmount;

  @Column(name = "new_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal newAmount;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected InsurerReserveChange() {}

  /**
   * Records an amendment.
   *
   * @param insurerClaimId insurer line
   * @param previousAmount previous reserve, may be null
   * @param newAmount new reserve
   * @param reason reason
   * @param changedBy user
   * @param changedAt time
   */
  public InsurerReserveChange(
      Long insurerClaimId,
      BigDecimal previousAmount,
      BigDecimal newAmount,
      String reason,
      String changedBy,
      Instant changedAt) {
    this.insurerClaimId = insurerClaimId;
    this.previousAmount = previousAmount;
    this.newAmount = newAmount;
    this.reason = reason;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }

  public Long getInsurerClaimId() {
    return insurerClaimId;
  }

  public BigDecimal getPreviousAmount() {
    return previousAmount;
  }

  public BigDecimal getNewAmount() {
    return newAmount;
  }

  public String getReason() {
    return reason;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
