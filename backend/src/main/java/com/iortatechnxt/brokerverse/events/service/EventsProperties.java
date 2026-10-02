package com.iortatechnxt.brokerverse.events.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integration event settings bound from {@code brokerverse.kafka.*}; the connection itself is the
 * standard {@code spring.kafka.*} (bootstrap servers, security protocol, SASL).
 *
 * @param enabled true to relay the outbox to Kafka and run the consumers; false to record every
 *     event as delivered in-process ({@code LOCAL})
 * @param partitions partitions of each topic created by the application (default 3)
 * @param replicationFactor replication factor of each topic (default 1; 3 on Amazon MSK)
 * @param consumerGroupPrefix prefix of the consumer group ids (default {@code bibs})
 * @param relayBatchSize outbox rows sent per relay round (default 200)
 * @param relayMaxAttempts send attempts of a row before it is marked FAILED (default 10)
 * @param relayRetryBackoff first retry delay of a row; doubles per attempt (default 30 seconds)
 * @param sendTimeout wait for the broker acknowledgement of a send (default 30 seconds)
 * @param consumerRetries retries of a failing consumer record before its dead-letter topic (default
 *     3)
 * @param consumerRetryBackoff delay between consumer retries (default 2 seconds)
 * @param outboxRetention age after which delivered outbox rows are deleted (default 30 days)
 * @param archiveRetention age after which archived events are deleted (default 400 days)
 */
@ConfigurationProperties(prefix = "brokerverse.kafka")
public record EventsProperties(
    boolean enabled,
    Integer partitions,
    Short replicationFactor,
    String consumerGroupPrefix,
    Integer relayBatchSize,
    Integer relayMaxAttempts,
    Duration relayRetryBackoff,
    Duration sendTimeout,
    Integer consumerRetries,
    Duration consumerRetryBackoff,
    Duration outboxRetention,
    Duration archiveRetention) {

  /** Property that switches Kafka on. */
  public static final String ENABLED_PROPERTY = "brokerverse.kafka.enabled";

  private static final int DEFAULT_PARTITIONS = 3;
  private static final int DEFAULT_BATCH = 200;
  private static final int DEFAULT_MAX_ATTEMPTS = 10;
  private static final int DEFAULT_CONSUMER_RETRIES = 3;
  private static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofSeconds(30);
  private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_CONSUMER_BACKOFF = Duration.ofSeconds(2);
  private static final Duration DEFAULT_OUTBOX_RETENTION = Duration.ofDays(30);
  private static final Duration DEFAULT_ARCHIVE_RETENTION = Duration.ofDays(400);

  /** Applies the defaults. */
  public EventsProperties {
    partitions = or(partitions, DEFAULT_PARTITIONS);
    replicationFactor = or(replicationFactor, (short) 1);
    consumerGroupPrefix =
        consumerGroupPrefix == null || consumerGroupPrefix.isBlank() ? "bibs" : consumerGroupPrefix;
    relayBatchSize = or(relayBatchSize, DEFAULT_BATCH);
    relayMaxAttempts = or(relayMaxAttempts, DEFAULT_MAX_ATTEMPTS);
    relayRetryBackoff = or(relayRetryBackoff, DEFAULT_RETRY_BACKOFF);
    sendTimeout = or(sendTimeout, DEFAULT_SEND_TIMEOUT);
    consumerRetries = or(consumerRetries, DEFAULT_CONSUMER_RETRIES);
    consumerRetryBackoff = or(consumerRetryBackoff, DEFAULT_CONSUMER_BACKOFF);
    outboxRetention = or(outboxRetention, DEFAULT_OUTBOX_RETENTION);
    archiveRetention = or(archiveRetention, DEFAULT_ARCHIVE_RETENTION);
  }

  private static <T> T or(T value, T fallback) {
    return value == null ? fallback : value;
  }

  /**
   * A consumer group id.
   *
   * @param name group name
   * @return {@code <prefix>-<name>}
   */
  public String group(String name) {
    return consumerGroupPrefix + "-" + name;
  }
}
