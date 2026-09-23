package com.iortatechnxt.finverse.tax.domain;

import java.math.BigDecimal;

/**
 * Natural side of a line item: the ledger net (debit − credit) is shown positive for a DEBIT item
 * (assets, expenses) and negated for a CREDIT item (liabilities, equity, income).
 */
public enum NormalBalance {
  DEBIT,
  CREDIT;

  /**
   * Presents a ledger net amount on this side.
   *
   * @param debitMinusCredit ledger net
   * @return amount, positive when the balance is on the natural side
   */
  public BigDecimal present(BigDecimal debitMinusCredit) {
    return this == DEBIT ? debitMinusCredit : debitMinusCredit.negate();
  }
}
