package com.iortatechnxt.finverse.coa.domain;

/** Debit or credit side of an entry or balance. */
public enum BalanceSide {
  DEBIT,
  CREDIT;

  /**
   * Returns the opposite side.
   *
   * @return opposite side
   */
  public BalanceSide opposite() {
    return this == DEBIT ? CREDIT : DEBIT;
  }
}
