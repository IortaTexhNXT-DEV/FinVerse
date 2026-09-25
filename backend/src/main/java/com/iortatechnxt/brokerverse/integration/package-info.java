/**
 * Integration adapters: turn the in-process events of the business modules into integration events
 * (Kafka topics {@code bibs.<domain>.<event>.v1}) through the transactional outbox, and consume the
 * topics this application acts on asynchronously (e-mail dispatch of {@code notification
 * requested}). The source modules are not changed: the adapters listen to their Spring events
 * ({@code @TransactionalEventListener(BEFORE_COMMIT)}, so the outbox row commits with the business
 * change) or, where a module publishes no event (client, receipt), to its entity changes.
 *
 * <p>See docs/architecture/PLATFORM_CACHE_AND_EVENTS.md (topic catalogue).
 */
package com.iortatechnxt.brokerverse.integration;
