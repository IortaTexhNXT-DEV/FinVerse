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
 * sec_revoked_token} and the shared counters in {@code sys_shared_counter} (an atomic upsert that
 * restarts an expired window). Expired rows are harmless (every read checks the expiry) and are
 * deleted by the {@code SHARED_STATE_CLEANUP} job.
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
  private static final String INCREMENT =
      """
      insert into sys_shared_counter as c (counter_key, counter_value, expires_at)
      values (?, 1, ?)
      on conflict (counter_key) do update set
        counter_value = case when c.expires_at <= ? then 1 else c.counter_value + 1 end,
        expires_at = case when c.expires_at <= ? then excluded.expires_at else c.expires_at end
      returning counter_value
      """;
  private static final String CURRENT =
      "select counter_value from sys_shared_counter where counter_key = ? and expires_at > ?";
  private static final String RESET = "delete from sys_shared_counter where counter_key = ?";

  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template (joins the current transaction)
   * @param clock clock
   */
  public JdbcSessionStore(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
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
    Timestamp now = now();
    Timestamp expiry = Timestamp.from(clock.instant().plus(window));
    Long value = jdbc.queryForObject(INCREMENT, Long.class, key, expiry, now, now);
    return value == null ? 0 : value;
  }

  @Override
  public long current(String key) {
    return jdbc.queryForList(CURRENT, Long.class, key, now()).stream().findFirst().orElse(0L);
  }

  @Override
  public void reset(String key) {
    jdbc.update(RESET, key);
  }

  private Timestamp now() {
    return Timestamp.from(clock.instant());
  }
}
