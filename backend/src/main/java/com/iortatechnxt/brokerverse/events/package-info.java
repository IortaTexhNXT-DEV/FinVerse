/**
 * Integration events (platform): the transactional outbox ({@code evt_outbox}) behind {@link
 * com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher}, the relay that publishes
 * committed events to Apache Kafka (in order per key, idempotent producer, retries), the event
 * archive consumer ({@code evt_archive}), dead-letter handling ({@code evt_dead_letter}) and the
 * support API. In-process Spring events stay for logic inside one transaction; integration events
 * carry business facts out of the transaction.
 *
 * <p>See docs/architecture/PLATFORM_CACHE_AND_EVENTS.md.
 */
package com.iortatechnxt.brokerverse.events;
