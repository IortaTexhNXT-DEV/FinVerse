package com.iortatechnxt.brokerverse.period.api.dto;

import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Accounting period view.
 *
 * @param id id
 * @param fiscalYearId fiscal year
 * @param periodNo number
 * @param name name
 * @param startDate start
 * @param endDate end
 * @param status status
 * @param statusChangedBy last status change user
 * @param statusChangedAt last status change time
 * @param statusReason reason
 */
public record PeriodResponse(
    Long id,
    Long fiscalYearId,
    int periodNo,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    PeriodStatus status,
    String statusChangedBy,
    Instant statusChangedAt,
    String statusReason) {

  /**
   * Maps an entity.
   *
   * @param p period
   * @return response
   */
  public static PeriodResponse from(AccountingPeriod p) {
    return new PeriodResponse(
        p.getId(),
        p.getFiscalYear().getId(),
        p.getPeriodNo(),
        p.getName(),
        p.getStartDate(),
        p.getEndDate(),
        p.getStatus(),
        p.getStatusChangedBy(),
        p.getStatusChangedAt(),
        p.getStatusReason());
  }
}
