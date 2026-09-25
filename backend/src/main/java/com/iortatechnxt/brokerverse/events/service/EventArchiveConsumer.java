package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Clock;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Archives every integration event of every declared topic in {@code evt_archive} (support
 * traceability: what left the application, when, with which correlation id). Idempotent: a
 * redelivered event is stored once. A malformed record goes to the dead-letter topic.
 */
@Component
@ConditionalOnProperty(name = EventsProperties.ENABLED_PROPERTY, havingValue = "true")
public class EventArchiveConsumer {

  private final EventEnvelopeReader reader;
  private final EventArchiveStore store;
  private final Clock clock;

  /**
   * Creates the consumer.
   *
   * @param reader envelope reader
   * @param store archive store
   * @param clock clock
   */
  public EventArchiveConsumer(EventEnvelopeReader reader, EventArchiveStore store, Clock clock) {
    this.reader = reader;
    this.store = store;
    this.clock = clock;
  }

  /**
   * Archives one record.
   *
   * @param record consumed record
   * @throws JsonProcessingException when the record is not an envelope (dead-lettered)
   */
  @KafkaListener(
      id = "event-archive",
      groupId = "${brokerverse.kafka.consumer-group-prefix:bibs}-event-archive",
      topics = "#{@eventTopicCatalogue.names()}")
  public void archive(ConsumerRecord<String, String> record) throws JsonProcessingException {
    EventEnvelope envelope = reader.read(record.value());
    store.insert(
        envelope,
        record.topic(),
        record.partition(),
        record.offset(),
        record.value(),
        clock.instant());
  }
}
