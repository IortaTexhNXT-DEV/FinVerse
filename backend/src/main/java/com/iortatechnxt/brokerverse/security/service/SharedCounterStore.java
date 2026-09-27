package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;

/**
 * Port: fixed-window counters shared by every instance (failed logins, login rate limit). The
 * window starts with the first increment of a key and the counter restarts at 1 once it ends.
 *
 * <p>Implemented on Redis ({@code INCR} with {@code PEXPIRE}) or on the table {@code
 * sys_shared_counter} (module {@code sharedstate}).
 */
public interface SharedCounterStore {

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
