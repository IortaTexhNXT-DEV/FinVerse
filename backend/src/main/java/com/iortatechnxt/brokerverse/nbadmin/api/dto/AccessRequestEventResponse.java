package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEvent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import java.time.Instant;

/**
 * One event of the request history (BRD 1.008; History tab).
 *
 * @param id id
 * @param action what happened
 * @param fromStatus status before
 * @param toStatus status after
 * @param remarks remarks
 * @param actor user
 * @param occurredAt time
 */
public record AccessRequestEventResponse(
    Long id,
    AccessRequestAction action,
    AccessRequestStatus fromStatus,
    AccessRequestStatus toStatus,
    String remarks,
    String actor,
    Instant occurredAt) {

  /**
   * Maps an event.
   *
   * @param e event
   * @return response
   */
  public static AccessRequestEventResponse from(AccessRequestEvent e) {
    return new AccessRequestEventResponse(
        e.getId(),
        e.getAction(),
        e.getFromStatus(),
        e.getToStatus(),
        e.getRemarks(),
        e.getActor(),
        e.getOccurredAt());
  }
}
