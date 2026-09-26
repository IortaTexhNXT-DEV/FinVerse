package com.iortatechnxt.brokerverse.security.service.directory;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chooses the {@link DirectoryAuthenticator} of the sign-in mode (FR-UA-003 R1). A mode without an
 * adapter (DIRECTORY until the EUA adapter exists, UQ04) refuses the sign-in with a service
 * message; nothing falls back to another mode.
 */
@Component
public class DirectoryAuthenticators {

  /** Message when the directory adapter is missing. */
  public static final String NOT_AVAILABLE =
      "Directory sign-in is not available. Contact your administrator.";

  private final List<DirectoryAuthenticator> adapters;

  /**
   * Creates the selector.
   *
   * @param adapters the registered adapters (LOCAL always)
   */
  public DirectoryAuthenticators(List<DirectoryAuthenticator> adapters) {
    this.adapters = List.copyOf(adapters);
  }

  /**
   * Checks a password with the adapter of a mode; the password array is cleared afterwards.
   *
   * @param mode sign-in mode
   * @param userId user name (LOCAL) or Windows ID (DIRECTORY)
   * @param password password
   * @return the outcome
   */
  public DirectoryResult authenticate(AuthMode mode, String userId, char[] password) {
    try {
      return adapters.stream()
          .filter(a -> a.mode() == mode)
          .findFirst()
          .map(a -> a.authenticate(userId, password))
          .orElseGet(() -> DirectoryResult.error(NOT_AVAILABLE));
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  /**
   * Whether a mode has an adapter.
   *
   * @param mode mode
   * @return true when available
   */
  public boolean available(AuthMode mode) {
    return adapters.stream().anyMatch(a -> a.mode() == mode);
  }
}
