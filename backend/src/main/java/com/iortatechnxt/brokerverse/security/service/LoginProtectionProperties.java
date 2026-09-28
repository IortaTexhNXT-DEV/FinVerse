package com.iortatechnxt.brokerverse.security.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Login protection settings bound from {@code brokerverse.security.login-protection.*}.
 *
 * @param maxAttemptsPerWindow login requests accepted from one client address per window (default
 *     20); further requests get HTTP 429 until the window ends
 * @param rateLimitWindow window of the login rate limit (default 1 minute)
 * @param failedAttemptWindow life of the shared failed-login counter of a user (default 1 day); the
 *     database ({@code sec_user.failed_attempts}) stays the record
 * @param resetMaxPerAddress "Forgot password?" requests (request, check and confirm of a link)
 *     accepted from one client address per reset window (default 10)
 * @param resetMaxPerUser reset links e-mailed for one user per reset window (default 3); further
 *     requests get the same answer and no e-mail
 * @param resetWindow window of the "Forgot password?" limits (default 15 minutes)
 */
@ConfigurationProperties(prefix = "brokerverse.security.login-protection")
public record LoginProtectionProperties(
    Integer maxAttemptsPerWindow,
    Duration rateLimitWindow,
    Duration failedAttemptWindow,
    Integer resetMaxPerAddress,
    Integer resetMaxPerUser,
    Duration resetWindow) {

  /** Login requests per client address and window when nothing is configured. */
  public static final int DEFAULT_MAX_ATTEMPTS_PER_WINDOW = 20;

  /** "Forgot password?" requests per client address and window when nothing is configured. */
  public static final int DEFAULT_RESET_MAX_PER_ADDRESS = 10;

  /** Reset links per user and window when nothing is configured. */
  public static final int DEFAULT_RESET_MAX_PER_USER = 3;

  /** Applies the defaults. */
  @ConstructorBinding
  public LoginProtectionProperties {
    maxAttemptsPerWindow =
        maxAttemptsPerWindow == null
            ? Integer.valueOf(DEFAULT_MAX_ATTEMPTS_PER_WINDOW)
            : maxAttemptsPerWindow;
    rateLimitWindow = rateLimitWindow == null ? Duration.ofMinutes(1) : rateLimitWindow;
    failedAttemptWindow = failedAttemptWindow == null ? Duration.ofDays(1) : failedAttemptWindow;
    resetMaxPerAddress =
        resetMaxPerAddress == null
            ? Integer.valueOf(DEFAULT_RESET_MAX_PER_ADDRESS)
            : resetMaxPerAddress;
    resetMaxPerUser =
        resetMaxPerUser == null ? Integer.valueOf(DEFAULT_RESET_MAX_PER_USER) : resetMaxPerUser;
    resetWindow = resetWindow == null ? Duration.ofMinutes(15) : resetWindow;
  }

  /**
   * Settings of the login limit with the default "Forgot password?" limits.
   *
   * @param maxAttemptsPerWindow login requests per client address and window
   * @param rateLimitWindow window of the login rate limit
   * @param failedAttemptWindow life of the shared failed-login counter
   */
  public LoginProtectionProperties(
      Integer maxAttemptsPerWindow, Duration rateLimitWindow, Duration failedAttemptWindow) {
    this(maxAttemptsPerWindow, rateLimitWindow, failedAttemptWindow, null, null, null);
  }
}
