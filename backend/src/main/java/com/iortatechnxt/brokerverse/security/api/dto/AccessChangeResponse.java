package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import java.time.Instant;

/**
 * One logged change of a user's access.
 *
 * @param id row id
 * @param occurredAt time of the change
 * @param activity what was done (CREATE_USER, ROLES_CHANGED, UNLOCK ...)
 * @param attribute changed attribute
 * @param fromValue value before
 * @param toValue value after
 * @param requestNo UIDM-ISC request number or access request number
 * @param doneBy who made the change (UIDM-ISC for the provisioning interface)
 * @param approvedBy who approved it
 */
public record AccessChangeResponse(
    Long id,
    Instant occurredAt,
    String activity,
    String attribute,
    String fromValue,
    String toValue,
    String requestNo,
    String doneBy,
    String approvedBy) {

  /**
   * Maps a change row.
   *
   * @param c change
   * @return response
   */
  public static AccessChangeResponse from(AccessChangeLog c) {
    return new AccessChangeResponse(
        c.getId(),
        c.getOccurredAt(),
        c.getActivity().name(),
        c.getAttribute(),
        c.getFromValue(),
        c.getToValue(),
        c.getRequestNo(),
        c.getDoneBy(),
        c.getApprovedBy());
  }
}
