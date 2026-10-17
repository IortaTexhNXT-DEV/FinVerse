package com.iortatechnxt.brokerverse.booking.domain;

/** Premium components of a booked invoice (Operations invoice ledger, CSHID.022 hierarchy). */
public enum PremiumComponent {
  /** Basic (net) premium. */
  BASIC,
  /** Documentary stamp tax. */
  DST,
  /** Premium tax or VAT on premium. */
  PREMIUM_TAX_OR_VAT,
  /** Local government tax. */
  LGT,
  /** Fire service tax. */
  FST,
  /** Other charges. */
  OTHER
}
