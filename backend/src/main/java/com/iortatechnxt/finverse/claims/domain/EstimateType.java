package com.iortatechnxt.finverse.claims.domain;

/**
 * Claim estimate / paid line types of the Reports Book. Reversal types carry negative amounts, so
 * "estimate payment = type 1 + 3" and "estimate recovery = type 2 + 4" are plain sums.
 */
public enum EstimateType {
  /** 1 Payment: payment estimate increase, or amount paid. */
  PAYMENT(1),
  /** 2 Recovery: recovery estimate increase, or amount recovered. */
  RECOVERY(2),
  /** 3 Reversal of Payment: payment estimate decrease (negative amount). */
  REVERSAL_OF_PAYMENT(3),
  /** 4 Reversal of Recovery: recovery estimate decrease (negative amount). */
  REVERSAL_OF_RECOVERY(4);

  private final int code;

  EstimateType(int code) {
    this.code = code;
  }

  /**
   * Reports Book code.
   *
   * @return 1 to 4
   */
  public int code() {
    return code;
  }

  /**
   * Type of a signed line on a side: positive amounts are types 1 / 2, negative ones reversals.
   *
   * @param side estimate side
   * @param negative whether the amount is negative
   * @return type
   */
  public static EstimateType of(EstimateSide side, boolean negative) {
    if (side == EstimateSide.PAYMENT) {
      return negative ? REVERSAL_OF_PAYMENT : PAYMENT;
    }
    return negative ? REVERSAL_OF_RECOVERY : RECOVERY;
  }
}
