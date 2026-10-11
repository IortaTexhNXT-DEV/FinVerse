package com.iortatechnxt.brokerverse.adjustment.domain;

/**
 * How the remaining term is priced (ADJID.009/014, OQ36): pro-rata days or the short-period table.
 */
public enum RefundBasis {
  /** Days remaining over the days of the policy year. */
  PRO_RATA,
  /** Short-period table percentage. */
  SHORT_PERIOD
}
