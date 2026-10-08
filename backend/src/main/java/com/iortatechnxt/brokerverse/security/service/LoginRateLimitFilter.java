package com.iortatechnxt.brokerverse.security.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the {@link LoginRateLimiter} to the anonymous sign-in endpoints before any password or
 * link check: {@code POST /api/v1/auth/login} and the other sign-in steps of {@link #SIGN_IN_PATHS}
 * (error code {@code LOGIN_RATE_LIMITED}) and {@code POST /api/v1/auth/password-reset/**} (error
 * code {@code RESET_RATE_LIMITED}). Over the limit the request is answered with HTTP 429 and a
 * {@code Retry-After} header. The client address is the request's remote address (behind the
 * gateway, set {@code server.forward-headers-strategy} so it is the caller's).
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

  /** Path of the login endpoint. */
  public static final String LOGIN_PATH = "/api/v1/auth/login";

  /** Prefix of the "Forgot password?" endpoints. */
  public static final String RESET_PREFIX = "/api/v1/auth/password-reset/";

  /**
   * The anonymous sign-in steps counted in the login limit: the password, the second factor and the
   * completion of a single sign-on.
   */
  public static final Set<String> SIGN_IN_PATHS =
      Set.of(
          LOGIN_PATH,
          "/api/v1/auth/mfa/verify",
          "/api/v1/auth/mfa/enrolment/start",
          "/api/v1/auth/mfa/enrolment/confirm",
          "/api/v1/auth/sso/complete");

  private static final String LOGIN_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
          + "\"detail\":\"Too many sign-in attempts. Wait a minute and try again.\","
          + "\"code\":\"LOGIN_RATE_LIMITED\"}";
  private static final String RESET_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
          + "\"detail\":\"Too many password reset requests. Wait a few minutes and try again.\","
          + "\"code\":\"RESET_RATE_LIMITED\"}";

  private final LoginRateLimiter limiter;

  /**
   * Creates the filter.
   *
   * @param limiter rate limiter
   */
  public LoginRateLimitFilter(LoginRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !HttpMethod.POST.matches(request.getMethod())
        || !(SIGN_IN_PATHS.contains(path) || path.startsWith(RESET_PREFIX));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    boolean login = SIGN_IN_PATHS.contains(request.getRequestURI());
    String address = request.getRemoteAddr();
    if (login ? limiter.tryAcquire(address) : limiter.tryAcquireReset(address)) {
      chain.doFilter(request, response);
      return;
    }
    Duration window = login ? limiter.window() : limiter.resetWindow();
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(window.toSeconds()));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response
        .getOutputStream()
        .write((login ? LOGIN_BODY : RESET_BODY).getBytes(StandardCharsets.UTF_8));
  }
}
