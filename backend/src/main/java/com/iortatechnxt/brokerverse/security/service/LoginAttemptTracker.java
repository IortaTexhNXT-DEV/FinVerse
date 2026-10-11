package com.iortatechnxt.brokerverse.security.service;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Counts consecutive failed logins of a user on the shared counter, so simultaneous failures on
 * several instances are all counted. The database ({@code sec_user.failed_attempts}) stays the
 * record: a user whose recorded count is zero (after a success or an unlock) starts a fresh
 * counter, the count applied is never below the recorded count plus one, and when the counter store
 * is unreachable the database count alone applies.
 */
@Component
public class LoginAttemptTracker {

  private static final Logger LOG = LoggerFactory.getLogger(LoginAttemptTracker.class);

  private final SharedCounterStore counters;
  private final LoginProtectionProperties properties;

  /**
   * Creates the tracker.
   *
   * @param counters shared counters
   * @param properties login protection settings (counter window)
   */
  public LoginAttemptTracker(SharedCounterStore counters, LoginProtectionProperties properties) {
    this.counters = counters;
    this.properties = properties;
  }

  /**
   * Records a failed login.
   *
   * @param username user name
   * @param recordedFailures failures recorded on the user before this one
   * @return consecutive failures including this one
   */
  public int recordFailure(String username, int recordedFailures) {
    String key = key(username);
    long shared;
    try {
      if (recordedFailures == 0) {
        counters.reset(key);
      }
      shared = counters.increment(key, properties.failedAttemptWindow());
    } catch (RuntimeException ex) {
      LOG.error("Shared login counter unavailable; database count used for {}", username, ex);
      shared = 0;
    }
    return (int) Math.max(shared, recordedFailures + 1L);
  }

  /**
   * Clears the counter (successful login, unlock).
   *
   * @param username user name
   */
  public void reset(String username) {
    try {
      counters.reset(key(username));
    } catch (RuntimeException ex) {
      LOG.error("Shared login counter of {} could not be cleared", username, ex);
    }
  }

  private static String key(String username) {
    return "login-failures:" + username.toLowerCase(Locale.ROOT);
  }
}
