package com.iortatechnxt.brokerverse.cache.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis switch and key namespace bound from {@code brokerverse.redis.*}; the connection itself is
 * the standard {@code spring.data.redis.*} (host, port, TLS, user name, password).
 *
 * @param enabled true to use Redis for caches, job locks, the token denylist and the shared
 *     counters; false for the in-memory cache and the database fallbacks
 * @param keyPrefix prefix of every key the application writes (default {@code bv:}), so several
 *     environments can share one Redis
 */
@ConfigurationProperties(prefix = "brokerverse.redis")
public record RedisSettings(boolean enabled, String keyPrefix) {

  /** Property that switches Redis on. */
  public static final String ENABLED_PROPERTY = "brokerverse.redis.enabled";

  /** Key prefix when nothing is configured. */
  public static final String DEFAULT_KEY_PREFIX = "bv:";

  /** Applies the defaults. */
  public RedisSettings {
    keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? DEFAULT_KEY_PREFIX : keyPrefix;
  }

  /**
   * A namespaced key.
   *
   * @param parts key parts, joined with {@code :}
   * @return prefixed key
   */
  public String key(String... parts) {
    return keyPrefix + String.join(":", parts);
  }
}
