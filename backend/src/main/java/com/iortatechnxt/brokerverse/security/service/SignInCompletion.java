package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.directory.LocalPasswordAuthenticator;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaPolicy;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The steps every sign-in shares after its first factor (the password, or the identity provider):
 *
 * <ol>
 *   <li>the second factor when the policy requires it or the user enrolled one ({@link MfaPolicy}):
 *       the answer asks for a code (VERIFY) or for the enrolment of an app (ENROL) with a challenge
 *       token valid {@value #CHALLENGE_MINUTES} minutes; a remembered device skips the code;
 *   <li>the session: the failed-attempt count is cleared, the sign-in is audited and recorded on
 *       the user, the session is opened with its tokens ({@link SignInSessions}).
 * </ol>
 *
 * <p>A refused sign-in always gets the same message ({@link LocalPasswordAuthenticator#INVALID});
 * the real reason is audited and logged. A wrong code counts towards the lockout like a wrong
 * password.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SignInCompletion {

  /** Minutes the second factor may be entered after the first step. */
  public static final int CHALLENGE_MINUTES = 5;

  private static final Logger LOG = LoggerFactory.getLogger(SignInCompletion.class);
  private static final String ENTITY = "AppUser";

  private final SignInSessions sessions;
  private final UserSessionLog sessionLog;
  private final MfaPolicy mfaPolicy;
  private final MfaService mfa;
  private final JwtTokenService tokens;
  private final LoginAttemptTracker attempts;
  private final SystemParameterService parameters;
  private final SecurityProperties properties;
  private final AuthPasswordPolicy passwordPolicy;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sessions token model
   * @param sessionLog session log (a lock ends the sessions)
   * @param mfaPolicy who needs the second factor
   * @param mfa second factor
   * @param tokens challenge tokens
   * @param attempts shared failed-login counter
   * @param parameters business parameters (lockout threshold)
   * @param properties security settings (default lockout threshold)
   * @param passwordPolicy password rules (a due change)
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the sign-in
  public SignInCompletion(
      SignInSessions sessions,
      UserSessionLog sessionLog,
      MfaPolicy mfaPolicy,
      MfaService mfa,
      JwtTokenService tokens,
      LoginAttemptTracker attempts,
      SystemParameterService parameters,
      SecurityProperties properties,
      AuthPasswordPolicy passwordPolicy,
      AuditTrailService audit,
      Clock clock) {
    this.sessions = sessions;
    this.sessionLog = sessionLog;
    this.mfaPolicy = mfaPolicy;
    this.mfa = mfa;
    this.tokens = tokens;
    this.attempts = attempts;
    this.parameters = parameters;
    this.properties = properties;
    this.passwordPolicy = passwordPolicy;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Continues a sign-in whose first factor was accepted: asks for the second factor or opens the
   * session.
   *
   * @param user the user (enabled and not locked)
   * @param method PASSWORD, OIDC or SAML
   * @param deviceToken token of a remembered device, may be null
   * @return the answer
   */
  public SignInResult afterFirstFactor(AppUser user, String method, String deviceToken) {
    boolean singleSignOn = !SignInMethod.PASSWORD.equals(method);
    boolean enrolled = mfa.enrolled(user.getUsername());
    boolean required = enrolled || mfaPolicy.required(user, passwordPolicy.mode(), singleSignOn);
    if (!required) {
      return complete(user, method, false, null);
    }
    if (enrolled && mfa.trustedDevice(user.getUsername(), deviceToken)) {
      return complete(user, method, true, null);
    }
    JwtTokenService.IssuedToken challenge =
        tokens.issueChallenge(
            user.getUsername(),
            JwtTokenService.MFA_CHALLENGE,
            method,
            Duration.ofMinutes(CHALLENGE_MINUTES));
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.LOGIN,
        "First sign-in step passed (" + method + "); second factor asked");
    return SignInResult.pending(
        LoginResponse.secondFactor(
            enrolled ? LoginResponse.MFA_VERIFY : LoginResponse.MFA_ENROL,
            challenge.token(),
            challenge.expiresAt(),
            mfaPolicy.rememberDays()));
  }

  /**
   * Opens the session of a user whose sign-in is complete.
   *
   * @param user the user
   * @param method PASSWORD, OIDC or SAML
   * @param secondFactor whether a second factor was checked
   * @param deviceToken token of a device remembered now, may be null
   * @return the answer with the tokens
   */
  public SignInResult complete(
      AppUser user, String method, boolean secondFactor, String deviceToken) {
    if (user.getFailedAttempts() > 0) {
      attempts.reset(user.getUsername());
    }
    user.recordSuccessfulLogin(clock.instant());
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.LOGIN,
        "Logged in" + suffix(method, secondFactor));
    SignInSessions.Tokens issued = sessions.open(user.getUsername(), method, secondFactor);
    String changeReason =
        SignInMethod.PASSWORD.equals(method)
            ? passwordPolicy.changeReason(user, clock.instant()).orElse(null)
            : null;
    LoginResponse response =
        LoginResponse.signedIn(
            issued.accessToken(),
            issued.sessionExpiresAt(),
            UserProfileResponse.from(user),
            changeReason,
            issued.accessExpiresAt(),
            deviceToken);
    return new SignInResult(response, issued.refreshToken(), issued.sessionExpiresAt());
  }

  /**
   * Counts a failed attempt towards the lockout (a wrong password or code), audits it and refuses
   * the sign-in with the uniform message.
   *
   * @param user the user
   * @param detail what failed, for the audit trail
   * @return never returns
   * @throws BadCredentialsException always
   */
  public BadCredentialsException countFailure(AppUser user, String detail) {
    user.recordFailedLogins(
        attempts.recordFailure(user.getUsername(), user.getFailedAttempts()),
        parameters.intValue(
            SystemParameterService.LOGIN_MAX_FAILED_ATTEMPTS, properties.maxFailedAttempts()));
    audit.recordIndependently(
        user.getUsername(),
        ENTITY,
        user.getUsername(),
        AuditAction.LOGIN_FAILED,
        detail + " (attempt " + user.getFailedAttempts() + ")");
    if (user.isLocked()) {
      sessionLog.endAll(user.getUsername(), SessionEndReason.LOCKED);
    }
    LOG.info("Sign-in of {} refused: {}", user.getUsername(), detail);
    throw new BadCredentialsException(LocalPasswordAuthenticator.INVALID);
  }

  /**
   * Refuses a sign-in with the uniform message; the reason goes to the audit trail and the log
   * only. The attempt is not counted.
   *
   * @param username user name entered
   * @param reason the real reason
   * @return never returns
   * @throws BadCredentialsException always
   */
  public BadCredentialsException refuse(String username, String reason) {
    audit.recordIndependently(username, ENTITY, username, AuditAction.LOGIN_FAILED, reason);
    LOG.info("Sign-in of {} refused: {}", username, reason);
    throw new BadCredentialsException(LocalPasswordAuthenticator.INVALID);
  }

  /**
   * Refuses a locked or deactivated account; the attempt is not counted.
   *
   * @param user the user
   * @param username user name entered
   */
  public void refuseLockedOrDeactivated(AppUser user, String username) {
    if (user.isLocked()) {
      refuse(username, "Account locked");
    }
    if (!user.isEnabled()) {
      refuse(username, "Account deactivated");
    }
  }

  private static String suffix(String method, boolean secondFactor) {
    String how =
        switch (method) {
          case SignInMethod.OIDC -> " (single sign-on, OpenID Connect)";
          case SignInMethod.SAML -> " (single sign-on, SAML)";
          default -> "";
        };
    return how + (secondFactor ? " with the second factor" : "");
  }

  /**
   * The mode in force, for the first step.
   *
   * @return mode
   */
  public AuthMode mode() {
    return passwordPolicy.mode();
  }
}
