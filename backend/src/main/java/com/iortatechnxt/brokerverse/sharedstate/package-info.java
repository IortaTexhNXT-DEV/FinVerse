/**
 * Shared state across instances (platform): implementations of the cluster-wide job lock ({@code
 * system.service.JobLock}), the token denylist ({@code security.service.TokenRevocationStore}) and
 * the shared counters ({@code security.service.SharedCounterStore}), on Valkey 8 when {@code
 * brokerverse.redis.enabled} is true and on PostgreSQL (advisory locks, tables {@code
 * sec_revoked_token} and {@code sys_shared_counter}) otherwise.
 *
 * <p>See docs/architecture/PLATFORM_CACHE_AND_EVENTS.md.
 */
package com.iortatechnxt.brokerverse.sharedstate;
