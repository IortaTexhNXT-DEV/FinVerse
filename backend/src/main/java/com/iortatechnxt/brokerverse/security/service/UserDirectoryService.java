package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.util.AsciiCase;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user directory shown on screens: every user's display name and main role, so tables, record
 * headers and histories show "Paula Unapplied Handler (Unapplied Handler)" instead of the login id.
 * It holds no access data beyond the role names already shown in the header.
 */
@Service
public class UserDirectoryService {

  private final AppUserRepository users;

  /**
   * Creates the service.
   *
   * @param users user repository
   */
  public UserDirectoryService(AppUserRepository users) {
    this.users = users;
  }

  /**
   * Lists the directory entries, ordered by login id.
   *
   * @param withRoles whether the role names of every user are given
   * @param self login id of the caller, whose own role name is always given
   * @return entries
   */
  @Transactional(readOnly = true)
  public List<Entry> entries(boolean withRoles, String self) {
    return users.findAll(Sort.by("username")).stream()
        .map(UserDirectoryService::entry)
        .map(e -> withRoles || AsciiCase.equalsIgnoreCase(e.username(), self) ? e : e.withoutRole())
        .toList();
  }

  private static Entry entry(AppUser user) {
    String displayName =
        user.getFullName() == null || user.getFullName().isBlank()
            ? user.getUsername()
            : user.getFullName();
    String role =
        user.getRoles().stream()
            .filter(Role::isActive)
            .min(Comparator.comparing(Role::getCode))
            .map(Role::getName)
            .orElse(null);
    return new Entry(user.getUsername(), displayName, role);
  }

  /**
   * A directory entry.
   *
   * @param username login id
   * @param displayName full name shown on screens
   * @param roleName name of the user's main active role, null when none
   */
  public record Entry(String username, String displayName, String roleName) {

    /**
     * The entry without its role name.
     *
     * @return entry
     */
    public Entry withoutRole() {
      return new Entry(username, displayName, null);
    }
  }
}
