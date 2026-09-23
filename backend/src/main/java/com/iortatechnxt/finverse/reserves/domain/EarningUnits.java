package com.iortatechnxt.finverse.reserves.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Earning units of one premium transaction at a valuation date: days (1/365), twenty-fourths (1/24)
 * or eighths (1/8) of the cover.
 *
 * @param total total units of the cover
 * @param earned units earned at the valuation date
 * @param unearned units still unearned ({@code total − earned})
 */
public record EarningUnits(int total, int earned, int unearned) {

  private static final int FRACTION_SCALE = 10;

  /**
   * Units with the unearned count derived.
   *
   * @param total total units
   * @param earned earned units (clamped to 0..total)
   * @return units
   */
  public static EarningUnits of(int total, int earned) {
    int clamped = Math.clamp(earned, 0, Math.max(total, 0));
    return new EarningUnits(Math.max(total, 0), clamped, Math.max(total, 0) - clamped);
  }

  /**
   * Unearned share of the cover.
   *
   * @return unearned / total, zero for a cover without units
   */
  public BigDecimal unearnedFraction() {
    if (total == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(unearned)
        .divide(BigDecimal.valueOf(total), FRACTION_SCALE, RoundingMode.HALF_EVEN);
  }
}
