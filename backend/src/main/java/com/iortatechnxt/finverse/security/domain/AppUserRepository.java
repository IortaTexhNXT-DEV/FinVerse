package com.iortatechnxt.finverse.security.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AppUser}. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  /**
   * Finds a user by login name (case-insensitive).
   *
   * @param username login name
   * @return user if present
   */
  Optional<AppUser> findByUsernameIgnoreCase(String username);

  /**
   * Checks whether a login name is taken.
   *
   * @param username login name
   * @return true when taken
   */
  boolean existsByUsernameIgnoreCase(String username);
}
