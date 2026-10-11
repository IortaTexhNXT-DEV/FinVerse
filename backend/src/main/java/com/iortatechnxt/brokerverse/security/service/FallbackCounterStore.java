package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;

/**
 * Port: the fixed-window counters kept in PostgreSQL ({@code sys_shared_counter}), used by the rate
 * limits when the shared counter store ({@link SharedCounterStore}, Valkey) cannot be reached, so a
 * limit is never skipped. Implemented in the module {@code sharedstate}.
 */
public interface FallbackCounterStore {

  /**
   * Adds one to a counter.
   *
   * @param key counter key
   * @param window life of the counter from its first increment
   * @return the new value (1 for a new or expired counter)
   */
  long increment(String key, Duration window);

  /**
   * Current value of a counter.
   *
   * @param key counter key
   * @return value, 0 when missing or expired
   */
  long current(String key);

  /**
   * Removes a counter.
   *
   * @param key counter key
   */
  void reset(String key);
}
