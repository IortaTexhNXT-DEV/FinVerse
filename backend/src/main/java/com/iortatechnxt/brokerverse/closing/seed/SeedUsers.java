package com.iortatechnxt.brokerverse.closing.seed;

import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Runs seed data steps as a named SIT/UAT user (maker and checker must differ). */
class SeedUsers {

  private final UserDetailsService users;

  SeedUsers(UserDetailsService users) {
    this.users = users;
  }

  /**
   * Runs an action as a user, restoring the previous authentication afterwards.
   *
   * @param username SIT/UAT user
   * @param action action
   * @param <T> result type
   * @return result
   */
  <T> T as(String username, Supplier<T> action) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    var details = users.loadUserByUsername(username);
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
