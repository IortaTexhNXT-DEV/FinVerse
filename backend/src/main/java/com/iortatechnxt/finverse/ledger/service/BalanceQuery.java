package com.iortatechnxt.finverse.ledger.service;

import java.time.LocalDate;

/**
 * Parameters of a balance aggregation.
 *
 * @param companyId company (mandatory)
 * @param branchId branch, or null for all branches (consolidated company view)
 * @param fromDate inclusive start, or null for inception
 * @param toDate inclusive end (mandatory)
 * @param byCurrency true to split balances per currency
 */
public record BalanceQuery(
    Long companyId, Long branchId, LocalDate fromDate, LocalDate toDate, boolean byCurrency) {

  /**
   * Cumulative balances as of a date, all branches, base currency.
   *
   * @param companyId company
   * @param asOf date
   * @return query
   */
  public static BalanceQuery asOf(Long companyId, LocalDate asOf) {
    return new BalanceQuery(companyId, null, null, asOf, false);
  }
}
