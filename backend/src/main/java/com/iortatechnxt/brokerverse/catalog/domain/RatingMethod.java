package com.iortatechnxt.brokerverse.catalog.domain;

/** Premium formula of a product line (Appendix A). */
public enum RatingMethod {
  /** Fire: net = TSI x rate, then DST, premium tax, FST and LGT. */
  PROPERTY,
  /** Motor: OD/Theft coverage x rate plus excess BI and PD premiums, then DST, VAT, LGT. */
  MOTOR,
  /** Other lines: net = sum of item sums insured x rate, then the line's taxes. */
  GENERIC
}
