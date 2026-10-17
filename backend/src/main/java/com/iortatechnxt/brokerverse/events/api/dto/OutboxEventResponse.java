package com.iortatechnxt.brokerverse.events.api.dto;

import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxEntry;
import java.time.Instant;
import java.util.UUID;

/**
 * One outbox row for the support screen.
 *
 * @param id row id
 * @param eventId event id
 * @param topic topic
 * @param type event type
 * @param key ordering key
 * @param companyCode company
 * @param correlationId correlation id
 * @param occurredAt event time
 * @param status PENDING, SENT, LOCAL or FAILED
 * @param attempts failed send attempts
 * @param nextAttemptAt next attempt
 * @param lastError last send error
 * @param sentAt acknowledgement time
 * @param payload JSON payload
 */
public record OutboxEventResponse(
    long id,
    UUID eventId,
    String topic,
    String type,
    String key,
    String companyCode,
    String correlationId,
    Instant occurredAt,
    String status,
    int attempts,
    Instant nextAttemptAt,
    String lastError,
    Instant sentAt,
    String payload) {

  /**
   * Maps a row.
   *
   * @param e row
   * @return response
   */
  public static OutboxEventResponse from(OutboxEntry e) {
    return new OutboxEventResponse(
        e.id(),
        e.eventId(),
        e.topic(),
        e.type(),
        e.key(),
        e.companyCode(),
        e.correlationId(),
        e.occurredAt(),
        e.status().name(),
        e.attempts(),
        e.nextAttemptAt(),
        e.lastError(),
        e.sentAt(),
        e.payload());
  }
}
