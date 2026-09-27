package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rate limit of the login endpoint per client address, on the shared counters (fixed window of
 * {@code brokerverse.security.login-protection.rate-limit-window}). When the counter store is
 * unreachable the request is let through: the account lockout still protects the passwords.
 */
@Component
public class LoginRateLimiter {

  private static final Logger LOG = LoggerFactory.getLogger(LoginRateLimiter.class);

  private final SharedCounterStore counters;
  private final LoginProtectionProperties properties;

  /**
   * Creates the limiter.
   *
   * @param counters shared counters
   * @param properties limit and window
   */
  public LoginRateLimiter(SharedCounterStore counters, LoginProtectionProperties properties) {
    this.counters = counters;
    this.properties = properties;
  }

  /**
   * Counts one login request of a client.
   *
   * @param clientAddress client address
   * @return true when the request is within the limit
   */
  public boolean tryAcquire(String clientAddress) {
    try {
      return counters.increment("login-rate:" + clientAddress, properties.rateLimitWindow())
          <= properties.maxAttemptsPerWindow();
    } catch (RuntimeException ex) {
      LOG.error("Login rate limit counter unavailable; request allowed", ex);
      return true;
    }
  }

  /**
   * Window of the limit, for the {@code Retry-After} header.
   *
   * @return window
   */
  public Duration window() {
    return properties.rateLimitWindow();
  }
}
