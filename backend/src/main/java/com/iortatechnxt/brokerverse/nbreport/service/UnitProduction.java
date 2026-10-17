package com.iortatechnxt.brokerverse.nbreport.service;

import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Production of one sales unit in a period against its target (BRNB.075).
 *
 * @param level unit level
 * @param code unit code (region, department or team code, or username)
 * @param name unit name (the code when unknown)
 * @param bookings accounts booked (booking invoices)
 * @param premium booked basic premium, net of endorsements and cancellations (PHP)
 * @param commission commission (PHP)
 * @param targetCount target bookings (pro rata to the period)
 * @param targetPremium target premium (pro rata to the period)
 * @param targetCommission target commission (pro rata to the period)
 */
public record UnitProduction(
    UnitLevel level,
    String code,
    String name,
    long bookings,
    BigDecimal premium,
    BigDecimal commission,
    long targetCount,
    BigDecimal targetPremium,
    BigDecimal targetCommission) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Premium achievement against the target in percent, null without a target.
   *
   * @return percent with one decimal
   */
  public BigDecimal achievement() {
    if (targetPremium == null || targetPremium.signum() == 0) {
      return null;
    }
    return premium.multiply(HUNDRED).divide(targetPremium, 1, RoundingMode.HALF_UP);
  }
}
