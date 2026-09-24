package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * Enabled users holding a permission through any of their roles.
   *
   * @param permission permission
   * @return user names
   */
  @Query(
      "select distinct u.username from AppUser u join u.roles r join r.permissions p"
          + " where p = :permission and u.enabled = true and u.locked = false")
  List<String> findUsernamesWithPermission(@Param("permission") Permission permission);
}
