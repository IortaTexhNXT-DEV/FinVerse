package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent;
import java.time.Instant;

/**
 * One entry of the case timeline (FR-SS-040 Timeline tab).
 *
 * @param id event id
 * @param event event type
 * @param roundNo round
 * @param fromStage stage before
 * @param toStage stage after
 * @param fromValue value before
 * @param toValue value after
 * @param reasonCode reason or disposition
 * @param remarks remarks
 * @param actor user
 * @param occurredAt time
 */
public record CaseEventDto(
    Long id,
    String event,
    int roundNo,
    String fromStage,
    String toStage,
    String fromValue,
    String toValue,
    String reasonCode,
    String remarks,
    String actor,
    Instant occurredAt) {

  /**
   * Maps an event.
   *
   * @param e the event
   * @return the DTO
   */
  public static CaseEventDto from(CaseEvent e) {
    return new CaseEventDto(
        e.getId(),
        e.getEvent().name(),
        e.getRoundNo(),
        e.getFromStage(),
        e.getToStage(),
        e.getFromValue(),
        e.getToValue(),
        e.getReasonCode(),
        e.getRemarks(),
        e.getActor(),
        e.getOccurredAt());
  }
}
