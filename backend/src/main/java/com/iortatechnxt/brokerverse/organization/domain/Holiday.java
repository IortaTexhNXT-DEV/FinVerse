package com.iortatechnxt.brokerverse.organization.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Holiday calendar entry. A null {@code branchId} means a company-wide (global) holiday. */
@Entity
@Table(name = "org_holiday")
public class Holiday extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(name = "holiday_date", nullable = false)
  private LocalDate holidayDate;

  @Column(nullable = false, length = 120)
  private String description;

  protected Holiday() {}

  /**
   * Creates a holiday.
   *
   * @param companyId company
   * @param branchId branch, or null for all branches
   * @param holidayDate date
   * @param description description
   */
  public Holiday(Long companyId, Long branchId, LocalDate holidayDate, String description) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.holidayDate = holidayDate;
    this.description = description;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public LocalDate getHolidayDate() {
    return holidayDate;
  }

  public String getDescription() {
    return description;
  }
}
