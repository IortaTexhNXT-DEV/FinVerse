package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRiskFlag;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Risk rules of an access request (UAM-NFR-40; FR-UA-034): a request that raises a user or a role
 * to a HIGH or ADMIN privilege level (PRIVILEGE_INCREASE), or that is submitted or approved outside
 * {@code UAM_WORKING_HOURS} (OUTSIDE_HOURS), needs a second approval. The high-privilege profiles
 * and the hours are UQ07.
 */
@Component
@Transactional(readOnly = true)
public class AccessRiskRules {

  private static final Set<PrivilegeLevel> PRIVILEGED =
      EnumSet.of(PrivilegeLevel.HIGH, PrivilegeLevel.ADMIN);

  private final RoleRepository roles;
  private final AppUserRepository users;
  private final AccessSettings settings;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param roles roles (privilege levels)
   * @param users users (current roles)
   * @param settings parameters (working hours)
   * @param clock clock
   */
  public AccessRiskRules(
      RoleRepository roles, AppUserRepository users, AccessSettings settings, Clock clock) {
    this.roles = roles;
    this.users = users;
    this.settings = settings;
    this.clock = clock;
  }

  /**
   * The risk flags of a request now (on submission or approval).
   *
   * @param content what is requested
   * @return flags, empty when none
   */
  public Set<AccessRiskFlag> evaluate(AccessRequestContent content) {
    Set<AccessRiskFlag> flags = EnumSet.noneOf(AccessRiskFlag.class);
    if (raisesPrivilege(content)) {
      flags.add(AccessRiskFlag.PRIVILEGE_INCREASE);
    }
    if (!settings.workingHours().contains(ZonedDateTime.now(clock))) {
      flags.add(AccessRiskFlag.OUTSIDE_HOURS);
    }
    return flags;
  }

  private boolean raisesPrivilege(AccessRequestContent c) {
    if (c.type().carriesUserRoles()) {
      return grantsPrivilegedRole(c);
    }
    return switch (c.type()) {
      case CREATE_ROLE -> c.role() != null && PRIVILEGED.contains(c.role().privilegeLevel());
      case MODIFY_ROLE_PERMISSIONS -> raisesRoleLevel(c.roleCode(), c.role());
      case REACTIVATE_ROLE -> privileged(c.roleCode());
      default -> false;
    };
  }

  private boolean grantsPrivilegedRole(AccessRequestContent c) {
    Set<String> held =
        c.type() == AccessRequestType.CREATE_USER || c.username() == null
            ? Set.of()
            : users
                .findByUsernameIgnoreCase(c.username())
                .map(AppUser::getRoles)
                .orElse(Set.of())
                .stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
    return roles.findByCodeIn(c.roleCodes()).stream()
        .filter(r -> !held.contains(r.getCode()))
        .anyMatch(r -> PRIVILEGED.contains(r.getPrivilegeLevel()));
  }

  private boolean raisesRoleLevel(String roleCode, RequestedRole requested) {
    if (requested == null || !PRIVILEGED.contains(requested.privilegeLevel())) {
      return false;
    }
    return roles
        .findByCode(roleCode)
        .map(r -> r.getPrivilegeLevel().compareTo(requested.privilegeLevel()) < 0)
        .orElse(true);
  }

  private boolean privileged(String roleCode) {
    return roles
        .findByCode(roleCode)
        .map(r -> PRIVILEGED.contains(r.getPrivilegeLevel()))
        .orElse(false);
  }
}
