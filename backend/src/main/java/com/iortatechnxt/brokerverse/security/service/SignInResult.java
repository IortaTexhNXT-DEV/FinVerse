package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import java.time.Instant;

/**
 * The outcome of a sign-in step: the answer for the web client and, when a session opened, its
 * refresh token for the HttpOnly cookie (never in the body).
 *
 * @param response the answer
 * @param refreshToken refresh token of the new session, null when no session opened
 * @param sessionExpiresAt end of the session (life of the cookie), null when no session opened
 */
public record SignInResult(LoginResponse response, String refreshToken, Instant sessionExpiresAt) {

  /**
   * A step that opened no session (a second factor is asked for).
   *
   * @param response the answer
   * @return result
   */
  public static SignInResult pending(LoginResponse response) {
    return new SignInResult(response, null, null);
  }
}
