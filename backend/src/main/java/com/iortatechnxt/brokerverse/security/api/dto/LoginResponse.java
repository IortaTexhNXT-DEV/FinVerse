package com.iortatechnxt.brokerverse.security.api.dto;

import java.time.Instant;

/**
 * Sign-in result. Either the session is open ({@code accessToken} set; the refresh token travels in
 * an HttpOnly cookie, never in the body) or a second factor is asked for first ({@code mfaStep}
 * set, no token): VERIFY asks for the code of the authenticator app or a recovery code, ENROL for
 * the enrolment of an authenticator app (the second factor is required and none is enrolled yet).
 *
 * <p>When {@code mustChangePassword} is true the web client asks for a new password before it opens
 * the home page (UAM-NFR-36; FR-UA-005): the password was set by an administrator (RESET) or is
 * older than {@code PASSWORD_MAX_AGE_DAYS} (EXPIRED).
 *
 * @param accessToken bearer token, null while a second factor is asked for
 * @param expiresAt end of the sign-in session (absolute; the access token is renewed until then)
 * @param user profile of the signed-in user, null while a second factor is asked for
 * @param mustChangePassword whether the password must be changed first
 * @param passwordChangeReason RESET or EXPIRED when a change is due, otherwise null
 * @param accessTokenExpiresAt expiry of the access token (renew before)
 * @param mfaStep VERIFY or ENROL while a second factor is asked for, otherwise null
 * @param mfaChallenge proof of the password step, sent back with the code (valid 5 minutes)
 * @param deviceToken token of a device remembered for the second factor (only when the parameter
 *     MFA_REMEMBER_DEVICE_DAYS allows it and the user asked for it)
 * @param rememberDeviceDays days a device may be remembered (MFA_REMEMBER_DEVICE_DAYS), given with
 *     the VERIFY step; 0 = never
 */
public record LoginResponse(
    String accessToken,
    Instant expiresAt,
    UserProfileResponse user,
    boolean mustChangePassword,
    String passwordChangeReason,
    Instant accessTokenExpiresAt,
    String mfaStep,
    String mfaChallenge,
    String deviceToken,
    Integer rememberDeviceDays) {

  /** The password was set by someone else (creation, administrator reset). */
  public static final String RESET = "RESET";

  /** The password is older than the maximum age. */
  public static final String EXPIRED = "EXPIRED";

  /** A code of the enrolled authenticator app (or a recovery code) is asked for. */
  public static final String MFA_VERIFY = "VERIFY";

  /** The second factor is required and the user enrols an authenticator app first. */
  public static final String MFA_ENROL = "ENROL";

  /**
   * An open session; a change is due when a reason is given.
   *
   * @param accessToken bearer token
   * @param expiresAt end of the session
   * @param user profile
   * @param passwordChangeReason RESET, EXPIRED or null
   * @param accessTokenExpiresAt expiry of the access token
   * @param deviceToken token of a remembered device, may be null
   * @return response
   */
  public static LoginResponse signedIn(
      String accessToken,
      Instant expiresAt,
      UserProfileResponse user,
      String passwordChangeReason,
      Instant accessTokenExpiresAt,
      String deviceToken) {
    return new LoginResponse(
        accessToken,
        expiresAt,
        user,
        passwordChangeReason != null,
        passwordChangeReason,
        accessTokenExpiresAt,
        null,
        null,
        deviceToken,
        null);
  }

  /**
   * A second factor is asked for before the session opens.
   *
   * @param step VERIFY or ENROL
   * @param challenge proof of the password step
   * @param challengeExpiresAt end of the challenge
   * @param rememberDeviceDays days a device may be remembered, 0 = never
   * @return response
   */
  public static LoginResponse secondFactor(
      String step, String challenge, Instant challengeExpiresAt, int rememberDeviceDays) {
    return new LoginResponse(
        null,
        challengeExpiresAt,
        null,
        false,
        null,
        null,
        step,
        challenge,
        null,
        rememberDeviceDays);
  }
}
