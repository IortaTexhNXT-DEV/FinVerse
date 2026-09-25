package com.iortatechnxt.brokerverse.events.api.dto;

import com.iortatechnxt.brokerverse.events.service.EventArchiveStore.ArchivedEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * One archived event.
 *
 * @param id row id
 * @param eventId event id
 * @param topic topic
 * @param type event type
 * @param key event key
 * @param companyCode company
 * @param correlationId correlation id
 * @param occurredAt event time
 * @param partition Kafka partition
 * @param offset Kafka offset
 * @param archivedAt archive time
 * @param envelope envelope JSON
 */
public record ArchivedEventResponse(
    long id,
    UUID eventId,
    String topic,
    String type,
    String key,
    String companyCode,
    String correlationId,
    Instant occurredAt,
    int partition,
    long offset,
    Instant archivedAt,
    String envelope) {

  /**
   * Maps an archived event.
   *
   * @param e archived event
   * @return response
   */
  public static ArchivedEventResponse from(ArchivedEvent e) {
    return new ArchivedEventResponse(
        e.id(),
        e.eventId(),
        e.topic(),
        e.type(),
        e.key(),
        e.companyCode(),
        e.correlationId(),
        e.occurredAt(),
        e.partition(),
        e.offset(),
        e.archivedAt(),
        e.envelope());
  }
}
