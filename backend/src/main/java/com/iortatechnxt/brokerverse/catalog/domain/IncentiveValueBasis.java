package com.iortatechnxt.brokerverse.catalog.domain;

/** How the value of an incentive criterion is expressed (PMADD07; content PQ04). */
public enum IncentiveValueBasis {
  /** A percentage (0 to 100). */
  RATE,
  /** A fixed amount. */
  FIXED_AMOUNT,
  /** A rule described by the rule parameters; no single value. */
  RULE
}
