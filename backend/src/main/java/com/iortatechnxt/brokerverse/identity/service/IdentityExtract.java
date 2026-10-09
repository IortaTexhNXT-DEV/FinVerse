package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryProfile;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryProfileRepository;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The extract of the accounts, group profiles and permissions for the access certification of
 * UIDM-ISC (BDOI FRS FRUM.002.02).
 */
@Service
@Transactional(readOnly = true)
public class IdentityExtract {

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final DirectoryProfileRepository profiles;

  /**
   * Creates the extract.
   *
   * @param users users
   * @param roles roles
   * @param profiles directory details
   */
  public IdentityExtract(
      AppUserRepository users, RoleRepository roles, DirectoryProfileRepository profiles) {
    this.users = users;
    this.roles = roles;
    this.profiles = profiles;
  }

  /**
   * Every user with a Windows ID, as an account with its group profiles.
   *
   * @return accounts, by user ID
   */
  public List<Account> accounts() {
    Map<String, DirectoryProfile> byUser =
        profiles.findAll().stream()
            .collect(
                Collectors.toMap(
                    p -> p.getUsername().toLowerCase(Locale.ROOT),
                    Function.identity(),
                    (a, b) -> a));
    return users.findAll().stream()
        .filter(u -> u.getWindowsId() != null && !u.getWindowsId().isBlank())
        .sorted(Comparator.comparing(AppUser::getUsername))
        .map(u -> account(u, byUser.get(u.getUsername().toLowerCase(Locale.ROOT))))
        .toList();
  }

  /**
   * The account of a user ID (the SCIM resource id) or of a Windows ID.
   *
   * @param key user ID or Windows ID
   * @return the account, empty when no user has it
   */
  public Optional<Account> account(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return users
        .findByUsernameIgnoreCase(key.trim())
        .or(() -> users.findByWindowsIdIgnoreCase(key.trim()))
        .filter(u -> u.getWindowsId() != null)
        .map(u -> account(u, profiles.findByUsernameIgnoreCase(u.getUsername()).orElse(null)));
  }

  /**
   * Every group profile with its permissions and members.
   *
   * @return group profiles, by code
   */
  public List<Group> groups() {
    return roles.findAll().stream()
        .sorted(Comparator.comparing(Role::getCode))
        .map(
            r ->
                new Group(
                    r.getCode(),
                    r.getName(),
                    r.isActive(),
                    r.getPermissions().stream().map(Permission::name).sorted().toList(),
                    users.findEnabledUsernamesWithRole(r.getCode())))
        .toList();
  }

  private static Account account(AppUser user, DirectoryProfile p) {
    DirectoryStatus status =
        user.isEnabled() ? DirectoryStatus.ACTIVE : DirectoryStatus.DEACTIVATED;
    DirectoryAccount account =
        p == null
            ? new DirectoryAccount(
                user.getWindowsId(),
                user.getUsername(),
                user.getEmail(),
                null,
                null,
                user.getFullName(),
                null,
                status,
                null,
                null,
                null,
                null)
            : new DirectoryAccount(
                user.getWindowsId(),
                user.getUsername(),
                user.getEmail(),
                p.getFirstName(),
                p.getLastName(),
                user.getFullName(),
                p.getAdGroup(),
                status,
                new DirectoryAccount.Hierarchy(
                    p.getTeamLeaderName(),
                    p.getTeamHeadName(),
                    p.getSectionHeadName(),
                    p.getUnitHeadName()),
                new DirectoryAccount.Organisation(
                    p.getUnitSegment(), p.getDepartment(), p.getLocation()),
                p.getUidmRequestNo(),
                null);
    return new Account(account, user.getRoles().stream().map(Role::getCode).sorted().toList());
  }

  /**
   * An account of the extract.
   *
   * @param account details and status in the system
   * @param groupProfiles group profiles held
   */
  public record Account(DirectoryAccount account, List<String> groupProfiles) {}

  /**
   * A group profile of the extract.
   *
   * @param code code
   * @param name name
   * @param active whether active
   * @param permissions permissions
   * @param members enabled members
   */
  public record Group(
      String code, String name, boolean active, List<String> permissions, List<String> members) {}
}
