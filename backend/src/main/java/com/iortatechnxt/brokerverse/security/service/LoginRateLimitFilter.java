package com.iortatechnxt.brokerverse.security.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the {@link LoginRateLimiter} to {@code POST /api/v1/auth/login}: over the limit the
 * request is answered with HTTP 429, a {@code Retry-After} header and the error code {@code
 * LOGIN_RATE_LIMITED}, before any password check. The client address is the request's remote
 * address (behind the ingress, set {@code server.forward-headers-strategy} so it is the caller's).
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

  /** Path of the login endpoint. */
  public static final String LOGIN_PATH = "/api/v1/auth/login";

  private static final String BODY =
      "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
          + "\"detail\":\"Too many sign-in attempts. Wait a minute and try again.\","
          + "\"code\":\"LOGIN_RATE_LIMITED\"}";

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
    return !HttpMethod.POST.matches(request.getMethod())
        || !LOGIN_PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (limiter.tryAcquire(request.getRemoteAddr())) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limiter.window().toSeconds()));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
  }
}
