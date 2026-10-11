package com.iortatechnxt.brokerverse.closing.domain;

import java.math.BigDecimal;

/**
 * Revaluation of one foreign currency balance (account × branch × currency). Balances are net debit
 * (debit positive).
 *
 * @param branchId branch
 * @param accountId account
 * @param accountCode account code
 * @param accountName account name
 * @param currency foreign currency
 * @param fcBalance balance in foreign currency
 * @param bookedBase base currency balance as booked
 * @param closingRate closing rate on the revaluation date
 * @param revaluedBase foreign currency balance at the closing rate
 */
public record RevaluationItem(
    Long branchId,
    Long accountId,
    String accountCode,
    String accountName,
    String currency,
    BigDecimal fcBalance,
    BigDecimal bookedBase,
    BigDecimal closingRate,
    BigDecimal revaluedBase) {

  /**
   * Unrealized exchange difference.
   *
   * @return revalued minus booked base (positive = gain on an asset / loss avoided on liability)
   */
  public BigDecimal difference() {
    return revaluedBase.subtract(bookedBase);
  }

  /**
   * Whether the balance can be revalued by journal: the foreign currency balance is not zero and
   * the booked base balance has the same sign (a positive booked rate exists).
   *
   * @return true when postable
   */
  public boolean postable() {
    return fcBalance.signum() != 0 && bookedBase.signum() == fcBalance.signum();
  }
}
