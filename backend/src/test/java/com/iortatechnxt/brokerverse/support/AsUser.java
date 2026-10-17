package com.iortatechnxt.brokerverse.support;

import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/** Runs code as a given SIT/UAT user (for tests that switch between maker and checker). */
@Component
public class AsUser {

  private final UserDetailsService users;

  AsUser(UserDetailsService users) {
    this.users = users;
  }

  public <T> T run(String username, Supplier<T> action) {
    var details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
