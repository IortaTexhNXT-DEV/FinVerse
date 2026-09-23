package com.iortatechnxt.finverse.investment.domain;

/**
 * Coupon payment frequency. Coupon dates are counted back from the maturity date in steps of {@link
 * #months()}; {@link #AT_MATURITY} pays all interest at maturity (time deposits) and {@link #NONE}
 * pays none (discounted treasury bills, equities).
 */
public enum CouponFrequency {
  NONE(0),
  MONTHLY(1),
  QUARTERLY(3),
  SEMI_ANNUAL(6),
  ANNUAL(12),
  AT_MATURITY(0);

  private final int months;

  CouponFrequency(int months) {
    this.months = months;
  }

  /**
   * Months between coupon dates.
   *
   * @return months, 0 when there are no periodic coupons
   */
  public int months() {
    return months;
  }
}
