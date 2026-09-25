package com.iortatechnxt.brokerverse.support;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Base of the tests that run the application with Redis and Kafka enabled, without Docker: an
 * in-JVM Redis protocol server ({@link EmbeddedRedis}) and an embedded KRaft Kafka broker. Every
 * test class starts its own context (the annotations of {@code IntegrationTest}, declared here so
 * the embedded database applies) and closes it afterwards, so the broker and the second context do
 * not stay in memory for the rest of the run.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
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
