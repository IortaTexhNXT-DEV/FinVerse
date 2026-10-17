package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON envelope of every message on an integration topic (schema version 1). The Kafka record
 * key is {@link #key}; the headers repeat {@code eventId}, {@code eventType} and {@code
 * correlationId}.
 *
 * @param eventId unique event id (consumers deduplicate on it)
 * @param type event type, e.g. {@code booking.invoice.booked}
 * @param schemaVersion envelope version (1)
 * @param occurredAt when the business transaction recorded the event
 * @param company company code, null when not company specific
 * @param key ordering key
 * @param correlationId id of the request or job that caused the event (log correlation)
 * @param source producing system ({@code brokerverse})
 * @param payload event body
 */
public record EventEnvelope(
    UUID eventId,
    String type,
    int schemaVersion,
    Instant occurredAt,
    String company,
    String key,
    String correlationId,
    String source,
    JsonNode payload) {

  /** Current envelope version. */
  public static final int SCHEMA_VERSION = 1;

  /** Producing system. */
  public static final String SOURCE = "brokerverse";
}
