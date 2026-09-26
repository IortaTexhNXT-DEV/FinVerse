package com.iortatechnxt.brokerverse.events;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.runtime.CurrentRuntimeRole;
import com.iortatechnxt.brokerverse.events.service.EventsProperties;
import com.iortatechnxt.brokerverse.events.service.OutboxRelay;
import com.iortatechnxt.brokerverse.events.service.OutboxStore;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.env.MockEnvironment;

/** The after-commit outbox drain runs only on the roles with the integration workload. */
class OutboxRelayRoleTest {

  private final JobLock lock = mock(JobLock.class);

  @Test
  void aWebInstanceLeavesTheOutboxToTheIntegrationDeployment() {
    try (OutboxRelayHandle relay = relay("web")) {
      relay.relay().requestDrain();

      verify(lock, after(300).never()).tryAcquire(anyString());
    }
  }

  @Test
  void anIntegrationInstanceDrainsAfterCommit() {
    when(lock.tryAcquire(OutboxRelay.JOB_NAME)).thenReturn(Optional.empty());
    try (OutboxRelayHandle relay = relay("integration")) {
      relay.relay().requestDrain();

      verify(lock, timeout(5000).atLeastOnce()).tryAcquire(OutboxRelay.JOB_NAME);
    }
  }

  @SuppressWarnings("unchecked")
  private OutboxRelayHandle relay(String role) {
    EventsProperties properties =
        new EventsProperties(
            true, null, null, null, null, null, null, null, null, null, null, null);
    OutboxRelay relay =
        new OutboxRelay(
            mock(OutboxStore.class),
            properties,
            (ObjectProvider<KafkaTemplate<String, String>>) mock(ObjectProvider.class),
            lock,
            new ObjectMapper(),
            Clock.systemUTC(),
            new CurrentRuntimeRole(
                new MockEnvironment().withProperty("brokerverse.runtime.role", role)));
    return new OutboxRelayHandle(relay);
  }

  /** Stops the relay thread after the test. */
  private record OutboxRelayHandle(OutboxRelay relay) implements AutoCloseable {
    @Override
    public void close() {
      relay.destroy();
    }
  }
}
