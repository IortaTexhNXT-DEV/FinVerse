package com.iortatechnxt.brokerverse.cache.service;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cache settings bound from {@code brokerverse.cache.*}.
 *
 * @param ttl time to live per cache name, overriding the {@link CacheSpec} default
 * @param maximumSize maximum entries per cache of the in-memory fallback (default 10,000)
 */
@ConfigurationProperties(prefix = "brokerverse.cache")
public record CacheProperties(Map<String, Duration> ttl, Long maximumSize) {

  /** Entries per cache of the in-memory fallback when nothing is configured. */
  public static final long DEFAULT_MAXIMUM_SIZE = 10_000;

  /** Applies the defaults. */
  public CacheProperties {
    ttl = ttl == null ? Map.of() : Map.copyOf(ttl);
    maximumSize = maximumSize == null ? Long.valueOf(DEFAULT_MAXIMUM_SIZE) : maximumSize;
  }

  /**
   * Effective time to live of a cache.
   *
   * @param spec declaration
   * @return configured override or the declared default
   */
  public Duration ttlOf(CacheSpec spec) {
    return ttl.getOrDefault(spec.name(), spec.ttl());
  }
}
