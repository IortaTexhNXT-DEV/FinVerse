package com.iortatechnxt.brokerverse.cache.service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Declaration of one cache, published as a Spring bean by the module that owns the cached data.
 *
 * <p>Every change (insert, update, delete, collection change) of an entity of {@link
 * #invalidatedBy} clears the whole cache, at once and again when the transaction ends, on every
 * instance when the cache is on Redis. Cached values must be immutable records, strings, numbers or
 * lists of them, never JPA entities.
 *
 * @param name cache name (lower-case, hyphenated: {@code <module>-<content>})
 * @param ttl time to live of an entry (overridable with {@code brokerverse.cache.ttl.<name>})
 * @param invalidatedBy entity types whose changes clear the cache
 * @param readOnlyTransactionsOnly true when the cache serves reads only outside read-write
 *     transactions: for values derived from many entities with many write paths, so a transaction
 *     that changes them always reads its own changes
 */
public record CacheSpec(
    String name, Duration ttl, Set<Class<?>> invalidatedBy, boolean readOnlyTransactionsOnly) {

  /** Validates and copies. */
  public CacheSpec {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(ttl, "ttl");
    invalidatedBy = Set.copyOf(invalidatedBy);
  }

  /**
   * A cache read in every transaction.
   *
   * @param name cache name
   * @param ttl time to live
   * @param invalidatedBy entity types whose changes clear the cache
   * @return spec
   */
  public static CacheSpec of(String name, Duration ttl, Class<?>... invalidatedBy) {
    return new CacheSpec(name, ttl, Set.of(invalidatedBy), false);
  }

  /**
   * The same cache, served only outside read-write transactions.
   *
   * @return spec
   */
  public CacheSpec servedOnlyOutsideWriteTransactions() {
    return new CacheSpec(name, ttl, invalidatedBy, true);
  }
}
