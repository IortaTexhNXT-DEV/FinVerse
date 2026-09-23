package com.iortatechnxt.finverse.period.api.dto;

import com.iortatechnxt.finverse.period.domain.FiscalYear;
import com.iortatechnxt.finverse.period.domain.FiscalYearStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Fiscal year view.
 *
 * @param id id
 * @param companyId company
 * @param yearCode year
 * @param startDate start
 * @param endDate end
 * @param status status
 * @param closedBy closing user
 * @param closedAt closing time
 */
public record FiscalYearResponse(
    Long id,
    Long companyId,
    int yearCode,
    LocalDate startDate,
    LocalDate endDate,
    FiscalYearStatus status,
    String closedBy,
    Instant closedAt) {

  /**
   * Maps an entity.
   *
   * @param y year
   * @return response
   */
  public static FiscalYearResponse from(FiscalYear y) {
    return new FiscalYearResponse(
        y.getId(),
        y.getCompanyId(),
        y.getYearCode(),
        y.getStartDate(),
        y.getEndDate(),
        y.getStatus(),
        y.getClosedBy(),
        y.getClosedAt());
  }
}
