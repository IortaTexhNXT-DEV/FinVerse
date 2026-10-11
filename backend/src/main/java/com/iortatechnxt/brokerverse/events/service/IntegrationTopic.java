package com.iortatechnxt.brokerverse.events.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Declaration of one integration topic, published as a Spring bean by the module that produces the
 * events. The application creates the topic and its dead-letter topic ({@code <name>.dlt}) with
 * {@code brokerverse.kafka.partitions} and {@code replication-factor}; broker-side auto-creation
 * stays off.
 *
 * @param name topic name {@code bibs.<domain>.<event>.v<version>} (lower case, hyphens)
 * @param eventTypes event types carried (envelope {@code type})
 * @param description what the topic carries, for the support screen and the catalogue
 */
public record IntegrationTopic(String name, List<String> eventTypes, String description) {

  /** Pattern of a topic name. */
  public static final Pattern NAME = Pattern.compile("bibs\\.[a-z0-9-]+\\.[a-z0-9-]+\\.v[0-9]+");

  /** Suffix of the dead-letter topic of a topic. */
  public static final String DEAD_LETTER_SUFFIX = ".dlt";

  /** Validates the name and copies. */
  public IntegrationTopic {
    Objects.requireNonNull(name, "name");
    if (!NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("Topic name must match bibs.<domain>.<event>.vN: " + name);
    }
    eventTypes = List.copyOf(eventTypes);
  }

  /**
   * The dead-letter topic.
   *
   * @return {@code <name>.dlt}
   */
  public String deadLetterTopic() {
    return name + DEAD_LETTER_SUFFIX;
  }
}
