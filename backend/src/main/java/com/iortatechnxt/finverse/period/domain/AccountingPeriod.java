package com.iortatechnxt.finverse.period.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/** Monthly accounting period with a controlled status lifecycle (see {@link PeriodStatus}). */
@Entity
@Table(name = "per_period")
public class AccountingPeriod extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fiscal_year_id", nullable = false)
  private FiscalYear fiscalYear;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "period_no", nullable = false)
  private int periodNo;

  @Column(nullable = false, length = 20)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private PeriodStatus status = PeriodStatus.FUTURE;

  @Column(name = "status_changed_by", length = 50)
  private String statusChangedBy;

  @Column(name = "status_changed_at")
  private Instant statusChangedAt;

  @Column(name = "status_reason", length = 200)
  private String statusReason;

  protected AccountingPeriod() {}

  /**
   * Creates a period.
   *
   * @param fiscalYear fiscal year
   * @param periodNo 1..12
   * @param name display name, e.g. "2026-01"
   * @param startDate first day
   * @param endDate last day
   */
  public AccountingPeriod(
      FiscalYear fiscalYear, int periodNo, String name, LocalDate startDate, LocalDate endDate) {
    this.fiscalYear = fiscalYear;
    this.companyId = fiscalYear.getCompanyId();
    this.periodNo = periodNo;
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  /**
   * Moves the period to a new status, enforcing the allowed transitions.
   *
   * @param target new status
   * @param user acting user
   * @param when timestamp
   * @param reason reason (mandatory for reopen)
   */
  public void transition(PeriodStatus target, String user, Instant when, String reason) {
    if (!allowedTargets().contains(target)) {
      throw new BusinessRuleException(
          "INVALID_PERIOD_TRANSITION",
          "Period " + name + " cannot move from " + status + " to " + target);
    }
    this.status = target;
    this.statusChangedBy = user;
    this.statusChangedAt = when;
    this.statusReason = reason;
  }

  private Set<PeriodStatus> allowedTargets() {
    return switch (status) {
      case FUTURE -> EnumSet.of(PeriodStatus.OPEN);
      case OPEN -> EnumSet.of(PeriodStatus.CLOSING, PeriodStatus.CLOSED);
      case CLOSING -> EnumSet.of(PeriodStatus.OPEN, PeriodStatus.CLOSED);
      case CLOSED -> EnumSet.of(PeriodStatus.REOPENED);
      case REOPENED -> EnumSet.of(PeriodStatus.CLOSED);
    };
  }

  /**
   * Checks whether a journal may post into the period.
   *
   * @param systemOrAdjustment true for system generated or adjustment journals
   * @return true when posting is permitted
   */
  public boolean acceptsPosting(boolean systemOrAdjustment) {
    return switch (status) {
      case OPEN -> true;
      case CLOSING, REOPENED -> systemOrAdjustment;
      case FUTURE, CLOSED -> false;
    };
  }

  /**
   * Checks whether a date falls in the period.
   *
   * @param date date
   * @return true when within the period
   */
  public boolean contains(LocalDate date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }

  public FiscalYear getFiscalYear() {
    return fiscalYear;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getPeriodNo() {
    return periodNo;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public PeriodStatus getStatus() {
    return status;
  }

  public String getStatusChangedBy() {
    return statusChangedBy;
  }

  public Instant getStatusChangedAt() {
    return statusChangedAt;
  }

  public String getStatusReason() {
    return statusReason;
  }
}
