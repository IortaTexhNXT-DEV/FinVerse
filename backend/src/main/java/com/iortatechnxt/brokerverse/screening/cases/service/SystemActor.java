package com.iortatechnxt.brokerverse.screening.cases.service;

import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs automatic case work as the system (SNSRP-303): a case opened because a user registered a
 * client or submitted an account is not that user's case; the workflow records SYSTEM as its
 * originator, so the user is never told about the screening of the client (no tipping-off).
 */
final class SystemActor {

  private SystemActor() {}

  /**
   * Runs work without an authenticated user and restores the caller's security context.
   *
   * @param work the work
   * @param <T> result type
   * @return the result
   */
  static <T> T call(Supplier<T> work) {
    SecurityContext caller = SecurityContextHolder.getContext();
    SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
    try {
      return work.get();
    } finally {
      SecurityContextHolder.setContext(caller);
    }
  }
}
