package com.iortatechnxt.brokerverse.cache.api.dto;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import java.time.Duration;
import java.util.List;

/**
 * One cache for the support API.
 *
 * @param name cache name
 * @param ttl time to live (ISO-8601 duration)
 * @param store REDIS or IN_MEMORY
 * @param readOnlyTransactionsOnly bypassed inside read-write transactions
 * @param invalidatedBy entity types whose changes clear it
 */
public record CacheResponse(
    String name,
    String ttl,
    String store,
    boolean readOnlyTransactionsOnly,
    List<String> invalidatedBy) {

  /**
   * Maps a declaration.
   *
   * @param spec declaration
   * @param ttl effective time to live
   * @param store store
   * @return response
   */
  public static CacheResponse from(CacheSpec spec, Duration ttl, String store) {
    return new CacheResponse(
        spec.name(),
        ttl.toString(),
        store,
        spec.readOnlyTransactionsOnly(),
        spec.invalidatedBy().stream().map(Class::getSimpleName).sorted().toList());
  }
}
