package com.iortatechnxt.brokerverse.migration.cutover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A run-off cohort of a monthly snapshot (DATA_MIGRATION_DESIGN section 15.2; FR-DM-122): the
 * migrated headers in force at go-live expiring in a month, per source system, with how many were
 * renewed in BIBS, not renewed, lapsed or are still open.
 */
@Entity
@Table(name = "mig_runoff_cohort")
public class RunoffCohort {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "snapshot_date", nullable = false, updatable = false)
  private LocalDate snapshotDate;

  @Column(name = "expiry_month", nullable = false, updatable = false)
  private LocalDate expiryMonth;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "headers_in_force", nullable = false)
  private int headersInForce;

  @Column(name = "premium_in_force", nullable = false, precision = 19, scale = 2)
  private BigDecimal premiumInForce;

  @Column(nullable = false)
  private int renewed;

  @Column(name = "not_renewed", nullable = false)
  private int notRenewed;

  @Column(nullable = false)
  private int lapsed;

  @Column(name = "still_open", nullable = false)
  private int stillOpen;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected RunoffCohort() {}

  /**
   * A cohort of a snapshot.
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   * @param key expiry month and source system
   * @param counts outcomes
   * @param when time
   */
  public RunoffCohort(
      Long companyId, LocalDate snapshotDate, Key key, Outcomes counts, Instant when) {
    this.companyId = companyId;
    this.snapshotDate = snapshotDate;
    this.expiryMonth = key.expiryMonth();
    this.sourceSystem = key.sourceSystem();
    this.headersInForce = counts.headers();
    this.premiumInForce = counts.premium();
    this.renewed = counts.renewed();
    this.notRenewed = counts.notRenewed();
    this.lapsed = counts.lapsed();
    this.stillOpen = counts.stillOpen();
    this.createdAt = when;
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  public LocalDate getExpiryMonth() {
    return expiryMonth;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public int getHeadersInForce() {
    return headersInForce;
  }

  public BigDecimal getPremiumInForce() {
    return premiumInForce;
  }

  public int getRenewed() {
    return renewed;
  }

  public int getNotRenewed() {
    return notRenewed;
  }

  public int getLapsed() {
    return lapsed;
  }

  public int getStillOpen() {
    return stillOpen;
  }

  /**
   * Key of a cohort.
   *
   * @param expiryMonth first day of the expiry month
   * @param sourceSystem source system
   */
  public record Key(LocalDate expiryMonth, String sourceSystem) {}

  /**
   * Outcomes of a cohort.
   *
   * @param headers headers in force at go-live
   * @param premium premium in force
   * @param renewed renewed in BIBS
   * @param notRenewed not renewed
   * @param lapsed lapsed without decision
   * @param stillOpen not yet expired or decided
   */
  public record Outcomes(
      int headers, BigDecimal premium, int renewed, int notRenewed, int lapsed, int stillOpen) {}
}
