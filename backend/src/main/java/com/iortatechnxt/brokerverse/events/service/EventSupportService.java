package com.iortatechnxt.brokerverse.events.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.events.service.DeadLetterStore.DeadLetter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Support actions on integration events (System Administrator): requeue a FAILED outbox row, retry
 * a dead letter (the stored record is published again, unchanged, to its original topic, so every
 * consumer group of that topic sees it again and deduplicates on the event id) or discard it.
 */
@Service
public class EventSupportService {

  private static final Logger LOG = LoggerFactory.getLogger(EventSupportService.class);
  private static final String DEAD_LETTER = "Dead letter";

  private final OutboxStore outbox;
  private final OutboxRelay relay;
  private final DeadLetterStore deadLetters;
  private final ObjectProvider<KafkaTemplate<String, String>> kafka;
  private final EventsProperties properties;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param outbox outbox store
   * @param relay outbox relay
   * @param deadLetters dead-letter store
   * @param kafka Kafka template (present when Kafka is enabled)
   * @param properties Kafka settings
   * @param currentUser current user
   * @param clock clock
   */
  public EventSupportService(
      OutboxStore outbox,
      OutboxRelay relay,
      DeadLetterStore deadLetters,
      ObjectProvider<KafkaTemplate<String, String>> kafka,
      EventsProperties properties,
      CurrentUser currentUser,
      Clock clock) {
    this.outbox = outbox;
    this.relay = relay;
    this.deadLetters = deadLetters;
    this.kafka = kafka;
    this.properties = properties;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Puts an outbox row back in the queue and wakes the relay.
   *
   * @param id outbox row id
   */
  public void requeueOutbox(long id) {
    requireKafka();
    OutboxStore.OutboxEntry entry =
        outbox.find(id).orElseThrow(() -> new ResourceNotFoundException("Outbox event", id));
    if (!outbox.requeue(id, clock.instant())) {
      throw new BusinessRuleException(
          "OUTBOX_NOT_REQUEUEABLE", "Only FAILED or PENDING events can be sent again");
    }
    LOG.info("Outbox event {} requeued by {}", entry.eventId(), currentUser.username());
    relay.requestDrain();
  }

  /**
   * Publishes a dead letter again to its original topic.
   *
   * @param id dead-letter id
   */
  public void retryDeadLetter(long id) {
    requireKafka();
    DeadLetter letter = requireNew(id);
    ProducerRecord<String, String> record =
        new ProducerRecord<>(letter.originalTopic(), letter.key(), letter.payload());
    if (letter.eventId() != null) {
      record.headers().add("eventId", letter.eventId().toString().getBytes(StandardCharsets.UTF_8));
    }
    try {
      kafka
          .getObject()
          .send(record)
          .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException | TimeoutException ex) {
      throw new BusinessRuleException(
          "DEAD_LETTER_RETRY_FAILED", "Kafka did not accept the event: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BusinessRuleException("DEAD_LETTER_RETRY_FAILED", "Retry interrupted", ex);
    }
    deadLetters.resolve(id, "RETRIED", currentUser.username(), clock.instant());
    LOG.info("Dead letter {} retried by {}", id, currentUser.username());
  }

  /**
   * Discards a dead letter (no further processing).
   *
   * @param id dead-letter id
   */
  public void discardDeadLetter(long id) {
    requireNew(id);
    deadLetters.resolve(id, "DISCARDED", currentUser.username(), clock.instant());
    LOG.info("Dead letter {} discarded by {}", id, currentUser.username());
  }

  private DeadLetter requireNew(long id) {
    DeadLetter letter =
        deadLetters.find(id).orElseThrow(() -> new ResourceNotFoundException(DEAD_LETTER, id));
    if (!"NEW".equals(letter.status())) {
      throw new BusinessRuleException(
          "DEAD_LETTER_RESOLVED", "The dead letter was already " + letter.status());
    }
    return letter;
  }

  private void requireKafka() {
    if (!properties.enabled()) {
      throw new BusinessRuleException(
          "KAFKA_DISABLED", "Kafka is disabled (brokerverse.kafka.enabled=false)");
    }
  }
}
