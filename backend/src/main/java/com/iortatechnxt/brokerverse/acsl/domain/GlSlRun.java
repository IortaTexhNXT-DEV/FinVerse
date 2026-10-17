package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One GL-SL reconciliation of a company as of a date (ACSL 2.13.2), with its totals. */
@Entity
@Table(name = "acsl_glsl_run")
public class GlSlRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "as_of", nullable = false, updatable = false)
  private LocalDate asOf;

  @Column(name = "run_at", nullable = false, updatable = false)
  private Instant runAt;

  @Column(name = "run_by", nullable = false, length = 50, updatable = false)
  private String runBy;

  @Column(nullable = false)
  private int accounts;

  @Column(nullable = false)
  private int differences;

  @Column(name = "total_difference", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDifference = BigDecimal.ZERO;

  protected GlSlRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param asOf balances as of
   * @param runAt when
   * @param runBy user or job
   */
  public GlSlRun(Long companyId, LocalDate asOf, Instant runAt, String runBy) {
    this.companyId = companyId;
    this.asOf = asOf;
    this.runAt = runAt;
    this.runBy = runBy;
  }

  /**
   * Records the totals.
   *
   * @param accountCount control accounts compared
   * @param differenceCount accounts with a difference
   * @param total sum of the absolute differences
   */
  public void totals(int accountCount, int differenceCount, BigDecimal total) {
    this.accounts = accountCount;
    this.differences = differenceCount;
    this.totalDifference = total;
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public LocalDate getAsOf() {
    return asOf;
  }

  public Instant getRunAt() {
    return runAt;
  }

  public String getRunBy() {
    return runBy;
  }

  public int getAccounts() {
    return accounts;
  }

  public int getDifferences() {
    return differences;
  }

  public BigDecimal getTotalDifference() {
    return totalDifference;
  }
}
