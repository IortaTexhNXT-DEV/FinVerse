package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ProductClass;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;

/**
 * A TSU routing rule.
 *
 * @param id id
 * @param code code
 * @param description description
 * @param productClass product class
 * @param lineCode product line
 * @param minFleetUnits minimum vehicles
 * @param minLocations minimum locations
 * @param tsiAbove TSI threshold
 * @param endorsementType endorsement type
 * @param priority evaluation order
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record TsuRuleResponse(
    Long id,
    String code,
    String description,
    ProductClass productClass,
    String lineCode,
    Integer minFleetUnits,
    Integer minLocations,
    BigDecimal tsiAbove,
    String endorsementType,
    int priority,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static TsuRuleResponse from(TsuRule e) {
    return new TsuRuleResponse(
        e.getId(),
        e.getCode(),
        e.getDescription(),
        e.getProductClass(),
        e.getLineCode(),
        e.getMinFleetUnits(),
        e.getMinLocations(),
        e.getTsiAbove(),
        e.getEndorsementType(),
        e.getPriority(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
