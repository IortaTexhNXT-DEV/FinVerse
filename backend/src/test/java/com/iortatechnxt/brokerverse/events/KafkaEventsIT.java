package com.iortatechnxt.brokerverse.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.events.service.OutboxStore;
import com.iortatechnxt.brokerverse.integration.service.IntegrationTopics;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.PlatformServicesSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration events with an embedded Kafka broker: outbox relay to the topic and the consumers
 * (archive, e-mail dispatch), order per key, dead-letter topic and the support API with retry.
 */
class KafkaEventsIT extends PlatformServicesSupport {

  private static final Duration WAIT = Duration.ofSeconds(90);
  private static final String ADMIN = "admin";

  @Autowired private IntegrationEventPublisher publisher;
  @Autowired private OutboxStore outbox;
  @Autowired private MessageService messages;
  @Autowired private KafkaTemplate<String, String> kafka;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @Autowired private AsUser as;
  @Autowired private Api api;

  @Test
  void aQueuedEmailIsRelayedToKafkaAndDeliveredByTheConsumer() {
    QueuedEmail queued =
        as.run(
            "proc",
            () ->
                messages.queueEmail(
                    new OutboundEmail(
                        null,
                        "TEST",
                        List.of("client@example.ph"),
                        null,
                        "Kafka delivery",
                        "Body",
                        null,
                        null,
                        null)));
    String key = String.valueOf(queued.messageId());
    await()
        .atMost(WAIT)
        .untilAsserted(
            () ->
                assertThat(messages.get(queued.messageId()).getStatus())
                    .isEqualTo(MessageStatus.SENT));
    await().atMost(WAIT).untilAsserted(() -> assertThat(archived(key)).hasSize(1));
    OutboxStore.OutboxEntry row =
        outbox.search(new OutboxStore.OutboxFilter(null, null, key), 1, 0).get(0);
    assertThat(row.topic()).isEqualTo(IntegrationTopics.NOTIFICATION_REQUESTED);
    assertThat(row.status()).isEqualTo(OutboxStore.OutboxStatus.SENT);
    assertThat(row.sentAt()).isNotNull();
  }

  @Test
  void eventsOfOneKeyArriveInPublicationOrder() throws Exception {
    String key = "ORDER-" + UUID.randomUUID();
    IntStream.range(0, 25)
        .forEach(
            i ->
                tx.executeWithoutResult(
                    s ->
                        publisher.publish(
                            new IntegrationEvent(
                                IntegrationTopics.COLLECTION_FEED_READY,
                                IntegrationTopics.TYPE_COLLECTION_FEED_READY,
                                key,
                                null,
                                Map.of("feedCode", key, "seq", i)))));
    await().atMost(WAIT).untilAsserted(() -> assertThat(archived(key)).hasSize(25));
    List<Integer> order =
        archived(key).stream().map(json -> payload(json).path("seq").asInt()).toList();
    assertThat(order).isEqualTo(IntStream.range(0, 25).boxed().toList());
    JsonNode envelope = mapper.readTree(archived(key).get(0));
    assertThat(envelope.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(envelope.path("type").asText())
        .isEqualTo(IntegrationTopics.TYPE_COLLECTION_FEED_READY);
    assertThat(envelope.path("correlationId").asText()).isNotBlank();
    assertThat(envelope.path("source").asText()).isEqualTo("brokerverse");
  }

  @Test
  void aPoisonMessageGoesToTheDeadLetterTopicAndCanBeRetriedOrDiscarded() throws Exception {
    String key = "POISON-" + UUID.randomUUID();
    kafka.send(IntegrationTopics.INVOICE_BOOKED, key, "this is not an envelope").get();
    await().atMost(WAIT).untilAsserted(() -> assertThat(deadLetterIds(key)).hasSize(1));
    long first = deadLetterIds(key).get(0);
    api.doGet(ADMIN, "/api/v1/admin/events/dead-letters?status=NEW&size=200")
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[?(@.key == '" + key + "')].originalTopic")
                .value(IntegrationTopics.INVOICE_BOOKED));

    api.doPost(ADMIN, "/api/v1/admin/events/dead-letters/" + first + "/retry", null)
        .andExpect(status().isNoContent());
    api.doPost(ADMIN, "/api/v1/admin/events/dead-letters/" + first + "/retry", null)
        .andExpect(status().isUnprocessableEntity());
    // Still not an envelope: it comes back to the dead-letter topic once more.
    await().atMost(WAIT).untilAsserted(() -> assertThat(deadLetterIds(key)).hasSize(2));
    long second = deadLetterIds(key).get(1);
    api.doPost(ADMIN, "/api/v1/admin/events/dead-letters/" + second + "/discard", null)
        .andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForList(
                "select status from evt_dead_letter where event_key = ? order by id",
                String.class,
                key))
        .containsExactly("RETRIED", "DISCARDED");
    api.doGet(ADMIN, "/api/v1/admin/events/dead-letters?status=ALL").andExpect(status().isOk());
    api.doPost(ADMIN, "/api/v1/admin/events/dead-letters/999999999/discard", null)
        .andExpect(status().isNotFound());
  }

  @Test
  void aFailedOutboxEventCanBeSentAgainAndTheSupportApiListsEvents() throws Exception {
    String key = "REQUEUE-" + UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    outbox.insert(
        new OutboxStore.NewOutboxRow(
            eventId,
            IntegrationTopics.PRODUCT_VERSION_RELEASED,
            IntegrationTopics.TYPE_PRODUCT_VERSION_RELEASED,
            key,
            null,
            "test-correlation",
            Instant.now(),
            "{\"productCode\":\"" + key + "\",\"versionNo\":1}",
            OutboxStore.OutboxStatus.FAILED,
            "SYSTEM"));
    long id = outbox.search(new OutboxStore.OutboxFilter("FAILED", null, key), 1, 0).get(0).id();

    api.doGet(ADMIN, "/api/v1/admin/events/topics")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(9))
        .andExpect(jsonPath("$[0].kafkaEnabled").value(true));
    api.doPost(ADMIN, "/api/v1/admin/events/outbox/" + id + "/retry", null)
        .andExpect(status().isNoContent());
    await()
        .atMost(WAIT)
        .untilAsserted(
            () ->
                assertThat(outbox.find(id).orElseThrow().status())
                    .isEqualTo(OutboxStore.OutboxStatus.SENT));
    api.doPost(ADMIN, "/api/v1/admin/events/outbox/" + id + "/retry", null)
        .andExpect(status().isUnprocessableEntity());
    await().atMost(WAIT).untilAsserted(() -> assertThat(archived(key)).hasSize(1));
    api.doGet(ADMIN, "/api/v1/admin/events/outbox?status=SENT&key=" + key)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    api.doGet(ADMIN, "/api/v1/admin/events/archive?key=" + key)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.content[0].correlationId").value("test-correlation"));
    api.doGet("accountant", "/api/v1/admin/events/outbox").andExpect(status().isForbidden());
  }

  private List<String> archived(String key) {
    return jdbc.queryForList(
        "select envelope from evt_archive where event_key = ? order by kafka_offset",
        String.class,
        key);
  }

  private List<Long> deadLetterIds(String key) {
    return jdbc.queryForList(
        "select id from evt_dead_letter where event_key = ? order by id", Long.class, key);
  }

  private JsonNode payload(String envelope) {
    try {
      return mapper.readTree(envelope).path("payload");
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
