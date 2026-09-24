package com.iortatechnxt.brokerverse.security.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
 * effect immediately, without waiting for token expiry.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";

  private final JwtTokenService tokens;
  private final UserDetailsService userDetailsService;

  /**
   * Creates the filter.
   *
   * @param tokens token service
   * @param userDetailsService user loader
   */
  public JwtAuthenticationFilter(JwtTokenService tokens, UserDetailsService userDetailsService) {
    this.tokens = tokens;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      tokens.validate(header.substring(BEARER.length())).ifPresent(u -> authenticate(u, request));
    }
    chain.doFilter(request, response);
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
