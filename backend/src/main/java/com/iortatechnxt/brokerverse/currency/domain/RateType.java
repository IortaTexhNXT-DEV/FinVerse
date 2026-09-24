package com.iortatechnxt.brokerverse.currency.domain;

/** Exchange rate types (Currency Rate Type Master). */
public enum RateType {
  /** Daily transaction (spot) rate used for postings. */
  SPOT,
  /** Month-end closing rate used for revaluation of monetary balances. */
  CLOSING,
  /** Period average rate used for translation of income statement items. */
  AVERAGE,
  /** Planning rate used for budgets. */
  BUDGET
}
