package com.iortatechnxt.brokerverse.cache.service;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cache decorator for {@link CacheSpec#readOnlyTransactionsOnly()} caches: inside a read-write
 * transaction every read is a miss and nothing is stored, so a transaction that has just changed
 * the underlying entities (not yet flushed) always reads the database. Outside a transaction and in
 * read-only transactions the cache works normally.
 */
public class WriteTransactionBypassCache implements Cache {

  private final Cache delegate;

  /**
   * Wraps a cache.
   *
   * @param delegate the real cache
   */
  public WriteTransactionBypassCache(Cache delegate) {
    this.delegate = delegate;
  }

  /**
   * Whether the current thread runs a read-write transaction.
   *
   * @return true when the cache must be bypassed
   */
  static boolean inWriteTransaction() {
    return TransactionSynchronizationManager.isActualTransactionActive()
        && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Object getNativeCache() {
    return delegate.getNativeCache();
  }

  @Override
  public ValueWrapper get(Object key) {
    return inWriteTransaction() ? null : delegate.get(key);
  }

  @Override
  public <T> T get(Object key, Class<T> type) {
    return inWriteTransaction() ? null : delegate.get(key, type);
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    if (!inWriteTransaction()) {
      return delegate.get(key, valueLoader);
    }
    try {
      return valueLoader.call();
    } catch (Exception ex) {
      throw new ValueRetrievalException(key, valueLoader, ex);
    }
  }

  @Override
  public void put(Object key, Object value) {
    if (!inWriteTransaction()) {
      delegate.put(key, value);
    }
  }

  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    return inWriteTransaction() ? null : delegate.putIfAbsent(key, value);
  }

  @Override
  public void evict(Object key) {
    delegate.evict(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return delegate.evictIfPresent(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean invalidate() {
    return delegate.invalidate();
  }
}
