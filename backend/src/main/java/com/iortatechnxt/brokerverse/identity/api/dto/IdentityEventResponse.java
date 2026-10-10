package com.iortatechnxt.brokerverse.identity.api.dto;

import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import java.time.Instant;

/**
 * A provisioning event and its outcome (Identity Synchronisation screen).
 *
 * @param id event number
 * @param receivedAt time received
 * @param source source code
 * @param sourceLabel source in words
 * @param eventType JOINER, MOVER, LEAVER, REHIRE or STATUS
 * @param windowsId Windows ID
 * @param userId user ID of the event
 * @param uidmRequestNo UIDM request number
 * @param adStatus AD status of the event
 * @param status APPLIED, NO_CHANGE, REFUSED or FAILED
 * @param username user concerned
 * @param message what was done or why it was refused
 * @param processedAt time processed
 * @param processedBy who processed it
 * @param attempts number of processings
 * @param reprocessable whether it can be processed again
 */
public record IdentityEventResponse(
    Long id,
    Instant receivedAt,
    String source,
    String sourceLabel,
    String eventType,
    String windowsId,
    String userId,
    String uidmRequestNo,
    String adStatus,
    String status,
    String username,
    String message,
    Instant processedAt,
    String processedBy,
    int attempts,
    boolean reprocessable) {

  /**
   * Maps an event.
   *
   * @param e event
   * @return response
   */
  public static IdentityEventResponse from(IdentityEvent e) {
    return new IdentityEventResponse(
        e.getId(),
        e.getReceivedAt(),
        e.getSource().name(),
        e.getSource().label(),
        e.getEventType().name(),
        e.getWindowsId(),
        e.getUserId(),
        e.getUidmRequestNo(),
        e.getAdStatus() == null ? null : e.getAdStatus().name(),
        e.getStatus().name(),
        e.getUsername(),
        e.getMessage(),
        e.getProcessedAt(),
        e.getProcessedBy(),
        e.getAttempts(),
        e.reprocessable());
  }
}
