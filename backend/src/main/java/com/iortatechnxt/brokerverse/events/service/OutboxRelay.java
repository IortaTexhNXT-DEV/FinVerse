package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxEntry;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Sends committed outbox rows to Kafka.
 *
 * <p>One relay runs at a time in the cluster (the {@link JobLock} of {@value #JOB_NAME}), reading
 * the pending rows in id order, so events of one key reach their partition in publication order.
 * The producer is idempotent ({@code enable.idempotence}, {@code acks=all}), so broker retries
 * never duplicate or reorder. A batch stops at the first row the broker does not acknowledge: the
 * rows before it are marked SENT, that row waits for its next attempt (exponential back-off) and
 * becomes FAILED after {@code brokerverse.kafka.relay-max-attempts}; rows after it are sent again
 * later (at-least-once delivery, consumers deduplicate on the event id).
 *
 * <p>With Kafka disabled the relay marks any pending row as delivered in-process ({@code LOCAL}).
 *
 * <p>The relay belongs to the integration workload: on an instance whose runtime role does not run
 * it (a {@code web} or {@code jobs} instance) the after-commit drain is not started, and the
 * {@value #JOB_NAME} job of the {@code integration} deployment sends the rows on its next run.
 */
@Component
public class OutboxRelay implements DisposableBean {

  /** Job (and job lock) name of the relay. */
  public static final String JOB_NAME = "EVENT_OUTBOX_RELAY";

  private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
  private static final Duration MAX_BACKOFF = Duration.ofHours(1);
  private static final long BUSY_RETRY_MILLIS = 1000;
  private static final int MAX_ERROR = 1000;
  private static final int MAX_BACKOFF_DOUBLINGS = 16;

  private final OutboxStore store;
  private final EventsProperties properties;
  private final ObjectProvider<KafkaTemplate<String, String>> kafka;
  private final JobLock jobLock;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final boolean drainsAfterCommit;
  private final AtomicBoolean drainQueued = new AtomicBoolean();
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread thread = new Thread(r, "outbox-relay");
            thread.setDaemon(true);
            return thread;
          });

  /**
   * Creates the relay.
   *
   * @param store outbox store
   * @param properties Kafka settings
   * @param kafka Kafka template (present when Kafka is enabled)
   * @param jobLock cluster-wide lock
   * @param mapper JSON mapper (envelopes)
   * @param clock clock
   * @param role runtime role (the after-commit drain runs with the integration workload only)
   */
  public OutboxRelay(
      OutboxStore store,
      EventsProperties properties,
      ObjectProvider<KafkaTemplate<String, String>> kafka,
      JobLock jobLock,
      ObjectMapper mapper,
      Clock clock,
      CurrentRuntimeRole role) {
    this.store = store;
    this.properties = properties;
    this.kafka = kafka;
    this.jobLock = jobLock;
    this.mapper = mapper;
    this.clock = clock;
    this.drainsAfterCommit = role.runs(Workload.INTEGRATION);
  }

  /**
   * Asks for a drain soon (after a commit that added rows); returns at once. Requests arriving
   * while one is queued are merged. Does nothing on an instance without the integration workload.
   */
  public void requestDrain() {
    if (drainsAfterCommit && drainQueued.compareAndSet(false, true)) {
      executor.execute(() -> drainInBackground(true));
    }
  }

  /**
   * Drains while the caller already holds the relay lock (the {@value #JOB_NAME} job).
   *
   * @return rows sent (or marked LOCAL when Kafka is disabled)
   */
  public int drainUnderJob() {
    if (!properties.enabled()) {
      return store.markPendingLocal(clock.instant());
    }
    return sendDue();
  }

  private void drainInBackground(boolean retryWhenBusy) {
    drainQueued.set(false);
    try {
      Optional<JobLock.Lease> lease = jobLock.tryAcquire(JOB_NAME);
      if (lease.isEmpty()) {
        if (retryWhenBusy) {
          executor.schedule(
              () -> drainInBackground(false), BUSY_RETRY_MILLIS, TimeUnit.MILLISECONDS);
        }
        return;
      }
      try (JobLock.Lease held = lease.get()) {
        LOG.debug("Outbox relay runs with fencing token {}", held.fencingToken());
        drainUnderJob();
      }
    } catch (RuntimeException ex) {
      LOG.error("Outbox relay failed; the {} job retries", JOB_NAME, ex);
    }
  }

  private int sendDue() {
    int sent = 0;
    while (true) {
      List<OutboxEntry> batch = store.due(clock.instant(), properties.relayBatchSize());
      if (batch.isEmpty()) {
        return sent;
      }
      int acknowledged = send(batch);
      sent += acknowledged;
      if (acknowledged < batch.size()) {
        return sent;
      }
    }
  }

  private int send(List<OutboxEntry> batch) {
    KafkaTemplate<String, String> template = kafka.getObject();
    List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>();
    for (OutboxEntry entry : batch) {
      futures.add(template.send(record(entry)));
    }
    List<Long> acknowledged = new ArrayList<>();
    try {
      for (int i = 0; i < batch.size(); i++) {
        futures.get(i).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        acknowledged.add(batch.get(i).id());
      }
    } catch (ExecutionException | TimeoutException ex) {
      attemptFailed(batch.get(acknowledged.size()), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      if (!acknowledged.isEmpty()) {
        store.markSent(acknowledged, clock.instant());
      }
    }
    return acknowledged.size();
  }

  private ProducerRecord<String, String> record(OutboxEntry entry) {
    ProducerRecord<String, String> record =
        new ProducerRecord<>(entry.topic(), entry.key(), envelope(entry));
    record.headers().add("eventId", bytes(entry.eventId().toString()));
    record.headers().add("eventType", bytes(entry.type()));
    record.headers().add("correlationId", bytes(entry.correlationId()));
    return record;
  }

  /**
   * The JSON envelope of an outbox row.
   *
   * @param entry outbox row
   * @return envelope JSON
   */
  String envelope(OutboxEntry entry) {
    try {
      return mapper.writeValueAsString(
          new EventEnvelope(
              entry.eventId(),
              entry.type(),
              EventEnvelope.SCHEMA_VERSION,
              entry.occurredAt(),
              entry.companyCode(),
              entry.key(),
              entry.correlationId(),
              EventEnvelope.SOURCE,
              mapper.readTree(entry.payload())));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Outbox row " + entry.id() + " has an invalid payload", ex);
    }
  }

  private void attemptFailed(OutboxEntry entry, Exception ex) {
    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
    String error = cause.getClass().getSimpleName() + ": " + cause.getMessage();
    error = error.length() > MAX_ERROR ? error.substring(0, MAX_ERROR) : error;
    int attempts = entry.attempts() + 1;
    if (attempts >= properties.relayMaxAttempts()) {
      LOG.error("Outbox event {} FAILED after {} attempts: {}", entry.eventId(), attempts, error);
      store.markAttemptFailed(entry.id(), error, null);
      return;
    }
    Duration backoff =
        properties
            .relayRetryBackoff()
            .multipliedBy(1L << Math.min(attempts, MAX_BACKOFF_DOUBLINGS));
    Instant next = clock.instant().plus(backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff);
    LOG.warn(
        "Outbox event {} not acknowledged ({}); next attempt {}", entry.eventId(), error, next);
    store.markAttemptFailed(entry.id(), error, next);
  }

  private static byte[] bytes(String value) {
    return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }
}
