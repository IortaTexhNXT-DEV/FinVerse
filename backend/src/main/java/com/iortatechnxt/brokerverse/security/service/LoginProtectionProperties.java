package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Login protection settings bound from {@code brokerverse.security.login-protection.*}.
 *
 * @param maxAttemptsPerWindow login requests accepted from one client address per window (default
 *     20); further requests get HTTP 429 until the window ends
 * @param rateLimitWindow window of the login rate limit (default 1 minute)
 * @param failedAttemptWindow life of the shared failed-login counter of a user (default 1 day); the
 *     database ({@code sec_user.failed_attempts}) stays the record
 */
@ConfigurationProperties(prefix = "brokerverse.security.login-protection")
public record LoginProtectionProperties(
    Integer maxAttemptsPerWindow, Duration rateLimitWindow, Duration failedAttemptWindow) {

  /** Login requests per client address and window when nothing is configured. */
  public static final int DEFAULT_MAX_ATTEMPTS_PER_WINDOW = 20;

  /** Applies the defaults. */
  public LoginProtectionProperties {
    maxAttemptsPerWindow =
        maxAttemptsPerWindow == null
            ? Integer.valueOf(DEFAULT_MAX_ATTEMPTS_PER_WINDOW)
            : maxAttemptsPerWindow;
    rateLimitWindow = rateLimitWindow == null ? Duration.ofMinutes(1) : rateLimitWindow;
    failedAttemptWindow = failedAttemptWindow == null ? Duration.ofDays(1) : failedAttemptWindow;
  }
}
