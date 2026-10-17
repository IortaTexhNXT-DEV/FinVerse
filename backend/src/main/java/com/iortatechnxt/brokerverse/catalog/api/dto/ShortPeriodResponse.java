package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A short-period table row.
 *
 * @param id id
 * @param monthsCovered months covered
 * @param percentOfAnnual percent of annual premium
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record ShortPeriodResponse(
    Long id,
    int monthsCovered,
    BigDecimal percentOfAnnual,
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
  public static ShortPeriodResponse from(ShortPeriodRate e) {
    return new ShortPeriodResponse(
        e.getId(),
        e.getMonthsCovered(),
        e.getPercentOfAnnual(),
        e.getEffectiveFrom(),
        e.getEffectiveTo(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
