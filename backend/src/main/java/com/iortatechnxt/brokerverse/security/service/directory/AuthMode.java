package com.iortatechnxt.brokerverse.security.service.directory;

import com.iortatechnxt.brokerverse.common.util.AsciiCase;
import java.util.Arrays;

/**
 * Sign-in mode, parameter {@code AUTH_MODE} (UAM-NFR-11, 17, 33; decision D6).
 *
 * <ul>
 *   <li>LOCAL checks the password held by BrokerVerse.
 *   <li>DIRECTORY checks it against the client's directory by Windows ID, through a {@link
 *       DirectoryAuthenticator} adapter that is installed once the client gives the interface.
 *   <li>OIDC and SAML sign the users in at the client's identity provider (OpenID Connect with the
 *       authorisation code and PKCE, or SAML 2.0), configured per deployment ({@code
 *       brokerverse.security.sso.*}); the identity is linked to an existing user only. Local
 *       passwords stay for the break-glass administrators ({@code
 *       brokerverse.security.sso.break-glass-users}).
 * </ul>
 */
public enum AuthMode {
  LOCAL,
  DIRECTORY,
  OIDC,
  SAML;

  /** Parameter holding the mode. */
  public static final String PARAMETER = "AUTH_MODE";

  /**
   * The mode of a parameter value; an unknown or missing value is LOCAL.
   *
   * @param value parameter value, may be null
   * @return mode
   */
  public static AuthMode of(String value) {
    if (value == null) {
      return LOCAL;
    }
    return Arrays.stream(values())
        .filter(m -> AsciiCase.equalsIgnoreCase(m.name(), value.trim()))
        .findFirst()
        .orElse(LOCAL);
  }

  /**
   * Whether the users sign in at an identity provider (single sign-on).
   *
   * @return true for OIDC and SAML
   */
  public boolean singleSignOn() {
    return this == OIDC || this == SAML;
  }
}
