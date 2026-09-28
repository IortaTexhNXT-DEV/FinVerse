package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.SalesLevel;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.time.Instant;

/**
 * A sales organisation unit.
 *
 * @param id id
 * @param level level
 * @param code code
 * @param name name
 * @param parentCode parent unit
 * @param costCenter default cost center
 * @param headUsername Unit Head (BRCLXN.011, CQ05)
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 * @param statusReason reason of the last deactivation or reactivation
 * @param createdBy creator
 * @param createdAt creation time
 * @param lastChangedAt time of the last change
 * @param authorizedAt authorization time
 */
public record SalesUnitResponse(
    Long id,
    SalesLevel level,
    String code,
    String name,
    String parentCode,
    String costCenter,
    String headUsername,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy,
    String statusReason,
    String createdBy,
    Instant createdAt,
    Instant lastChangedAt,
    Instant authorizedAt) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static SalesUnitResponse from(SalesUnit e) {
    return new SalesUnitResponse(
        e.getId(),
        e.getLevel(),
        e.getCode(),
        e.getName(),
        e.getParentCode(),
        e.getCostCenter(),
        e.getHeadUsername(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy(),
        e.getStatusReason(),
        e.getCreatedBy(),
        e.getCreatedAt(),
        e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(),
        e.getAuthorizedAt());
  }
}
