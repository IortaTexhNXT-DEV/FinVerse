package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Prevents conflicting permission combinations (BDOI FRS FRUM.006.03; conflict C16): the two
 * permissions of an active rule of kind PERMISSIONS may be held neither by one group profile nor,
 * through several group profiles, by one user. A combination that already exists is not refused
 * until a change adds one of its permissions.
 */
@Component
public class PermissionConflicts {

  private static final String CODE = "ACCESS_PERMISSION_COMBINATION";

  private final SodRuleRepository rules;

  /**
   * Creates the check.
   *
   * @param rules separation-of-duties rules
   */
  public PermissionConflicts(SodRuleRepository rules) {
    this.rules = rules;
  }

  /**
   * Checks the permissions a group profile would hold after a change.
   *
   * @param profile name of the group profile
   * @param resulting permissions after the change
   * @param added permissions the change adds
   * @throws BusinessRuleException ACCESS_PERMISSION_COMBINATION
   */
  public void checkProfile(String profile, Set<String> resulting, Set<String> added) {
    for (SodRule rule : permissionRules()) {
      if (rule.forbids(resulting)
          && (added.contains(rule.getProfileA()) || added.contains(rule.getProfileB()))) {
        throw new BusinessRuleException(
            CODE,
            "Group profile "
                + profile
                + " may not hold both "
                + PermissionNames.name(rule.getProfileA())
                + " and "
                + PermissionNames.name(rule.getProfileB())
                + " (rule "
                + rule.getRuleCode()
                + ")");
      }
    }
  }

  /**
   * Checks the permissions one user would hold through group profiles.
   *
   * @param profiles the group profiles of the user after the change
   * @throws BusinessRuleException ACCESS_PERMISSION_COMBINATION
   */
  public void checkUser(Collection<Role> profiles) {
    Set<String> held = new HashSet<>();
    profiles.forEach(r -> r.getPermissions().stream().map(Permission::name).forEach(held::add));
    for (SodRule rule : permissionRules()) {
      if (rule.forbids(held) && !withinOneProfile(rule, profiles)) {
        throw new BusinessRuleException(
            CODE,
            "One user may not hold both "
                + PermissionNames.name(rule.getProfileA())
                + " and "
                + PermissionNames.name(rule.getProfileB())
                + " through group profiles "
                + profiles.stream().map(Role::getName).sorted().collect(Collectors.joining(", "))
                + " (rule "
                + rule.getRuleCode()
                + ")");
      }
    }
  }

  private static boolean withinOneProfile(SodRule rule, Collection<Role> profiles) {
    return profiles.stream()
        .map(r -> r.getPermissions().stream().map(Permission::name).collect(Collectors.toSet()))
        .anyMatch(rule::forbids);
  }

  private List<SodRule> permissionRules() {
    return rules.findByRecordStatus(RecordStatus.ACTIVE).stream()
        .filter(r -> r.getKind() == SodRuleKind.PERMISSIONS)
        .toList();
  }
}
