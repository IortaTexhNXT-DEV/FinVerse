package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One change of a tracked field of a claim with its old and new value, reason, user and time
 * (CLAIMS_BROKING_DESIGN 5.2; FR-CM-003: every version kept). Insert-only.
 */
@Entity
@Table(name = "bcl_claim_event")
public class ClaimEvent extends BaseEntity {

  private static final int MAX_VALUE = 2000;
  private static final int MAX_REASON = 500;

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private ClaimField field;

  @Column(name = "old_value", length = MAX_VALUE, updatable = false)
  private String oldValue;

  @Column(name = "new_value", length = MAX_VALUE, updatable = false)
  private String newValue;

  @Column(length = MAX_REASON, updatable = false)
  private String reason;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected ClaimEvent() {}

  /**
   * Records a change.
   *
   * @param claimId claim
   * @param field field
   * @param values previous and new value (null when none / cleared)
   * @param who user, time and reason
   */
  public ClaimEvent(Long claimId, ClaimField field, Values values, Author who) {
    this.claimId = claimId;
    this.field = field;
    this.oldValue = clip(values.oldValue(), MAX_VALUE);
    this.newValue = clip(values.newValue(), MAX_VALUE);
    this.reason = clip(who.reason(), MAX_REASON);
    this.changedBy = who.by();
    this.changedAt = who.at();
  }

  private static String clip(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  public Long getClaimId() {
    return claimId;
  }

  public ClaimField getField() {
    return field;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
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

  /**
   * The value before and after a change.
   *
   * @param oldValue previous value, null when none
   * @param newValue new value, null when cleared
   */
  public record Values(String oldValue, String newValue) {}

  /**
   * Who made a change, when and why.
   *
   * @param by user
   * @param at time
   * @param reason reason or remark, may be null
   */
  public record Author(String by, Instant at, String reason) {}
}
