package com.iortatechnxt.brokerverse.sharedstate.service;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Session state on PostgreSQL when Redis is disabled: the token denylist in {@code
 * sec_revoked_token} and the shared counters in {@code sys_shared_counter} ({@link JdbcCounters}).
 * Expired rows are harmless (every read checks the expiry) and are deleted by the {@code
 * SHARED_STATE_CLEANUP} job.
 */
@Component
@ConditionalOnProperty(
    name = RedisSettings.ENABLED_PROPERTY,
    havingValue = "false",
    matchIfMissing = true)
public class JdbcSessionStore implements TokenRevocationStore, SharedCounterStore {

  private static final String REVOKE =
      """
      insert into sec_revoked_token (jti, username, expires_at, revoked_at)
      values (?, ?, ?, ?)
      on conflict (jti) do nothing
      """;
  private static final String IS_REVOKED =
      "select count(*) from sec_revoked_token where jti = ? and expires_at > ?";
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final JdbcCounters counters;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template (joins the current transaction)
   * @param clock clock
   * @param counters the counters in {@code sys_shared_counter}
   */
  public JdbcSessionStore(JdbcTemplate jdbc, Clock clock, JdbcCounters counters) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.counters = counters;
  }

  @Override
  public void revoke(String tokenId, String username, Instant expiresAt) {
    jdbc.update(REVOKE, tokenId, username, Timestamp.from(expiresAt), now());
  }

  @Override
  public boolean isRevoked(String tokenId) {
    Long count = jdbc.queryForObject(IS_REVOKED, Long.class, tokenId, now());
    return count != null && count > 0;
  }

  @Override
  public long increment(String key, Duration window) {
    return counters.increment(key, window);
  }

  @Override
  public long current(String key) {
    return counters.current(key);
  }

  @Override
  public void reset(String key) {
    counters.reset(key);
  }

  private Timestamp now() {
    return Timestamp.from(clock.instant());
  }
}
