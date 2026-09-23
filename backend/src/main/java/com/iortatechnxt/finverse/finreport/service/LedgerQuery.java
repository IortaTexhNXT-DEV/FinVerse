package com.iortatechnxt.finverse.finreport.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Selection of posted ledger data (balances or entries).
 *
 * @param companyId company
 * @param branchId branch or null for all
 * @param costCenter cost centre or null for all
 * @param from first date of the period (opening balance = everything before it)
 * @param to last date of the period
 * @param accountIds accounts to read
 * @param partyFrom lowest party code or null
 * @param partyTo highest party code or null
 */
public record LedgerQuery(
    Long companyId,
    Long branchId,
    String costCenter,
    LocalDate from,
    LocalDate to,
    Collection<Long> accountIds,
    String partyFrom,
    String partyTo) {

  /** Canonical constructor copying the account list. */
  public LedgerQuery {
    accountIds = List.copyOf(accountIds);
  }

  /**
   * Query without cost centre and party filters.
   *
   * @param companyId company
   * @param branchId branch or null
   * @param from period start
   * @param to period end
   * @param accountIds accounts
   * @return query
   */
  public static LedgerQuery of(
      Long companyId, Long branchId, LocalDate from, LocalDate to, Collection<Long> accountIds) {
    return new LedgerQuery(companyId, branchId, null, from, to, accountIds, null, null);
  }

  /**
   * Copy with another period.
   *
   * @param newFrom period start
   * @param newTo period end
   * @return query
   */
  public LedgerQuery withPeriod(LocalDate newFrom, LocalDate newTo) {
    return new LedgerQuery(
        companyId, branchId, costCenter, newFrom, newTo, accountIds, partyFrom, partyTo);
  }
}
