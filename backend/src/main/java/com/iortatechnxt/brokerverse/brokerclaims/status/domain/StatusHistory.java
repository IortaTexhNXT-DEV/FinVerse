package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One status change of a claim (BRCLM.011/027/028; CLAIMS_BROKING_DESIGN 5.2): from and to status
 * and phase, user, time, remark and the days spent in the previous status, which the ageing per
 * status report reads. Insert-only; a closure by settlement type and a reopen keep the status and
 * change the phase.
 */
@Entity
@Table(name = "bcl_status_history")
public class StatusHistory extends BaseEntity {

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "from_status", length = 40, updatable = false)
  private String fromStatus;

  @Column(name = "to_status", nullable = false, length = 40, updatable = false)
  private String toStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_phase", length = 20, updatable = false)
  private ClaimPhase fromPhase;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_phase", nullable = false, length = 20, updatable = false)
  private ClaimPhase toPhase;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(length = 1000, updatable = false)
  private String remark;

  @Column(name = "days_in_previous", updatable = false)
  private Integer daysInPrevious;

  protected StatusHistory() {}

  /**
   * Records a change.
   *
   * @param claimId claim
   * @param from previous status and phase (both null for the first status)
   * @param to new status and phase
   * @param who user, time, remark and days in the previous status
   */
  public StatusHistory(Long claimId, Step from, Step to, Change who) {
    this.claimId = claimId;
    this.fromStatus = from.status();
    this.fromPhase = from.phase();
    this.toStatus = to.status();
    this.toPhase = to.phase();
    this.changedAt = who.at();
    this.changedBy = who.by();
    this.remark = who.remark();
    this.daysInPrevious = who.daysInPrevious();
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public ClaimPhase getFromPhase() {
    return fromPhase;
  }

  public ClaimPhase getToPhase() {
    return toPhase;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public String getRemark() {
    return remark;
  }

  public Integer getDaysInPrevious() {
    return daysInPrevious;
  }

  /**
   * A status and its phase.
   *
   * @param status status code, null before the first status
   * @param phase phase, null before the first status
   */
  public record Step(String status, ClaimPhase phase) {}

  /**
   * Who changed the status, when and why.
   *
   * @param by user (SYSTEM for jobs)
   * @param at time
   * @param remark remark or reason
   * @param daysInPrevious calendar days spent in the previous status, null for the first status
   */
  public record Change(String by, Instant at, String remark, Integer daysInPrevious) {}
}
