package com.iortatechnxt.brokerverse.identity.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The connection to the Enterprise SSO platform / UIDM-ISC, bound from {@code
 * brokerverse.identity.*}. The live connection is set at BDOI SIT; until then the simulator serves
 * the flows (SIT and UAT only; refused in production).
 *
 * @param simulator whether the Enterprise SSO simulator replaces the platform (seed profile)
 * @param directoryUrl base address of the platform's user directory (SCIM 2.0), blank when not
 *     connected
 * @param directoryToken bearer token of the directory, from the secret store ({@code
 *     BROKERVERSE_IDENTITY_DIRECTORY_TOKEN})
 * @param timeout connect and read timeout of the directory (default 10 seconds)
 */
@ConfigurationProperties(prefix = "brokerverse.identity")
public record IdentityProperties(
    boolean simulator, String directoryUrl, String directoryToken, Duration timeout) {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  /** Defaults. */
  public IdentityProperties {
    timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
  }

  /**
   * Whether a live directory is configured.
   *
   * @return true when the address is set
   */
  public boolean directoryConfigured() {
    return directoryUrl != null && !directoryUrl.isBlank();
  }
}
