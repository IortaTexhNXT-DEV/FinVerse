package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;

/**
 * A product line.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param riskItemKind kind of risk items
 * @param ratingMethod Appendix A formula
 * @param sortOrder display order
 * @param codePattern risk-code pattern, null for none
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record LineResponse(
    Long id,
    String code,
    String name,
    RiskItemKind riskItemKind,
    RatingMethod ratingMethod,
    int sortOrder,
    String codePattern,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static LineResponse from(ProductLine e) {
    return new LineResponse(
        e.getId(),
        e.getCode(),
        e.getName(),
        e.getRiskItemKind(),
        e.getRatingMethod(),
        e.getSortOrder(),
        e.getCodePattern(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
