package com.iortatechnxt.brokerverse.security.service.directory;

import java.util.Locale;

/**
 * Sign-in mode, parameter {@code AUTH_MODE} (UAM-NFR-11, 17, 33; decision D6): LOCAL checks the
 * password held by BrokerVerse; DIRECTORY checks it against the BDO directory (EUA / AD) by Windows
 * ID, through a {@link DirectoryAuthenticator} adapter that is parked until BDO gives the interface
 * (UQ04).
 */
public enum AuthMode {
  LOCAL,
  DIRECTORY;

  /** Parameter holding the mode. */
  public static final String PARAMETER = "AUTH_MODE";

  /**
   * The mode of a parameter value; anything but DIRECTORY is LOCAL.
   *
   * @param value parameter value, may be null
   * @return mode
   */
  public static AuthMode of(String value) {
    return value != null && DIRECTORY.name().equals(value.trim().toUpperCase(Locale.ROOT))
        ? DIRECTORY
        : LOCAL;
  }
}
