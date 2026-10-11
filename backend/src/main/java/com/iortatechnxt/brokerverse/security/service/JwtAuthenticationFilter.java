package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>} (an access token of {@link
 * JwtTokenService}).
 *
 * <p>The user is reloaded on every request so that disabling, locking or changing roles takes
 * effect immediately. A token authenticates nobody when it was revoked by logout ({@link
 * TokenRevocationStore}), when its sign-in session is ended or unknown in the session log
 * (sign-out, administrator, lock, idle timeout, expiry, reused refresh token; UAM-NFR-35), or when
 * it has no session. Each request records the activity of its session ({@link
 * UserSessionLog#check}, at most every five minutes), and a token of a user found locked or
 * disabled ends its session.
 *
 * <p>The checks fail closed: when the denylist cannot be read, the session log (database) decides
 * alone, since every sign-out also ends the session there; when the session log cannot be read the
 * request is refused with HTTP 503 ({@code SIGN_IN_CHECK_UNAVAILABLE}, the web client retries
 * without signing the user out). Every outage is reported ({@link SecurityStoreAlarm}).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER = "Bearer ";
  private static final String UNAVAILABLE_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,"
          + "\"detail\":\"The sign-in could not be checked. Try again in a moment.\","
          + "\"code\":\"SIGN_IN_CHECK_UNAVAILABLE\"}";

  private final JwtTokenService tokens;
  private final UserDetailsService userDetailsService;
  private final TokenRevocationStore revocations;
  private final UserSessionLog sessions;
  private final SecurityStoreAlarm alarm;

  /**
   * Creates the filter.
   *
   * @param tokens token service
   * @param userDetailsService user loader
   * @param revocations token denylist
   * @param sessions session log
   * @param alarm reports an unreachable store
   */
  public JwtAuthenticationFilter(
      JwtTokenService tokens,
      UserDetailsService userDetailsService,
      TokenRevocationStore revocations,
      UserSessionLog sessions,
      SecurityStoreAlarm alarm) {
    this.tokens = tokens;
    this.userDetailsService = userDetailsService;
    this.revocations = revocations;
    this.sessions = sessions;
    this.alarm = alarm;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    Optional<JwtTokenService.TokenClaims> claims =
        header != null && header.startsWith(BEARER)
            ? tokens.parse(header.substring(BEARER.length()))
            : Optional.empty();
    if (claims.isPresent() && !revoked(claims.get())) {
      Optional<UserSessionLog.SessionState> state = sessionState(claims.get());
      if (state.isEmpty()) {
        unavailable(response);
        return;
      }
      if (state.get() == UserSessionLog.SessionState.OPEN) {
        authenticate(claims.get(), request);
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Whether the token was revoked. When the denylist cannot be read the session log decides (a
   * sign-out always ends the session there as well).
   */
  private boolean revoked(JwtTokenService.TokenClaims claims) {
    try {
      return revocations.isRevoked(claims.tokenId());
    } catch (RuntimeException ex) {
      alarm.raise("denylist", "access tokens are checked against the session log only", ex);
      return false;
    }
  }

  /**
   * The state of the session of the token, recording its activity; empty when the session log
   * cannot be read (the request is then refused).
   */
  private Optional<UserSessionLog.SessionState> sessionState(JwtTokenService.TokenClaims claims) {
    try {
      return Optional.of(sessions.check(claims.sessionId()));
    } catch (RuntimeException ex) {
      alarm.raise("sessions", "requests with an access token are refused (HTTP 503)", ex);
      return Optional.empty();
    }
  }

  private static void unavailable(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, "5");
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getOutputStream().write(UNAVAILABLE_BODY.getBytes(StandardCharsets.UTF_8));
  }

  private void authenticate(JwtTokenService.TokenClaims claims, HttpServletRequest request) {
    try {
      UserDetails user = userDetailsService.loadUserByUsername(claims.username());
      if (user.isEnabled() && user.isAccountNonLocked()) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } else {
        endSession(
            claims,
            user.isAccountNonLocked() ? SessionEndReason.ADMIN_ENDED : SessionEndReason.LOCKED);
      }
    } catch (UsernameNotFoundException ex) {
      SecurityContextHolder.clearContext();
    }
  }

  private void endSession(JwtTokenService.TokenClaims claims, SessionEndReason reason) {
    try {
      sessions.end(claims.sessionId(), reason);
    } catch (RuntimeException ex) {
      LOG.error("Session of {} not ended", claims.username(), ex);
    }
  }
}
