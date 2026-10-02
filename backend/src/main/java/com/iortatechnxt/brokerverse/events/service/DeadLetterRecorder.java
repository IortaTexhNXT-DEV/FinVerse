package com.iortatechnxt.brokerverse.events.service;

import com.iortatechnxt.brokerverse.common.runtime.ConditionalOnWorkload;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.events.service.DeadLetterStore.DeadLetter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Records every message of the dead-letter topics in {@code evt_dead_letter} with the original
 * topic, the failing consumer group and the exception (headers added by the dead-letter publisher),
 * for the support screen and its retry.
 *
 * <p>Runs only on instances whose runtime role carries the integration workload ({@code
 * integration} or {@code all}).
 */
@Component
@ConditionalOnProperty(name = EventsProperties.ENABLED_PROPERTY, havingValue = "true")
@ConditionalOnWorkload(Workload.INTEGRATION)
public class DeadLetterRecorder {

  private final DeadLetterStore store;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param store dead-letter store
   * @param clock clock
   */
  public DeadLetterRecorder(DeadLetterStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  /**
   * Records one dead letter.
   *
   * @param record record of a dead-letter topic
   */
  @KafkaListener(
      id = "dead-letter-recorder",
      groupId = "${brokerverse.kafka.consumer-group-prefix:bibs}-dead-letter-recorder",
      topics = "#{@eventTopicCatalogue.deadLetterNames()}",
      containerFactory = KafkaEventsConfiguration.DEAD_LETTER_FACTORY)
  public void record(ConsumerRecord<String, String> record) {
    String original = header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
    if (original == null) {
      original = record.topic().replaceFirst("\\.dlt$", "");
    }
    store.insert(
        new DeadLetter(
            0,
            eventId(header(record, "eventId")),
            original,
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            header(record, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP),
            header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
            record.value() == null ? "" : record.value(),
            "NEW",
            clock.instant(),
            null,
            null));
  }

  private static String header(ConsumerRecord<String, String> record, String name) {
    Header header = record.headers().lastHeader(name);
    return header == null || header.value() == null
        ? null
        : new String(header.value(), StandardCharsets.UTF_8);
  }

  private static UUID eventId(String value) {
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
