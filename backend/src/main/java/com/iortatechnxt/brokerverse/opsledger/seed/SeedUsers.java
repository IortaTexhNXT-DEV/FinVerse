package com.iortatechnxt.brokerverse.opsledger.seed;

import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Signs the Operations seed runners in as the SIT/UAT user who would do the work (seed profile
 * only), so every seed record carries a real user and passes the same permission and four-eyes
 * checks as the screens: cashier pays, remit extracts, remittl approves, disb assigns the DV, and
 * so on.
 */
@Component
@Profile("seed")
public class SeedUsers {

  private final UserDetailsService users;

  /**
   * Creates the helper.
   *
   * @param users user directory
   */
  public SeedUsers(UserDetailsService users) {
    this.users = users;
  }

  /**
   * Runs work as a SIT/UAT user and restores the previous sign-in afterwards.
   *
   * @param username SIT/UAT user
   * @param work work
   * @param <T> result type
   * @return result of the work
   */
  public <T> T as(String username, Supplier<T> work) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    UserDetails details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return work.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }

  /**
   * Runs work as a SIT/UAT user without a result.
   *
   * @param username SIT/UAT user
   * @param work work
   */
  public void run(String username, Runnable work) {
    as(
        username,
        () -> {
          work.run();
          return Boolean.TRUE;
        });
  }
}
