package com.iortatechnxt.finverse.common.security;

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
}
