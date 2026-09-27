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
 * @param jwtSecret HMAC secret (min 32 bytes); must come from the environment in production
 * @param tokenValidity access token lifetime
 * @param allowedOrigins CORS origins permitted to call the API
 * @param maxFailedAttempts consecutive failed logins that lock an account when the business
 *     parameter {@code LOGIN_MAX_FAILED_ATTEMPTS} is not set (default 5; BDOI NFR: 3 through the
 *     parameter)
 */
@Validated
@ConfigurationProperties(prefix = "brokerverse.security")
public record SecurityProperties(
    @NotBlank @Size(min = 32) String jwtSecret,
    @NotNull Duration tokenValidity,
    List<String> allowedOrigins,
    @Min(1) Integer maxFailedAttempts) {

  /** Consecutive failed logins that lock an account when nothing is configured. */
  public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;

  /** Applies the default lockout threshold. */
  public SecurityProperties {
    maxFailedAttempts = Objects.requireNonNullElse(maxFailedAttempts, DEFAULT_MAX_FAILED_ATTEMPTS);
  }
}
