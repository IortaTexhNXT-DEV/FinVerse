package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.NewOutboxRow;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxStatus;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Outbox implementation of {@link IntegrationEventPublisher}: one {@code evt_outbox} row per event
 * in the caller's transaction; after the commit the relay is asked to send at once (the {@code
 * EVENT_OUTBOX_RELAY} job sends anything left and the retries). With Kafka disabled the row is
 * stored as {@code LOCAL} (delivered in-process) and nothing is queued.
 */
@Service
public class OutboxEventPublisher implements IntegrationEventPublisher {

  private final OutboxStore store;
  private final EventTopicCatalogue topics;
  private final OutboxRelay relay;
  private final ObjectMapper mapper;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final EventsProperties properties;

  /**
   * Creates the publisher.
   *
   * @param store outbox store
   * @param topics declared topics
   * @param relay outbox relay (woken after commit)
   * @param mapper JSON mapper of the payloads
   * @param currentUser current user (row author)
   * @param clock clock
   * @param properties Kafka switch
   */
  public OutboxEventPublisher(
      OutboxStore store,
      EventTopicCatalogue topics,
      OutboxRelay relay,
      ObjectMapper mapper,
      CurrentUser currentUser,
      Clock clock,
      EventsProperties properties) {
    this.store = store;
    this.topics = topics;
    this.relay = relay;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.clock = clock;
    this.properties = properties;
  }

  @Override
  @Transactional
  public UUID publish(IntegrationEvent event) {
    NewOutboxRow row = row(event);
    store.insert(row);
    if (properties.enabled() && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              relay.requestDrain();
            }
          });
    }
    return row.eventId();
  }

  /**
   * Builds the outbox row of an event (also used by the entity-change capture).
   *
   * @param event event
   * @return row
   */
  public NewOutboxRow row(IntegrationEvent event) {
    if (!topics.isDeclared(event.topic())) {
      throw new IllegalArgumentException("Undeclared integration topic " + event.topic());
    }
    try {
      return new NewOutboxRow(
          UUID.randomUUID(),
          event.topic(),
          event.type(),
          event.key(),
          event.companyId(),
          CorrelationIdFilter.current(),
          clock.instant(),
          mapper.writeValueAsString(event.payload()),
          properties.enabled() ? OutboxStatus.PENDING : OutboxStatus.LOCAL,
          currentUser.username());
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Event payload is not serializable: " + event.type(), ex);
    }
  }
}
