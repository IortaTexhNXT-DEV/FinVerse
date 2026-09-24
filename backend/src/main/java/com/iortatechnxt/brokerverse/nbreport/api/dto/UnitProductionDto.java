package com.iortatechnxt.brokerverse.nbreport.api.dto;

import com.iortatechnxt.brokerverse.nbreport.service.UnitProduction;
import java.math.BigDecimal;

/**
 * Production of a sales unit against its target (BRNB.075).
 *
 * @param level unit level
 * @param code unit code
 * @param name unit name
 * @param bookings bookings
 * @param premium booked premium (PHP)
 * @param commission commission (PHP)
 * @param targetCount target bookings
 * @param targetPremium target premium (PHP)
 * @param targetCommission target commission (PHP)
 * @param achievement premium achievement in percent, null without a target
 */
public record UnitProductionDto(
    String level,
    String code,
    String name,
    long bookings,
    BigDecimal premium,
    BigDecimal commission,
    long targetCount,
    BigDecimal targetPremium,
    BigDecimal targetCommission,
    BigDecimal achievement) {

  /**
   * Maps a unit's production.
   *
   * @param u production
   * @return DTO
   */
  public static UnitProductionDto from(UnitProduction u) {
    return new UnitProductionDto(
        u.level().name(),
        u.code(),
        u.name(),
        u.bookings(),
        u.premium(),
        u.commission(),
        u.targetCount(),
        u.targetPremium(),
        u.targetCommission(),
        u.achievement());
  }
}
