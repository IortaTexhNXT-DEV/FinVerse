package com.iortatechnxt.brokerverse.security.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Security settings bound from {@code brokerverse.security.*}.
 *
 * @param jwtSecret HMAC secret (min 32 bytes); must come from the environment outside local
 * @param tokenValidity maximum life of a sign-in session (absolute; default 8 hours): the refresh
 *     of the access token stops at this time and the user signs in again
 * @param allowedOrigins CORS origins permitted to call the API
 * @param maxFailedAttempts consecutive failed logins that lock an account when the business
 *     parameter {@code LOGIN_MAX_FAILED_ATTEMPTS} is not set (default 5)
 * @param accessTokenValidity life of an access token when the parameter {@code
 *     ACCESS_TOKEN_MINUTES} is not set (default 15 minutes)
 * @param refreshGrace time during which the refresh token just replaced is still accepted, for a
 *     second browser tab renewing at the same moment (default 30 seconds); presented later it ends
 *     the session
 */
@Validated
@ConfigurationProperties(prefix = "brokerverse.security")
public record SecurityProperties(
    @NotBlank @Size(min = 32) String jwtSecret,
    @NotNull Duration tokenValidity,
    List<String> allowedOrigins,
    @Min(1) Integer maxFailedAttempts,
    Duration accessTokenValidity,
    Duration refreshGrace) {

  /** Consecutive failed logins that lock an account when nothing is configured. */
  public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;

  /** Life of an access token when nothing is configured. */
  public static final Duration DEFAULT_ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);

  /** Applies the defaults. */
  public SecurityProperties {
    maxFailedAttempts = Objects.requireNonNullElse(maxFailedAttempts, DEFAULT_MAX_FAILED_ATTEMPTS);
    accessTokenValidity =
        Objects.requireNonNullElse(accessTokenValidity, DEFAULT_ACCESS_TOKEN_VALIDITY);
    refreshGrace = Objects.requireNonNullElse(refreshGrace, Duration.ofSeconds(30));
  }

  /**
   * Settings with the default access token life and refresh grace.
   *
   * @param jwtSecret HMAC secret
   * @param tokenValidity maximum life of a sign-in session
   * @param allowedOrigins CORS origins
   * @param maxFailedAttempts lockout threshold when the parameter is not set
   */
  public SecurityProperties(
      String jwtSecret,
      Duration tokenValidity,
      List<String> allowedOrigins,
      Integer maxFailedAttempts) {
    this(jwtSecret, tokenValidity, allowedOrigins, maxFailedAttempts, null, null);
  }
}
