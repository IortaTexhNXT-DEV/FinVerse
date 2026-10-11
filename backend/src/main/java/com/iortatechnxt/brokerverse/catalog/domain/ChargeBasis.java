package com.iortatechnxt.brokerverse.catalog.domain;

/** How an other charge is computed (template PM-04 Charges). */
public enum ChargeBasis {
  /** A fixed amount per policy. */
  AMOUNT,
  /** A rate in percent of the net premium. */
  RATE
}
