package com.iortatechnxt.brokerverse.events.api.dto;

import com.iortatechnxt.brokerverse.events.service.IntegrationTopic;
import java.util.List;

/**
 * One integration topic of the catalogue.
 *
 * @param name topic name
 * @param deadLetterTopic dead-letter topic
 * @param eventTypes event types carried
 * @param description content
 * @param kafkaEnabled true when events are relayed to Kafka (false: recorded LOCAL)
 */
public record TopicResponse(
    String name,
    String deadLetterTopic,
    List<String> eventTypes,
    String description,
    boolean kafkaEnabled) {

  /**
   * Maps a declaration.
   *
   * @param topic declaration
   * @param kafkaEnabled Kafka switch
   * @return response
   */
  public static TopicResponse from(IntegrationTopic topic, boolean kafkaEnabled) {
    return new TopicResponse(
        topic.name(),
        topic.deadLetterTopic(),
        topic.eventTypes(),
        topic.description(),
        kafkaEnabled);
  }
}
