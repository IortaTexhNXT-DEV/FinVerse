package com.iortatechnxt.brokerverse.reserves.domain;

/** Method used to estimate IBNR for a line of business. */
public enum IbnrMethod {
  /** Rate method (PGIBR079): IBNR = earned premium of the last twelve months × rate %. */
  RATE,
  /** Chain-ladder on the paid or incurred development triangle: IBNR = ultimate − incurred. */
  CHAIN_LADDER
}
