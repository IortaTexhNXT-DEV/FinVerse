package com.iortatechnxt.brokerverse.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated user name; {@value #SYSTEM} for background processing. */
@Component
public class CurrentUser {

  /** User name recorded for system generated activity (batch jobs, auto postings). */
  public static final String SYSTEM = "SYSTEM";

  /**
   * Returns the current user name.
   *
   * @return user name, or {@value #SYSTEM} when unauthenticated
   */
  public String username() {
    return optionalUsername().orElse(SYSTEM);
  }

  /**
   * Returns the current user name when a user is authenticated.
   *
   * @return optional user name
   */
  public Optional<String> optionalUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
      return Optional.empty();
    }
    return Optional.of(auth.getName());
  }

  /**
   * Whether the current user holds an authority (permission). Background processing holds none.
   *
   * @param authority permission name
   * @return true when granted
   */
  public boolean hasAuthority(String authority) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
  }

  /**
   * Whether two user names denote the same user (user names are case-insensitive).
   *
   * @param a user name, may be null
   * @param b user name, may be null
   * @return true when both are present and equal ignoring case
   */
  public static boolean sameUser(String a, String b) {
    return a != null && b != null && String.CASE_INSENSITIVE_ORDER.compare(a, b) == 0;
  }
}
