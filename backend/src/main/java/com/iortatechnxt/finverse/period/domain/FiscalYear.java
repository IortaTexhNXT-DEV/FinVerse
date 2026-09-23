package com.iortatechnxt.finverse.period.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Financial year of a company (fiscal or calendar, per company parameter). */
@Entity
@Table(name = "per_fiscal_year")
public class FiscalYear extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "year_code", nullable = false)
  private int yearCode;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private FiscalYearStatus status = FiscalYearStatus.OPEN;

  @Column(name = "closed_by", length = 50)
  private String closedBy;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected FiscalYear() {}

  /**
   * Creates a fiscal year.
   *
   * @param companyId company
   * @param yearCode year label (year in which the fiscal year starts)
   * @param startDate first day
   * @param endDate last day
   */
  public FiscalYear(Long companyId, int yearCode, LocalDate startDate, LocalDate endDate) {
    this.companyId = companyId;
    this.yearCode = yearCode;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  /**
   * Marks the year closed after year-end processing.
   *
   * @param user closing user
   * @param when timestamp
   */
  public void close(String user, Instant when) {
    this.status = FiscalYearStatus.CLOSED;
    this.closedBy = user;
    this.closedAt = when;
  }

  /**
   * Checks whether a date falls in the year.
   *
   * @param date date
   * @return true when within start and end (inclusive)
   */
  public boolean contains(LocalDate date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getYearCode() {
    return yearCode;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public FiscalYearStatus getStatus() {
    return status;
  }

  public String getClosedBy() {
    return closedBy;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
