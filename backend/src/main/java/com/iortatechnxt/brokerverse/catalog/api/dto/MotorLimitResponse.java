package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A motor BI / PD limit row.
 *
 * @param id id
 * @param coverage BI or PD
 * @param limitAmount limit
 * @param premium premium
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record MotorLimitResponse(
    Long id,
    MotorCoverage coverage,
    BigDecimal limitAmount,
    BigDecimal premium,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static MotorLimitResponse from(MotorLimit e) {
    return new MotorLimitResponse(
        e.getId(),
        e.getCoverage(),
        e.getLimitAmount(),
        e.getPremium(),
        e.getEffectiveFrom(),
        e.getEffectiveTo(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
