package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaService;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The second step of a sign-in that needs the second factor, with the challenge token of the first
 * step: the code of the authenticator app (or a recovery code), or the enrolment of an app when the
 * user has none yet. A wrong code counts towards the lockout; a locked or deactivated user is
 * refused with the uniform message.
 */
@Service
@Transactional(
    propagation = Propagation.REQUIRES_NEW,
    noRollbackFor = AuthenticationException.class)
public class SecondFactorSignIn {

  private final JwtTokenService tokens;
  private final AppUserRepository users;
  private final MfaService mfa;
  private final SignInCompletion completion;

  /**
   * Creates the service.
   *
   * @param tokens challenge tokens
   * @param users users
   * @param mfa second factor
   * @param completion shared sign-in steps
   */
  public SecondFactorSignIn(
      JwtTokenService tokens,
      AppUserRepository users,
      MfaService mfa,
      SignInCompletion completion) {
    this.tokens = tokens;
    this.users = users;
    this.mfa = mfa;
    this.completion = completion;
  }

  /**
   * Checks the code of a sign-in and opens the session.
   *
   * @param challenge challenge token of the first step
   * @param code code of the app or a recovery code
   * @param rememberDevice whether to remember this device (only when the parameter allows it)
   * @return the answer with the tokens
   */
  public SignInResult verify(String challenge, String code, boolean rememberDevice) {
    Step step = step(challenge);
    if (!mfa.verify(step.user().getUsername(), code)) {
      throw completion.countFailure(step.user(), "Wrong second factor code");
    }
    String device =
        rememberDevice ? mfa.rememberDevice(step.user().getUsername()).orElse(null) : null;
    return completion.complete(step.user(), step.method(), true, device);
  }

  /**
   * Starts the enrolment of an authenticator app during a sign-in (the second factor is required
   * and none is enrolled).
   *
   * @param challenge challenge token of the first step
   * @return the secret to scan
   */
  public MfaService.Enrolment startEnrolment(String challenge) {
    Step step = step(challenge);
    requireNotEnrolled(step.user());
    return mfa.startEnrolment(step.user().getUsername());
  }

  /**
   * Confirms the enrolment with the first code of the app and opens the session.
   *
   * @param challenge challenge token of the first step
   * @param code first code of the app
   * @return the recovery codes (shown once) and the answer with the tokens
   */
  public Enrolled confirmEnrolment(String challenge, String code) {
    Step step = step(challenge);
    requireNotEnrolled(step.user());
    List<String> recoveryCodes = mfa.confirmEnrolment(step.user().getUsername(), code);
    return new Enrolled(recoveryCodes, completion.complete(step.user(), step.method(), true, null));
  }

  private void requireNotEnrolled(AppUser user) {
    if (mfa.enrolled(user.getUsername())) {
      throw completion.refuse(user.getUsername(), "Enrolment asked by a user already enrolled");
    }
  }

  private Step step(String challenge) {
    JwtTokenService.Challenge parsed =
        tokens
            .parseChallenge(challenge, JwtTokenService.MFA_CHALLENGE)
            .orElseThrow(() -> new BadCredentialsException(SignInSessions.SESSION_OVER));
    AppUser user =
        users
            .findByUsernameIgnoreCase(parsed.username())
            .orElseThrow(() -> completion.refuse(parsed.username(), "Unknown user"));
    completion.refuseLockedOrDeactivated(user, user.getUsername());
    String method = parsed.method() == null ? SignInMethod.PASSWORD : parsed.method();
    return new Step(user, method);
  }

  private record Step(AppUser user, String method) {}

  /**
   * The outcome of an enrolment at sign-in.
   *
   * @param recoveryCodes the ten recovery codes, shown once
   * @param signIn the answer with the tokens
   */
  public record Enrolled(List<String> recoveryCodes, SignInResult signIn) {}
}
