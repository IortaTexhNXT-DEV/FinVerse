package com.iortatechnxt.brokerverse.ledger.service;

import java.math.BigDecimal;

/**
 * Aggregated debit and credit totals of an account (optionally per currency).
 *
 * @param accountId account
 * @param currency currency, or null when aggregated across currencies
 * @param debitFc debit in transaction currency (meaningful only per currency)
 * @param creditFc credit in transaction currency
 * @param debitBase debit in base currency
 * @param creditBase credit in base currency
 */
public record AccountBalance(
    Long accountId,
    String currency,
    BigDecimal debitFc,
    BigDecimal creditFc,
    BigDecimal debitBase,
    BigDecimal creditBase) {

  /**
   * Net base balance, debit positive.
   *
   * @return debit minus credit in base currency
   */
  public BigDecimal netBase() {
    return debitBase.subtract(creditBase);
  }

  /**
   * Net transaction currency balance, debit positive.
   *
   * @return debit minus credit in transaction currency
   */
  public BigDecimal netFc() {
    return debitFc.subtract(creditFc);
  }
}
