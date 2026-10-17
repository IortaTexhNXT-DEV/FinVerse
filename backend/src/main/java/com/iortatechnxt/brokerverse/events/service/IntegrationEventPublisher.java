package com.iortatechnxt.brokerverse.events.service;

import java.util.UUID;

/**
 * Publishes integration events through the transactional outbox. Call it <b>inside</b> the business
 * transaction: the event is stored in {@code evt_outbox} with the business change and leaves the
 * application only if that transaction commits. After the commit the relay sends it to Kafka (in
 * order per key, retried until the broker acknowledges it). When Kafka is disabled the event is
 * recorded as delivered in-process ({@code LOCAL}).
 *
 * <p>Delivery is at least once: consumers deduplicate on the envelope {@code eventId}.
 */
public interface IntegrationEventPublisher {

  /**
   * Stores an event in the outbox of the current transaction (a new transaction when none is
   * active).
   *
   * @param event the event
   * @return the event id (envelope {@code eventId})
   */
  UUID publish(IntegrationEvent event);
}
