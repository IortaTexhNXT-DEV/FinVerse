package com.iortatechnxt.finverse.security.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.security.api.dto.LoginResponse;
import com.iortatechnxt.finverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.finverse.security.domain.AppUser;
import com.iortatechnxt.finverse.security.domain.AppUserRepository;
import java.time.Clock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login with lockout: five consecutive failures lock the account until an administrator unlocks it.
 * Every success and failure is written to the audit trail.
 */
@Service
public class AuthService {

  private static final String ENTITY = "AppUser";
  private static final String INVALID = "Invalid user name or password";

  private final AuthenticationManager authenticationManager;
  private final AppUserRepository users;
  private final JwtTokenService tokens;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param authenticationManager authentication manager
   * @param users user repository
   * @param tokens token service
   * @param audit audit trail
   * @param clock clock
   */
  public AuthService(
      AuthenticationManager authenticationManager,
      AppUserRepository users,
      JwtTokenService tokens,
      AuditTrailService audit,
      Clock clock) {
    this.authenticationManager = authenticationManager;
    this.users = users;
    this.tokens = tokens;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Authenticates a user and issues a token.
   *
   * @param username user name
   * @param password password
   * @return token and profile
   */
  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = AuthenticationException.class)
  public LoginResponse login(String username, String password) {
    AppUser user = users.findByUsernameIgnoreCase(username).orElse(null);
    if (user == null) {
      audit.recordIndependently(
          username, ENTITY, username, AuditAction.LOGIN_FAILED, "Unknown user");
      throw new BadCredentialsException(INVALID);
    }
    if (user.isLocked()) {
      audit.recordIndependently(
          username, ENTITY, username, AuditAction.LOGIN_FAILED, "Account locked");
      throw new LockedException("Account is locked. Contact your administrator.");
    }
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(user.getUsername(), password));
    } catch (AuthenticationException ex) {
      user.recordFailedLogin();
      audit.recordIndependently(
          username,
          ENTITY,
          username,
          AuditAction.LOGIN_FAILED,
          "Failed login attempt " + user.getFailedAttempts());
      throw new BadCredentialsException(INVALID, ex);
    }
    user.recordSuccessfulLogin(clock.instant());
    audit.recordIndependently(
        user.getUsername(), ENTITY, user.getUsername(), AuditAction.LOGIN, "Logged in");
    JwtTokenService.IssuedToken token = tokens.issue(user.getUsername());
    return new LoginResponse(token.token(), token.expiresAt(), UserProfileResponse.from(user));
  }
}
