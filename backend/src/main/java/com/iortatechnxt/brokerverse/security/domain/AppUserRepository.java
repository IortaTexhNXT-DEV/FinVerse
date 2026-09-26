package com.iortatechnxt.brokerverse.security.domain;

import java.time.Instant;
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
   * Whether a Windows ID (directory identity) is already used by a user.
   *
   * @param windowsId Windows ID
   * @return true when taken
   */
  boolean existsByWindowsIdIgnoreCase(String windowsId);

  /**
   * Whether a Windows ID is used by a user other than the given one.
   *
   * @param windowsId Windows ID
   * @param id the user that may keep it
   * @return true when another user has it
   */
  boolean existsByWindowsIdIgnoreCaseAndIdNot(String windowsId, Long id);

  /**
   * Enabled users holding a permission through any of their active roles.
   *
   * @param permission permission
   * @return user names
   */
  @Query(
      "select distinct u.username from AppUser u join u.roles r join r.permissions p"
          + " where p = :permission and r.active = true and u.enabled = true and u.locked = false")
  List<String> findUsernamesWithPermission(@Param("permission") Permission permission);

  /**
   * Finds a user by Windows ID (directory sign-in, AUTH_MODE = DIRECTORY; UAM-NFR-11).
   *
   * @param windowsId Windows ID
   * @return user if present
   */
  Optional<AppUser> findByWindowsIdIgnoreCase(String windowsId);

  /**
   * Enabled users whose password was last changed in a period (password expiry notice, UAM-NFR-36).
   *
   * @param from start of the period (inclusive)
   * @param to end of the period (exclusive)
   * @return users
   */
  List<AppUser> findByEnabledTrueAndPasswordChangedAtGreaterThanEqualAndPasswordChangedAtLessThan(
      Instant from, Instant to);
}
