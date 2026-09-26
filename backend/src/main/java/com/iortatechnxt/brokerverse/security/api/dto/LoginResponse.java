package com.iortatechnxt.brokerverse.security.api.dto;

import java.time.Instant;

/**
 * Login result. When {@code mustChangePassword} is true the web client asks for a new password
 * before it opens the home page (UAM-NFR-36; FR-UA-005): the password was set by an administrator
 * (RESET) or is older than {@code PASSWORD_MAX_AGE_DAYS} (EXPIRED). Never set in DIRECTORY mode.
 *
 * @param accessToken bearer token
 * @param expiresAt expiry
 * @param user profile of the logged-in user
 * @param mustChangePassword whether the password must be changed first
 * @param passwordChangeReason RESET or EXPIRED when a change is due, otherwise null
 */
public record LoginResponse(
    String accessToken,
    Instant expiresAt,
    UserProfileResponse user,
    boolean mustChangePassword,
    String passwordChangeReason) {

  /** The password was set by someone else (creation, administrator reset). */
  public static final String RESET = "RESET";

  /** The password is older than the maximum age. */
  public static final String EXPIRED = "EXPIRED";

  /**
   * A login result; a change is due when a reason is given.
   *
   * @param accessToken bearer token
   * @param expiresAt expiry
   * @param user profile
   * @param passwordChangeReason RESET, EXPIRED or null
   */
  public LoginResponse(
      String accessToken,
      Instant expiresAt,
      UserProfileResponse user,
      String passwordChangeReason) {
    this(accessToken, expiresAt, user, passwordChangeReason != null, passwordChangeReason);
  }
}
