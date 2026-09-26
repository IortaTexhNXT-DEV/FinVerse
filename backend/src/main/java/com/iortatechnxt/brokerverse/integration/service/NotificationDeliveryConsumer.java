package com.iortatechnxt.brokerverse.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iortatechnxt.brokerverse.common.runtime.ConditionalOnWorkload;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.events.service.EventEnvelope;
import com.iortatechnxt.brokerverse.events.service.EventEnvelopeReader;
import com.iortatechnxt.brokerverse.events.service.EventsProperties;
import com.iortatechnxt.brokerverse.messaging.service.MailDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous e-mail delivery: consumes {@code bibs.messaging.notification-requested.v1} and
 * delivers the queued message (one attempt; a failed attempt stays queued for the {@code
 * MAIL_DISPATCH} job, which keeps retrying). Idempotent: a message already sent is skipped. When
 * Kafka is disabled this consumer does not exist and the mail relay of the messaging module
 * (delivery after commit, then the job) does the work.
 *
 * <p>Runs only on instances whose runtime role carries the integration workload ({@code
 * integration} or {@code all}).
 */
@Component
@ConditionalOnProperty(name = EventsProperties.ENABLED_PROPERTY, havingValue = "true")
@ConditionalOnWorkload(Workload.INTEGRATION)
public class NotificationDeliveryConsumer {

  private final EventEnvelopeReader reader;
  private final MailDispatcher dispatcher;

  /**
   * Creates the consumer.
   *
   * @param reader envelope reader
   * @param dispatcher e-mail dispatcher
   */
  public NotificationDeliveryConsumer(EventEnvelopeReader reader, MailDispatcher dispatcher) {
    this.reader = reader;
    this.dispatcher = dispatcher;
  }

  /**
   * Delivers the requested message.
   *
   * @param record consumed record
   * @throws JsonProcessingException when the record is not an envelope (dead-lettered)
   */
  @KafkaListener(
      id = "notification-delivery",
      groupId = "${brokerverse.kafka.consumer-group-prefix:bibs}-mail-dispatch",
      topics = IntegrationTopics.NOTIFICATION_REQUESTED)
  public void deliver(ConsumerRecord<String, String> record) throws JsonProcessingException {
    EventEnvelope envelope = reader.read(record.value());
    dispatcher.dispatch(EventEnvelopeReader.longField(envelope, "messageId"));
  }
}
