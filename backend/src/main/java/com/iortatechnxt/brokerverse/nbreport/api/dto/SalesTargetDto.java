package com.iortatechnxt.brokerverse.nbreport.api.dto;

import com.iortatechnxt.brokerverse.nbreport.domain.SalesTarget;
import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A production target (BRNB.075), request and response.
 *
 * @param id id (ignored on input)
 * @param unitLevel level
 * @param unitCode region, department or team code, or the officer's username
 * @param periodFrom period start
 * @param periodTo period end
 * @param targetCount target bookings
 * @param targetPremium target premium (PHP)
 * @param targetCommission target commission (PHP)
 * @param currency currency (PHP)
 */
public record SalesTargetDto(
    Long id,
    @NotNull UnitLevel unitLevel,
    @NotBlank @Size(max = 50) String unitCode,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo,
    @PositiveOrZero int targetCount,
    @NotNull @PositiveOrZero BigDecimal targetPremium,
    @NotNull @PositiveOrZero BigDecimal targetCommission,
    String currency) {

  /**
   * Maps a target.
   *
   * @param t target
   * @return DTO
   */
  public static SalesTargetDto from(SalesTarget t) {
    return new SalesTargetDto(
        t.getId(),
        t.getUnitLevel(),
        t.getUnitCode(),
        t.getPeriodFrom(),
        t.getPeriodTo(),
        t.getTargetCount(),
        t.getTargetPremium(),
        t.getTargetCommission(),
        t.getCurrency());
  }

  /**
   * The unit and period.
   *
   * @return unit
   */
  public SalesTarget.Unit unit() {
    return new SalesTarget.Unit(unitLevel, unitCode, periodFrom, periodTo);
  }

  /**
   * The values.
   *
   * @return values
   */
  public SalesTarget.Values values() {
    return new SalesTarget.Values(targetCount, targetPremium, targetCommission);
  }
}
