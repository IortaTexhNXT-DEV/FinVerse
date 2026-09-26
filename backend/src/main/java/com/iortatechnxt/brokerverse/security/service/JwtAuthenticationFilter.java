package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.SessionEndReason;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}.
 *
 * <p>The user is reloaded on every request so that disabling, locking or changing roles takes
 * effect immediately, without waiting for token expiry. A token revoked by logout ({@link
 * TokenRevocationStore}) authenticates nobody, nor does a token whose session was ended in the
 * session log (sign-out, administrator, lock, idle timeout or expiry; UAM-NFR-35). Each request
 * records the activity of its session ({@link UserSessionLog#check}, at most every five minutes),
 * and a token of a user found locked or disabled ends its session. Tokens issued before token ids
 * existed carry no {@code jti} and are accepted until they expire.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER = "Bearer ";

  private final JwtTokenService tokens;
  private final UserDetailsService userDetailsService;
  private final TokenRevocationStore revocations;
  private final UserSessionLog sessions;

  /**
   * Creates the filter.
   *
   * @param tokens token service
   * @param userDetailsService user loader
   * @param revocations token denylist
   * @param sessions session log
   */
  public JwtAuthenticationFilter(
      JwtTokenService tokens,
      UserDetailsService userDetailsService,
      TokenRevocationStore revocations,
      UserSessionLog sessions) {
    this.tokens = tokens;
    this.userDetailsService = userDetailsService;
    this.revocations = revocations;
    this.sessions = sessions;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      tokens
          .parse(header.substring(BEARER.length()))
          .filter(claims -> !revoked(claims))
          .filter(this::sessionUsable)
          .ifPresent(claims -> authenticate(claims, request));
    }
    chain.doFilter(request, response);
  }

  /**
   * Whether the token was revoked. When the denylist cannot be read the token is accepted (and the
   * failure logged), so an outage of the shared store does not sign every user out.
   */
  private boolean revoked(JwtTokenService.TokenClaims claims) {
    if (claims.tokenId() == null) {
      return false;
    }
    try {
      return revocations.isRevoked(claims.tokenId());
    } catch (RuntimeException ex) {
      LOG.error("Token denylist unavailable; token of {} accepted", claims.username(), ex);
      return false;
    }
  }

  /**
   * Whether the session of the token is still usable, recording its activity. When the session log
   * cannot be read the token is accepted (and the failure logged), as for the denylist.
   */
  private boolean sessionUsable(JwtTokenService.TokenClaims claims) {
    try {
      return sessions.check(claims.tokenId()) != UserSessionLog.SessionState.ENDED;
    } catch (RuntimeException ex) {
      LOG.error("Session log unavailable; token of {} accepted", claims.username(), ex);
      return true;
    }
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
      sessions.end(claims.tokenId(), reason);
    } catch (RuntimeException ex) {
      LOG.error("Session of {} not ended", claims.username(), ex);
    }
  }
}
