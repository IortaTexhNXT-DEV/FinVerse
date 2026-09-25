package com.iortatechnxt.brokerverse.events.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Every declared {@link IntegrationTopic}; the consumers subscribe through it ({@code
 * #{@eventTopicCatalogue.names()}}) and the relay refuses events for undeclared topics.
 */
@Component
public class EventTopicCatalogue {

  private final List<IntegrationTopic> topics;

  /**
   * Creates the catalogue.
   *
   * @param topics declared topics
   */
  public EventTopicCatalogue(List<IntegrationTopic> topics) {
    Set<String> names = new HashSet<>();
    for (IntegrationTopic topic : topics) {
      if (!names.add(topic.name())) {
        throw new IllegalStateException("Topic declared twice: " + topic.name());
      }
    }
    this.topics = topics.stream().sorted(Comparator.comparing(IntegrationTopic::name)).toList();
  }

  /**
   * Declared topics.
   *
   * @return topics by name
   */
  public List<IntegrationTopic> topics() {
    return topics;
  }

  /**
   * Whether a topic is declared.
   *
   * @param name topic name
   * @return true when declared
   */
  public boolean isDeclared(String name) {
    return topics.stream().anyMatch(t -> t.name().equals(name));
  }

  /**
   * Names of the declared topics.
   *
   * @return topic names (empty array when none)
   */
  public String[] names() {
    return topics.stream().map(IntegrationTopic::name).toArray(String[]::new);
  }

  /**
   * Names of the dead-letter topics.
   *
   * @return dead-letter topic names
   */
  public String[] deadLetterNames() {
    return topics.stream().map(IntegrationTopic::deadLetterTopic).toArray(String[]::new);
  }
}
