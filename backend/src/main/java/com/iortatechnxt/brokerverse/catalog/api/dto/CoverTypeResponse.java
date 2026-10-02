package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;

/**
 * A cover type.
 *
 * @param id id
 * @param lineCode product line
 * @param code code
 * @param name name
 * @param sortOrder display order
 * @param parentCode parent cover type of a subtype, null for a top-level type
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record CoverTypeResponse(
    Long id,
    String lineCode,
    String code,
    String name,
    int sortOrder,
    String parentCode,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static CoverTypeResponse from(CoverType e) {
    return new CoverTypeResponse(
        e.getId(),
        e.getLineCode(),
        e.getCode(),
        e.getName(),
        e.getSortOrder(),
        e.getParentCode(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
