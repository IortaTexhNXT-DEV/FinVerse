/**
 * Reference-data cache (platform): Spring Cache on Redis 7 when {@code brokerverse.redis.enabled}
 * is true, an in-memory Caffeine cache otherwise. Modules declare their caches with {@link
 * com.iortatechnxt.brokerverse.cache.service.CacheSpec} beans (name, time to live and the entity
 * types whose changes clear the cache) and read through {@code @Cacheable} methods.
 *
 * <p>See docs/architecture/PLATFORM_CACHE_AND_EVENTS.md.
 */
package com.iortatechnxt.brokerverse.cache;
