package com.iortatechnxt.brokerverse.underwriting.demo;

import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Runs demo data creation as a named demo user (the maker "uw", the checker "fmanager"), so audit
 * columns, maker-checker checks and the audit trail record realistic users. Used only at start-up
 * of the demo profile; the previous security context is always restored.
 */
@Component
public class DemoUserContext {

  private final UserDetailsService users;

  /**
   * Creates the helper.
   *
   * @param users user directory
   */
  public DemoUserContext(UserDetailsService users) {
    this.users = users;
  }

  /**
   * Runs an action authenticated as a user.
   *
   * @param username demo user
   * @param action action
   * @param <T> result type
   * @return action result
   */
  public <T> T runAs(String username, Supplier<T> action) {
    UserDetails details = users.loadUserByUsername(username);
    SecurityContext previous = SecurityContextHolder.getContext();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    SecurityContextHolder.setContext(context);
    try {
      return action.get();
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }
}
