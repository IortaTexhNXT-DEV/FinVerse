package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.util.AsciiCase;
import com.iortatechnxt.brokerverse.security.service.SignInResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token cookie of the web client: HttpOnly (scripts never read it), SameSite=Strict
 * (never sent by another site), limited to {@value #PATH} and Secure outside a developer's machine
 * served over plain HTTP. It lives until the end of the sign-in session.
 */
@Component
public class RefreshCookies {

  /** Name of the cookie. */
  public static final String NAME = "BV_REFRESH";

  /** Path of the cookie (the sign-in endpoints only). */
  public static final String PATH = "/api/v1/auth";

  /** Header every refresh request must carry (a cross-site form cannot set it). */
  public static final String REQUEST_HEADER = "X-Requested-With";

  private final Clock clock;
  private final boolean local;

  /**
   * Creates the helper.
   *
   * @param clock clock
   * @param environment kind of environment ({@code local} allows a cookie over plain HTTP)
   */
  public RefreshCookies(
      Clock clock, @Value("${brokerverse.environment:local}") String environment) {
    this.clock = clock;
    this.local = AsciiCase.equalsIgnoreCase("local", environment == null ? "" : environment.trim());
  }

  /**
   * Sets the cookie of a new or renewed session.
   *
   * @param result the sign-in step
   * @param request request (secure or not)
   * @param response response
   */
  public void write(SignInResult result, HttpServletRequest request, HttpServletResponse response) {
    if (result.refreshToken() != null && result.sessionExpiresAt() != null) {
      write(result.refreshToken(), result.sessionExpiresAt(), request, response);
    }
  }

  /**
   * Sets the cookie.
   *
   * @param token refresh token
   * @param until end of the session
   * @param request request
   * @param response response
   */
  public void write(
      String token, Instant until, HttpServletRequest request, HttpServletResponse response) {
    Duration life = Duration.between(clock.instant(), until);
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        cookie(token, life.isNegative() ? Duration.ZERO : life, request).toString());
  }

  /**
   * Removes the cookie (sign-out).
   *
   * @param request request
   * @param response response
   */
  public void clear(HttpServletRequest request, HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, request).toString());
  }

  /**
   * The refresh token of a request.
   *
   * @param request request
   * @return token, empty without the cookie
   */
  public static Optional<String> read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    return cookies == null
        ? Optional.empty()
        : Arrays.stream(cookies)
            .filter(c -> NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .filter(v -> v != null && !v.isBlank())
            .findFirst();
  }

  private ResponseCookie cookie(String value, Duration life, HttpServletRequest request) {
    return ResponseCookie.from(NAME, value)
        .httpOnly(true)
        .secure(request.isSecure() || !local)
        .sameSite("Strict")
        .path(PATH)
        .maxAge(life)
        .build();
  }
}
