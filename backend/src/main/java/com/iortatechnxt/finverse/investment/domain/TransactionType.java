package com.iortatechnxt.finverse.investment.domain;

/** Posted events in the life of a holding (its transaction history). */
public enum TransactionType {
  PURCHASE,
  TAKE_ON,
  ACCRUAL,
  AMORTIZATION,
  COUPON,
  FAIR_VALUE,
  MATURITY,
  SALE
}
