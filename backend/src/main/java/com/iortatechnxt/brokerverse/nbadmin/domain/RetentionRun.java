package com.iortatechnxt.brokerverse.nbadmin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Result of one retention review of one rule: how many records were eligible (BRNB.106). */
@Entity
@Table(name = "nba_retention_run")
public class RetentionRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  @Column(name = "cutoff_date", nullable = false, updatable = false)
  private LocalDate cutoffDate;

  @Column(name = "eligible_count", updatable = false)
  private Long eligibleCount;

  @Column(name = "provider_available", nullable = false, updatable = false)
  private boolean providerAvailable;

  @Column(name = "run_at", nullable = false, updatable = false)
  private Instant runAt;

  @Column(name = "run_by", nullable = false, length = 50, updatable = false)
  private String runBy;

  protected RetentionRun() {}

  /**
   * Records a review result.
   *
   * @param ruleId rule
   * @param businessDate business date
   * @param cutoffDate last activity date counted
   * @param eligibleCount eligible records, null when no module provides the record type yet
   * @param runBy user or SYSTEM
   * @param runAt time
   */
  public RetentionRun(
      Long ruleId,
      LocalDate businessDate,
      LocalDate cutoffDate,
      Long eligibleCount,
      String runBy,
      Instant runAt) {
    this.ruleId = ruleId;
    this.businessDate = businessDate;
    this.cutoffDate = cutoffDate;
    this.eligibleCount = eligibleCount;
    this.providerAvailable = eligibleCount != null;
    this.runBy = runBy;
    this.runAt = runAt;
  }

  public Long getId() {
    return id;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public LocalDate getBusinessDate() {
    return businessDate;
  }

  public LocalDate getCutoffDate() {
    return cutoffDate;
  }

  public Long getEligibleCount() {
    return eligibleCount;
  }

  public boolean isProviderAvailable() {
    return providerAvailable;
  }

  public Instant getRunAt() {
    return runAt;
  }

  public String getRunBy() {
    return runBy;
  }
}
