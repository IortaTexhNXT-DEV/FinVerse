package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import com.iortatechnxt.brokerverse.sharedstate.service.JdbcSessionStore;
import com.iortatechnxt.brokerverse.sharedstate.service.SharedStateCleanupJob;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The database fallback of the session state (Redis disabled, as in the test profile). */
@IntegrationTest
class JdbcSessionStoreIT extends SessionStoreContract {

  @Autowired private JdbcSessionStore store;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private SharedStateCleanupJob cleanup;

  @Override
  SharedCounterStore counters() {
    return store;
  }

  @Override
  SharedCounterStore otherInstanceCounters() {
    return new JdbcSessionStore(jdbc, clock);
  }

  @Override
  TokenRevocationStore revocations() {
    return store;
  }

  @Test
  void theCleanupJobDeletesExpiredRows() throws InterruptedException {
    store.increment("cleanup-test", Duration.ofMillis(50));
    Thread.sleep(100);
    assertThat(cleanup.cron()).isNotBlank();
    assertThat(cleanup.description()).contains("revoked");
    assertThat(cleanup.execute(LocalDate.now(clock)).itemsProcessed()).isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sys_shared_counter where counter_key = 'cleanup-test'",
                Long.class))
        .isZero();
  }
}
