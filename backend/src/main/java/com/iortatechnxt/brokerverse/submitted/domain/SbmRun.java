package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A processing run (BRIDSP-09; design section 3.2): its trigger and scope and the counts of the
 * records passed, bucketed, fallen out, overridden by a manual tag and above an insurer limit.
 */
@Entity
@Table(name = "sbm_run")
public class SbmRun extends BaseEntity {

  private static final int SCOPE = 250;

  /** What started a run. */
  public enum Trigger {
    /** An intake run committed. */
    INTAKE,
    /** The daily job. */
    SCHEDULED,
    /** A user. */
    MANUAL
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, updatable = false, length = 30)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, updatable = false, length = 20)
  private Trigger trigger;

  @Column(nullable = false, updatable = false, length = 250)
  private String scope;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(nullable = false)
  private int total;

  @Column(nullable = false)
  private int passed;

  @Column(nullable = false)
  private int bucketed;

  @Column(nullable = false)
  private int fallout;

  @Column(nullable = false)
  private int overridden;

  @Column(nullable = false)
  private int breaches;

  protected SbmRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param runNo run number
   * @param trigger trigger
   * @param scope scope description
   * @param startedAt start
   */
  public SbmRun(Long companyId, String runNo, Trigger trigger, String scope, Instant startedAt) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.trigger = trigger;
    this.scope = scope.length() > SCOPE ? scope.substring(0, SCOPE) : scope;
    this.startedAt = startedAt;
  }

  /**
   * Counts the result of one record.
   *
   * @param kind PASSED, BUCKETED, FALLOUT or OVERRIDDEN
   * @param breached whether the record is above an insurer limit
   */
  public void count(SbmRunResult.Outcome kind, boolean breached) {
    total++;
    switch (kind) {
      case PASSED -> passed++;
      case BUCKETED -> bucketed++;
      case FALLOUT -> fallout++;
      default -> overridden++;
    }
    if (breached) {
      breaches++;
    }
  }

  /**
   * Finishes the run.
   *
   * @param at time
   */
  public void finish(Instant at) {
    this.finishedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public Trigger getTrigger() {
    return trigger;
  }

  public String getScope() {
    return scope;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public int getTotal() {
    return total;
  }

  public int getPassed() {
    return passed;
  }

  public int getBucketed() {
    return bucketed;
  }

  public int getFallout() {
    return fallout;
  }

  public int getOverridden() {
    return overridden;
  }

  public int getBreaches() {
    return breaches;
  }
}
