package com.iortatechnxt.brokerverse.events.service;

import java.util.Objects;

/**
 * A business fact to publish to other systems (and to the asynchronous consumers of this one).
 *
 * @param topic declared topic ({@link IntegrationTopic#name()})
 * @param type event type, e.g. {@code booking.invoice.booked}
 * @param key ordering key: events with the same key are delivered in publication order (for example
 *     the invoice number)
 * @param companyId company the fact belongs to, null when not company specific
 * @param payload event body: a record of plain values, serialized as JSON; never an entity and no
 *     secrets (it leaves the application)
 */
public record IntegrationEvent(
    String topic, String type, String key, Long companyId, Object payload) {

  /** Validates. */
  public IntegrationEvent {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(payload, "payload");
  }
}
