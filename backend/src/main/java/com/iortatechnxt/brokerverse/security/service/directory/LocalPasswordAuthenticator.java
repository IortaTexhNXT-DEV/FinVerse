package com.iortatechnxt.brokerverse.security.service.directory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * The LOCAL adapter of {@link DirectoryAuthenticator} ({@code AUTH_MODE} = LOCAL, the delivered
 * mode): the BCrypt password held by BrokerVerse, checked by the Spring authentication manager.
 *
 * <p>A check costs a few hundred milliseconds of processor, so at most as many run at once as the
 * server has processors: a burst of sign-ins waits its turn instead of taking the processor from
 * every other request. A check that cannot start within {@value #WAIT_SECONDS} seconds answers that
 * the sign-in service is busy.
 */
@Component
public class LocalPasswordAuthenticator implements DirectoryAuthenticator {

  /** Message of a refused password (never says which of user name or password is wrong). */
  public static final String INVALID = "Invalid user name or password";

  /** Message when no password check could start in time. */
  public static final String BUSY = "The sign-in service is busy; try again in a moment";

  /** Longest wait for a free check, in seconds. */
  static final long WAIT_SECONDS = 20;

  private final AuthenticationManager authenticationManager;
  private final Semaphore checks =
      new Semaphore(Math.max(1, Runtime.getRuntime().availableProcessors()), true);

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
      if (!checks.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS)) {
        return DirectoryResult.error(BUSY);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return DirectoryResult.error(BUSY);
    }
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(userId, new String(password)));
      return DirectoryResult.success();
    } catch (AuthenticationException ex) {
      return DirectoryResult.invalid(INVALID);
    } finally {
      checks.release();
    }
  }
}
