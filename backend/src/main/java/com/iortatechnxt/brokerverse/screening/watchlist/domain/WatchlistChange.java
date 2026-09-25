package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * A change of a watchlist entry with its before and after values (SNSRP-203, 204). A manual or
 * uploaded change waits in PENDING for a checker who is not its maker; a change of the scheduled
 * official feed is recorded as APPROVED by the system. A decided change is read only (database
 * trigger).
 */
@Entity
@Table(name = "scr_watchlist_change")
public class WatchlistChange extends BaseEntity {

  /** Maker of the changes applied by a scheduled run of the official feed. */
  public static final String FEED = "SYSTEM";

  @Column(name = "entry_id", nullable = false, updatable = false)
  private Long entryId;

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 20, updatable = false)
  private ChangeType changeType;

  @Column(name = "before_values", columnDefinition = "text", updatable = false)
  private String beforeValues;

  @Column(name = "after_values", columnDefinition = "text", nullable = false, updatable = false)
  private String afterValues;

  @Column(name = "maker_remarks", nullable = false, length = 2000, updatable = false)
  private String makerRemarks;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ChangeStatus status = ChangeStatus.PENDING;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 2000)
  private String decisionRemarks;

  /** For JPA. */
  protected WatchlistChange() {}

  /**
   * Records a pending change.
   *
   * @param entryId entry
   * @param runId ingestion run that produced it, {@code null} for a manual change
   * @param changeType kind
   * @param beforeValues values before (JSON), {@code null} for an addition
   * @param afterValues values after (JSON)
   * @param makerRemarks remarks (mandatory)
   */
  public WatchlistChange(
      Long entryId,
      Long runId,
      ChangeType changeType,
      String beforeValues,
      String afterValues,
      String makerRemarks) {
    this.entryId = entryId;
    this.runId = runId;
    this.changeType = changeType;
    this.beforeValues = beforeValues;
    this.afterValues = afterValues;
    this.makerRemarks = makerRemarks;
  }

  /**
   * Refuses a decision unless the change is PENDING and the checker is not its maker.
   *
   * @param checker deciding user
   */
  public void requireDecidableBy(String checker) {
    if (status != ChangeStatus.PENDING) {
      throw new BusinessRuleException(
          "SCR_CHANGE_DECIDED", "The change was already " + status.name().toLowerCase(Locale.ROOT));
    }
    if (checker != null && checker.equalsIgnoreCase(getCreatedBy())) {
      throw new BusinessRuleException(
          "SCR_LIST_MAKER_APPROVES", "A list change is approved by someone other than its maker");
    }
  }

  /**
   * Approves the change (the caller applies it to the entry).
   *
   * @param checker checker (never the maker)
   * @param when time
   * @param remarks optional remarks
   */
  public void approve(String checker, Instant when, String remarks) {
    requireDecidableBy(checker);
    decide(ChangeStatus.APPROVED, checker, when, remarks);
  }

  /**
   * Records an official feed change as applied by the system at once.
   *
   * @param when time
   * @param remarks run reference
   */
  public void appliedByFeed(Instant when, String remarks) {
    decide(ChangeStatus.APPROVED, FEED, when, remarks);
  }

  /**
   * Rejects the change; the entry stays as it was.
   *
   * @param checker checker (never the maker)
   * @param when time
   * @param remarks mandatory remarks
   */
  public void reject(String checker, Instant when, String remarks) {
    requireDecidableBy(checker);
    decide(ChangeStatus.REJECTED, checker, when, remarks);
  }

  private void decide(ChangeStatus outcome, String user, Instant when, String remarks) {
    this.status = outcome;
    this.decidedBy = user;
    this.decidedAt = when;
    this.decisionRemarks = remarks;
  }

  public Long getEntryId() {
    return entryId;
  }

  public Long getRunId() {
    return runId;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getBeforeValues() {
    return beforeValues;
  }

  public String getAfterValues() {
    return afterValues;
  }

  public String getMakerRemarks() {
    return makerRemarks;
  }

  public ChangeStatus getStatus() {
    return status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }
}
