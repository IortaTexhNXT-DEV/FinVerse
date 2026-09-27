package com.iortatechnxt.brokerverse.security.service.directory;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * The LOCAL adapter of {@link DirectoryAuthenticator} ({@code AUTH_MODE} = LOCAL, the delivered
 * mode): the BCrypt password held by BrokerVerse, checked by the Spring authentication manager.
 */
@Component
public class LocalPasswordAuthenticator implements DirectoryAuthenticator {

  /** Message of a refused password (never says which of user name or password is wrong). */
  public static final String INVALID = "Invalid user name or password";

  private final AuthenticationManager authenticationManager;

  /**
   * Creates the adapter.
   *
   * @param authenticationManager authentication manager (user details and password encoder)
   */
  public LocalPasswordAuthenticator(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public AuthMode mode() {
    return AuthMode.LOCAL;
  }

  @Override
  public DirectoryResult authenticate(String userId, char[] password) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(userId, new String(password)));
      return DirectoryResult.success();
    } catch (AuthenticationException ex) {
      return DirectoryResult.invalid(INVALID);
    }
  }
}
