package com.iortatechnxt.brokerverse.migration.common.service;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs work as the system user {@code mig-loader} (DATA_MIGRATION_DESIGN section 10): the records
 * created by a load carry it as their creator; the batch records who approved the load.
 */
public final class LoaderIdentity {

  private LoaderIdentity() {}

  /**
   * Runs work as the loader and restores the previous sign-in afterwards.
   *
   * @param work work
   */
  public static void run(Runnable work) {
    call(
        () -> {
          work.run();
          return Boolean.TRUE;
        });
  }

  /**
   * Runs work as the loader and returns its result.
   *
   * @param work work
   * @param <T> result type
   * @return result
   */
  public static <T> T call(Supplier<T> work) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(MigrationCodes.LOADER_USER, null, List.of()));
    try {
      return work.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
