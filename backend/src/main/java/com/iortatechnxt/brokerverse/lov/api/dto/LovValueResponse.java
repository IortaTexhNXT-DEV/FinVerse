package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import java.time.LocalDate;

/**
 * A list value.
 *
 * @param id id
 * @param typeCode list
 * @param code code
 * @param label label
 * @param sortOrder order
 * @param parentCode parent value
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date
 * @param status maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record LovValueResponse(
    Long id,
    String typeCode,
    String code,
    String label,
    int sortOrder,
    String parentCode,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    RecordStatus status,
    String maker,
    String authorizedBy) {

  /**
   * Maps a value.
   *
   * @param v entity
   * @return response
   */
  public static LovValueResponse from(LovValue v) {
    return new LovValueResponse(
        v.getId(),
        v.getTypeCode(),
        v.getCode(),
        v.getLabel(),
        v.getSortOrder(),
        v.getParentCode(),
        v.getEffectiveFrom(),
        v.getEffectiveTo(),
        v.getRecordStatus(),
        v.getMaker(),
        v.getAuthorizedBy());
  }
}
