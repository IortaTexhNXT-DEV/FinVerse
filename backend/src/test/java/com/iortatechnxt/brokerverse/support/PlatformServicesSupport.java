package com.iortatechnxt.brokerverse.support;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base of the tests that run the application with Redis and Kafka enabled, without Docker: an
 * in-JVM Redis protocol server ({@link EmbeddedRedis}) and an embedded KRaft Kafka broker. Every
 * subclass shares one application context.
 */
@IntegrationTest
@EmbeddedKafka(
    kraft = true,
    partitions = 2,
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(
    properties = {
      "brokerverse.redis.enabled=true",
      "brokerverse.redis.key-prefix=it:",
      "brokerverse.kafka.enabled=true",
      "brokerverse.kafka.partitions=2",
      "brokerverse.kafka.consumer-retries=1",
      "brokerverse.kafka.consumer-retry-backoff=PT0.2S",
      "brokerverse.kafka.consumer-group-prefix=it",
      "brokerverse.jobs.lock-lease=PT5S"
    })
public abstract class PlatformServicesSupport {

  /**
   * Points the application at the in-JVM Redis server.
   *
   * @param registry property registry
   */
  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", () -> "localhost");
    registry.add("spring.data.redis.port", EmbeddedRedis::port);
  }
}
