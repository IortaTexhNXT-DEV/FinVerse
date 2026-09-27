package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only facts about users needed by business modules (roles, authorization limits). */
@Service
@Transactional(readOnly = true)
public class UserDirectory {

  private final AppUserRepository users;

  /**
   * Creates the service.
   *
   * @param users user repository
   */
  public UserDirectory(AppUserRepository users) {
    this.users = users;
  }

  /**
   * Returns the codes of the active roles of a user; a deactivated role counts as not held (BRD
   * 3.002.3).
   *
   * @param username user
   * @return role codes (empty for unknown users, e.g. SYSTEM)
   */
  public Set<String> roleCodes(String username) {
    return users
        .findByUsernameIgnoreCase(username)
        .map(
            u ->
                u.getRoles().stream()
                    .filter(Role::isActive)
                    .map(Role::getCode)
                    .collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  /**
   * Returns the authorization limit of a user (null means unlimited).
   *
   * @param username user
   * @return limit if configured
   */
  public Optional<BigDecimal> authorizationLimit(String username) {
    return users.findByUsernameIgnoreCase(username).map(AppUser::getAuthorizationLimit);
  }

  /**
   * Enabled users holding a permission (notification recipients, assignee pick lists).
   *
   * @param permission permission name
   * @return user names, sorted
   */
  public List<String> usersWithPermission(String permission) {
    return users.findUsernamesWithPermission(Permission.valueOf(permission)).stream()
        .sorted()
        .toList();
  }
}
