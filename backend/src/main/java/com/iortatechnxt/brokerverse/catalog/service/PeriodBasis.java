package com.iortatechnxt.brokerverse.catalog.service;

/** How the annual premium is adjusted to the period covered (Appendix A, all lines). */
public enum PeriodBasis {
  /** Full annual premium (one policy year). */
  ANNUAL,
  /** Days covered / 365 (366 when the policy year contains 29 February). */
  PRO_RATA,
  /** Percentage of the annual premium from the short-period table (months covered). */
  SHORT_PERIOD
}
