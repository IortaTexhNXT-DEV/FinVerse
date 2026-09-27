package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;

/**
 * An account officer of a team.
 *
 * @param id id
 * @param teamCode team
 * @param username user
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record SalesOfficerResponse(
    Long id,
    String teamCode,
    String username,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static SalesOfficerResponse from(SalesOfficer e) {
    return new SalesOfficerResponse(
        e.getId(),
        e.getTeamCode(),
        e.getUsername(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
