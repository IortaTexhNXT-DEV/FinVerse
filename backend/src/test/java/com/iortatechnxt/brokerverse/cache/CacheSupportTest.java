package com.iortatechnxt.brokerverse.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.cache.service.CacheProperties;
import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.cache.service.ResilientCacheErrorHandler;
import com.iortatechnxt.brokerverse.cache.service.WriteTransactionBypassCache;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Cache declarations, the write-transaction bypass and the error handler. */
class CacheSupportTest {

  @AfterEach
  void clearTransactionState() {
    TransactionSynchronizationManager.setActualTransactionActive(false);
    TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
  }

  @Test
  void specsAndPropertiesApplyDefaultsAndOverrides() {
    CacheSpec spec = CacheSpec.of("x-values", Duration.ofMinutes(5), String.class);
    assertThat(spec.readOnlyTransactionsOnly()).isFalse();
    assertThat(spec.servedOnlyOutsideWriteTransactions().readOnlyTransactionsOnly()).isTrue();
    assertThat(new CacheProperties(null, null).ttlOf(spec)).isEqualTo(Duration.ofMinutes(5));
    assertThat(new CacheProperties(null, null).maximumSize())
        .isEqualTo(CacheProperties.DEFAULT_MAXIMUM_SIZE);
    assertThat(new CacheProperties(Map.of("x-values", Duration.ofSeconds(9)), 5L).ttlOf(spec))
        .isEqualTo(Duration.ofSeconds(9));
    assertThat(new RedisSettings(true, null).key("joblock", "JOB")).isEqualTo("bv:joblock:JOB");
    assertThat(new RedisSettings(true, "uat:").key("a")).isEqualTo("uat:a");
  }

  @Test
  void aReadWriteTransactionBypassesTheCache() throws Exception {
    Cache cache = new WriteTransactionBypassCache(new ConcurrentMapCache("c"));
    cache.put("k", "cached");
    assertThat(cache.get("k", String.class)).isEqualTo("cached");

    TransactionSynchronizationManager.setActualTransactionActive(true);
    assertThat(cache.get("k")).isNull();
    assertThat(cache.get("k", String.class)).isNull();
    assertThat(cache.get("k", () -> "fresh")).isEqualTo("fresh");
    cache.put("k", "uncommitted");
    assertThat(cache.putIfAbsent("n", "x")).isNull();

    TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
    assertThat(cache.get("k", String.class)).isEqualTo("cached");
    assertThat(cache.get("n")).isNull();
    cache.evict("k");
    assertThat(cache.evictIfPresent("k")).isFalse();
    cache.clear();
    assertThat(cache.invalidate()).isFalse();
    assertThat(cache.getName()).isEqualTo("c");
    assertThat(cache.getNativeCache()).isNotNull();

    TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    assertThatThrownBy(
            () ->
                cache.get(
                    "k",
                    () -> {
                      throw new IllegalStateException("db down");
                    }))
        .isInstanceOf(Cache.ValueRetrievalException.class);
  }

  @Test
  void cacheFailuresNeverReachTheCaller() {
    ResilientCacheErrorHandler handler = new ResilientCacheErrorHandler();
    Cache broken = mock(Cache.class);
    when(broken.getName()).thenReturn("broken");
    doThrow(new IllegalStateException("redis down")).when(broken).evictIfPresent(any());
    RuntimeException failure = new IllegalStateException("redis down");
    assertThatNoException()
        .isThrownBy(
            () -> {
              handler.handleCacheGetError(failure, broken, "k");
              handler.handleCachePutError(failure, broken, "k", "v");
              handler.handleCacheEvictError(failure, broken, "k");
              handler.handleCacheClearError(failure, broken);
            });
  }
}
