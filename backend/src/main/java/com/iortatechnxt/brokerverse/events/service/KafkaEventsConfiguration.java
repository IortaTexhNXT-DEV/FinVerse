package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka wiring when {@code brokerverse.kafka.enabled} is true: the topics of every {@link
 * IntegrationTopic} and their dead-letter topics ({@link KafkaAdmin} creates the missing ones at
 * start-up with the configured partitions and replication factor; broker auto-creation stays off),
 * the consumer error handler (retries, then the record goes to {@code <topic>.dlt}) and the
 * container factory of the dead-letter recorder (which never dead-letters itself).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = EventsProperties.ENABLED_PROPERTY, havingValue = "true")
@EnableKafka
public class KafkaEventsConfiguration {

  /** Container factory of the dead-letter recorder. */
  public static final String DEAD_LETTER_FACTORY = "deadLetterListenerFactory";

  private static final long RECORDER_BACKOFF_MILLIS = 1000;
  private static final long RECORDER_RETRIES = 2;

  /**
   * Topics and dead-letter topics.
   *
   * @param catalogue declared topics
   * @param properties partitions and replication factor
   * @return topics for {@link KafkaAdmin}
   */
  @Bean
  public KafkaAdmin.NewTopics kafkaIntegrationTopics(
      EventTopicCatalogue catalogue, EventsProperties properties) {
    List<NewTopic> topics = new ArrayList<>();
    for (IntegrationTopic topic : catalogue.topics()) {
      topics.add(topic(topic.name(), properties));
      topics.add(topic(topic.deadLetterTopic(), properties));
    }
    return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
  }

  /**
   * Error handler of the event consumers: {@code brokerverse.kafka.consumer-retries} retries, then
   * the record is published unchanged (with the exception in its headers) to {@code <topic>.dlt}.
   * Malformed events go to the dead-letter topic at once.
   *
   * @param template Kafka template
   * @param properties retries and back-off
   * @return error handler (picked up by the default listener container factory)
   */
  @Bean
  public CommonErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, String> template, EventsProperties properties) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            template,
            (event, ex) ->
                new TopicPartition(event.topic() + IntegrationTopic.DEAD_LETTER_SUFFIX, -1));
    DefaultErrorHandler handler =
        new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(
                properties.consumerRetryBackoff().toMillis(), properties.consumerRetries()));
    handler.addNotRetryableExceptions(JsonProcessingException.class, InvalidEventException.class);
    return handler;
  }

  /**
   * Container factory of the dead-letter recorder: a record it cannot store is logged and skipped
   * after two retries (it has nowhere further to go).
   *
   * @param consumerFactory consumer factory
   * @return container factory
   */
  @Bean(DEAD_LETTER_FACTORY)
  public ConcurrentKafkaListenerContainerFactory<String, String> deadLetterListenerFactory(
      ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(
        new DefaultErrorHandler(new FixedBackOff(RECORDER_BACKOFF_MILLIS, RECORDER_RETRIES)));
    return factory;
  }

  private static NewTopic topic(String name, EventsProperties properties) {
    return TopicBuilder.name(name)
        .partitions(properties.partitions())
        .replicas(properties.replicationFactor())
        .build();
  }
}
