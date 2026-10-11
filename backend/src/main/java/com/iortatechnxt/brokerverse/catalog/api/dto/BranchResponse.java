package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;

/**
 * An insurer branch with its LGT rate.
 *
 * @param id id
 * @param insurerId insurer profile
 * @param code branch code
 * @param name name
 * @param city city
 * @param lgtRate LGT rate %
 * @param placementEmail placement mailbox
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record BranchResponse(
    Long id,
    Long insurerId,
    String code,
    String name,
    String city,
    BigDecimal lgtRate,
    String placementEmail,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static BranchResponse from(InsurerBranch e) {
    return new BranchResponse(
        e.getId(),
        e.getInsurerId(),
        e.getCode(),
        e.getName(),
        e.getCity(),
        e.getLgtRate(),
        e.getPlacementEmail(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
