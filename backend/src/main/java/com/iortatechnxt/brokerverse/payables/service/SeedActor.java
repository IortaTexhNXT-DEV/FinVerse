package com.iortatechnxt.brokerverse.payables.service;

import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Runs seed data generation as named SIT/UAT users, so maker-checker controls and audit columns are
 * exercised exactly as in on-line use (maker "accountant", checker "checker").
 */
@Component
public class SeedActor {

  /** Seed maker. */
  public static final String MAKER = "accountant";

  /** Seed checker. */
  public static final String CHECKER = "checker";

  private final UserDetailsService users;

  /**
   * Creates the helper.
   *
   * @param users user details
   */
  public SeedActor(UserDetailsService users) {
    this.users = users;
  }

  /**
   * Runs an action as a user, restoring the previous authentication afterwards.
   *
   * @param username user
   * @param action action
   * @param <T> result type
   * @return result
   */
  public <T> T as(String username, Supplier<T> action) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    UserDetails details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
