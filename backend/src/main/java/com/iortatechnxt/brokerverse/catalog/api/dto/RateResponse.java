package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CatalogRate;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A tax or rating factor row.
 *
 * @param id id
 * @param rateCode tax or factor
 * @param lineCode product line (null = all)
 * @param rate rate %
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record RateResponse(
    Long id,
    RateCode rateCode,
    String lineCode,
    BigDecimal rate,
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
  public static RateResponse from(CatalogRate e) {
    return new RateResponse(
        e.getId(),
        e.getRateCode(),
        e.getLineCode(),
        e.getRate(),
        e.getEffectiveFrom(),
        e.getEffectiveTo(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
