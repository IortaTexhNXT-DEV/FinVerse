package com.iortatechnxt.finverse.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Monetary arithmetic helpers. All amounts are {@link BigDecimal}; never use double for money. */
public final class Money {

  /** Scale used for stored monetary amounts. */
  public static final int SCALE = 2;

  /** Scale used for exchange rates. */
  public static final int RATE_SCALE = 8;

  /** Rounding applied to all monetary amounts (banker's rounding avoids bias). */
  public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

  private Money() {}

  /**
   * Rounds an amount to the monetary scale.
   *
   * @param amount amount, may be null
   * @return rounded amount, zero when null
   */
  public static BigDecimal round(BigDecimal amount) {
    return amount == null ? zero() : amount.setScale(SCALE, ROUNDING);
  }

  /**
   * Converts a foreign currency amount to base currency.
   *
   * @param amount foreign currency amount
   * @param rate exchange rate (base units per one foreign unit)
   * @return base currency amount
   */
  public static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
    return round(amount.multiply(rate));
  }

  /**
   * Returns zero at monetary scale.
   *
   * @return 0.00
   */
  public static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
  }

  /**
   * Null-safe value.
   *
   * @param amount amount, may be null
   * @return amount or zero
   */
  public static BigDecimal nz(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  /**
   * Checks whether an amount is strictly positive.
   *
   * @param amount amount
   * @return true when greater than zero
   */
  public static boolean isPositive(BigDecimal amount) {
    return amount != null && amount.signum() > 0;
  }
}
