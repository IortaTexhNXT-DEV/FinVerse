package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Rate limits of the anonymous sign-in endpoints on the shared counters (fixed windows): the login
 * per client address, the "Forgot password?" requests per client address and the reset links per
 * user. When the shared counter store (Valkey) is unreachable the database counters are used, and
 * when neither answers the request is refused: a limit is never skipped. Every outage is reported
 * ({@link SecurityStoreAlarm}).
 */
@Component
public class LoginRateLimiter {

  private final SharedCounterStore counters;
  private final FallbackCounterStore fallback;
  private final LoginProtectionProperties properties;
  private final SecurityStoreAlarm alarm;

  /**
   * Creates the limiter.
   *
   * @param counters shared counters
   * @param fallback database counters, used while the shared counters are unreachable
   * @param properties limits and windows
   * @param alarm reports an unreachable counter store
   */
  public LoginRateLimiter(
      SharedCounterStore counters,
      FallbackCounterStore fallback,
      LoginProtectionProperties properties,
      SecurityStoreAlarm alarm) {
    this.counters = counters;
    this.fallback = fallback;
    this.properties = properties;
    this.alarm = alarm;
  }

  /**
   * Counts one login request of a client.
   *
   * @param clientAddress client address
   * @return true when the request is within the limit
   */
  public boolean tryAcquire(String clientAddress) {
    return acquire(
        "login-rate:" + clientAddress,
        properties.maxAttemptsPerWindow(),
        properties.rateLimitWindow());
  }

  /**
   * Counts one "Forgot password?" request of a client.
   *
   * @param clientAddress client address
   * @return true when the request is within the limit
   */
  public boolean tryAcquireReset(String clientAddress) {
    return acquire(
        "reset-rate:" + clientAddress, properties.resetMaxPerAddress(), properties.resetWindow());
  }

  /**
   * Counts one reset link of a user.
   *
   * @param username user name
   * @return true when another link may be e-mailed
   */
  public boolean tryAcquireResetLink(String username) {
    return acquire(
        "reset-user:" + username.toLowerCase(Locale.ROOT),
        properties.resetMaxPerUser(),
        properties.resetWindow());
  }

  /**
   * Window of the login limit, for the {@code Retry-After} header.
   *
   * @return window
   */
  public Duration window() {
    return properties.rateLimitWindow();
  }

  /**
   * Window of the "Forgot password?" limits, for the {@code Retry-After} header.
   *
   * @return window
   */
  public Duration resetWindow() {
    return properties.resetWindow();
  }

  private boolean acquire(String key, int max, Duration window) {
    try {
      return counters.increment(key, window) <= max;
    } catch (RuntimeException ex) {
      alarm.raise("counters", "the rate limits use the database counters", ex);
    }
    try {
      return fallback.increment(key, window) <= max;
    } catch (RuntimeException ex) {
      alarm.raise("counters", "sign-in requests are refused until a counter store answers", ex);
      return false;
    }
  }
}
