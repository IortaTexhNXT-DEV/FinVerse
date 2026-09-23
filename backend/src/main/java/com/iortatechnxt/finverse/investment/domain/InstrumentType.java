package com.iortatechnxt.finverse.investment.domain;

/** Investment instruments admitted by the Insurance Code investment rules (non-life). */
public enum InstrumentType {
  TIME_DEPOSIT,
  TREASURY_BILL,
  GOVERNMENT_BOND,
  CORPORATE_BOND,
  EQUITY;

  /**
   * Whether the instrument has a maturity date (and therefore interest and amortization).
   *
   * @return false for equities
   */
  public boolean isDebt() {
    return this != EQUITY;
  }
}
