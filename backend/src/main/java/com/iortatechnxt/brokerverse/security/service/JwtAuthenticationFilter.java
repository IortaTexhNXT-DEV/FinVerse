package com.iortatechnxt.brokerverse.security.service;

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
 * TokenRevocationStore}) authenticates nobody. Tokens issued before token ids existed carry no
 * {@code jti} and are accepted until they expire.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER = "Bearer ";

  private final JwtTokenService tokens;
  private final UserDetailsService userDetailsService;
  private final TokenRevocationStore revocations;

  /**
   * Creates the filter.
   *
   * @param tokens token service
   * @param userDetailsService user loader
   * @param revocations token denylist
   */
  public JwtAuthenticationFilter(
      JwtTokenService tokens,
      UserDetailsService userDetailsService,
      TokenRevocationStore revocations) {
    this.tokens = tokens;
    this.userDetailsService = userDetailsService;
    this.revocations = revocations;
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
          .ifPresent(claims -> authenticate(claims.username(), request));
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

  private void authenticate(String username, HttpServletRequest request) {
    try {
      UserDetails user = userDetailsService.loadUserByUsername(username);
      if (user.isEnabled() && user.isAccountNonLocked()) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    } catch (UsernameNotFoundException ex) {
      SecurityContextHolder.clearContext();
    }
  }
}
