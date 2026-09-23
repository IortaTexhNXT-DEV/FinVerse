package com.iortatechnxt.finverse.investment.domain;

/**
 * How the premium or discount (face value minus clean purchase price) is spread to maturity.
 *
 * <ul>
 *   <li>{@link #EFFECTIVE_INTEREST}: the effective annual rate y (Actual/365 compounding) is solved
 *       at purchase so that the carrying amount reaches face value at maturity; each period's
 *       interest income is carrying amount x ((1 + y)^(days / 365) - 1) and the amortization is
 *       that income less the coupon accrued for the period (PFRS 9 effective interest method).
 *   <li>{@link #STRAIGHT_LINE}: the remaining premium or discount is spread evenly per day to
 *       maturity: (face value - carrying amount) x days in period / days to maturity.
 *   <li>{@link #NONE}: no amortization (equities, instruments bought at par).
 * </ul>
 *
 * <p>With both methods the period ending on the maturity date amortizes exactly the remaining
 * difference, absorbing rounding, so the carrying amount equals face value at maturity.
 */
public enum AmortizationMethod {
  EFFECTIVE_INTEREST,
  STRAIGHT_LINE,
  NONE
}
