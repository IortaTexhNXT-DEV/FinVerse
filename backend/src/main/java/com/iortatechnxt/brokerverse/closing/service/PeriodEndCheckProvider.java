package com.iortatechnxt.brokerverse.closing.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for modules that add their own controls to the monthly period-end checklist (for example
 * actuarial reserves: "valuation run posted for the month").
 *
 * <p>Implement it as a Spring bean in the owning module; the checklist appends the items of every
 * provider after the standard controls. The owning module depends on closing, never the reverse.
 */
public interface PeriodEndCheckProvider {

  /**
   * Controls of a period.
   *
   * @param companyId company
   * @param periodStart first day of the period
   * @param periodEnd last day of the period
   * @return checklist items (blocking or informational)
   */
  List<CheckItem> periodEndChecks(Long companyId, LocalDate periodStart, LocalDate periodEnd);
}
