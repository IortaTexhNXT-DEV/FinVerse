package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.security.service.JwtAuthenticationFilter;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.security.service.SecurityStoreAlarm;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import com.iortatechnxt.brokerverse.security.service.UserSessionLog;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link JwtAuthenticationFilter} of the user security chain from its collaborators (not
 * a filter bean itself, so the servlet container does not register it a second time).
 */
@Component
public class JwtFilterFactory {

  private final JwtTokenService tokens;
  private final UserDetailsService userDetailsService;
  private final TokenRevocationStore revocations;
  private final UserSessionLog sessions;
  private final SecurityStoreAlarm alarm;

  /**
   * Creates the factory.
   *
   * @param tokens token service
   * @param userDetailsService user loader
   * @param revocations token denylist (logout)
   * @param sessions session log (ended sessions are refused; activity is recorded)
   * @param alarm reports an unreachable denylist or session log
   */
  public JwtFilterFactory(
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

  /**
   * A new filter.
   *
   * @return filter
   */
  public JwtAuthenticationFilter create() {
    return new JwtAuthenticationFilter(tokens, userDetailsService, revocations, sessions, alarm);
  }
}
