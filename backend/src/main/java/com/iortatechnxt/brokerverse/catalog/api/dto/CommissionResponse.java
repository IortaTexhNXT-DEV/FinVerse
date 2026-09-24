package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A commission rate of an insurer.
 *
 * @param id id
 * @param insurerCode insurer
 * @param productCode product (null = all)
 * @param rate commission %
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record CommissionResponse(
    Long id,
    String insurerCode,
    String productCode,
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
  public static CommissionResponse from(CommissionRate e) {
    return new CommissionResponse(
        e.getId(),
        e.getInsurerCode(),
        e.getProductCode(),
        e.getRate(),
        e.getEffectiveFrom(),
        e.getEffectiveTo(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
