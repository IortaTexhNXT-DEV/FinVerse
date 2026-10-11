package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An account officer of a team.
 *
 * @param id id
 * @param teamCode team
 * @param username user
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 * @param assignedSince date of the current team assignment
 * @param statusReason reason of the removal from the team
 * @param lastChangedAt time of the last change
 * @param authorizedAt authorization time
 */
public record SalesOfficerResponse(
    Long id,
    String teamCode,
    String username,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy,
    LocalDate assignedSince,
    String statusReason,
    Instant lastChangedAt,
    Instant authorizedAt) {

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
        e.getAuthorizedBy(),
        e.getAssignedSince() != null || e.getCreatedAt() == null
            ? e.getAssignedSince()
            : BusinessClock.dateOf(e.getCreatedAt()),
        e.getStatusReason(),
        e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(),
        e.getAuthorizedAt());
  }
}
