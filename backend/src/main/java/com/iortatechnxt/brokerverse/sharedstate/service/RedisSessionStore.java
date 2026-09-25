package com.iortatechnxt.brokerverse.sharedstate.service;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Session state on Redis: the token denylist ({@code <prefix>session:revoked:<jti>} with a time to
 * live equal to the token's remaining life) and the shared counters ({@code <prefix>counter:<key>},
 * {@code INCR} and, on the first increment, {@code PEXPIRE} in one script so a counter never lives
 * without its window).
 */
@Component
@ConditionalOnProperty(name = RedisSettings.ENABLED_PROPERTY, havingValue = "true")
public class RedisSessionStore implements TokenRevocationStore, SharedCounterStore {

  private static final RedisScript<Long> INCREMENT =
      new DefaultRedisScript<>(
          "local v = redis.call('incr', KEYS[1]) "
              + "if v == 1 then redis.call('pexpire', KEYS[1], ARGV[1]) end return v",
          Long.class);

  private final StringRedisTemplate redis;
  private final RedisSettings settings;
  private final Clock clock;

  /**
   * Creates the store.
   *
   * @param redis Redis template
   * @param settings key prefix
   * @param clock clock (remaining token life)
   */
  public RedisSessionStore(StringRedisTemplate redis, RedisSettings settings, Clock clock) {
    this.redis = redis;
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public void revoke(String tokenId, String username, Instant expiresAt) {
    Duration remaining = Duration.between(clock.instant(), expiresAt);
    if (!remaining.isNegative() && !remaining.isZero()) {
      redis.opsForValue().set(revokedKey(tokenId), username, remaining);
    }
  }

  @Override
  public boolean isRevoked(String tokenId) {
    return Boolean.TRUE.equals(redis.hasKey(revokedKey(tokenId)));
  }

  @Override
  public long increment(String key, Duration window) {
    Long value =
        redis.execute(INCREMENT, List.of(counterKey(key)), String.valueOf(window.toMillis()));
    return value == null ? 0 : value;
  }

  @Override
  public long current(String key) {
    String value = redis.opsForValue().get(counterKey(key));
    return value == null ? 0 : Long.parseLong(value);
  }

  @Override
  public void reset(String key) {
    redis.delete(counterKey(key));
  }

  private String revokedKey(String tokenId) {
    return settings.key("session", "revoked", tokenId);
  }

  private String counterKey(String key) {
    return settings.key("counter", key);
  }
}
