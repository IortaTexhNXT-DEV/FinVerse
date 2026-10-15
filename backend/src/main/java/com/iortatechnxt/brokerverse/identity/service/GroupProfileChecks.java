package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleRepository;
import com.iortatechnxt.brokerverse.nbadmin.service.PermissionConflicts;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The checks of the group profiles UIDM-ISC gives a user (membership orchestrated by UIDM-ISC):
 * every group profile exists and is active, and together they break no active separation-of-duties
 * rule (group profiles one user may not hold together) nor combine conflicting permissions.
 */
@Component
@Transactional(readOnly = true)
public class GroupProfileChecks {

  /** Start of the refusal of a group profile that does not exist (answered as not found). */
  public static final String NO_GROUP = "No group profile ";

  private static final String GROUP_CONFLICT = "GROUP_CONFLICT";

  private final RoleRepository roles;
  private final SodRuleRepository sodRules;
  private final PermissionConflicts conflicts;

  /**
   * Creates the checks.
   *
   * @param roles group profiles
   * @param sodRules separation-of-duties rules
   * @param conflicts conflicting permission combinations across the group profiles of a user
   */
  public GroupProfileChecks(
      RoleRepository roles, SodRuleRepository sodRules, PermissionConflicts conflicts) {
    this.roles = roles;
    this.sodRules = sodRules;
    this.conflicts = conflicts;
  }

  /**
   * The group profiles one user would hold, after the checks.
   *
   * @param codes codes of the group profiles
   * @return the group profiles
   * @throws IdentityRefused UNKNOWN_GROUP, GROUP_INACTIVE or GROUP_CONFLICT
   */
  public Set<Role> groups(Collection<String> codes) {
    Set<String> wanted = new TreeSet<>(codes);
    List<Role> found = existing(wanted);
    requireNoSodConflict(wanted);
    try {
      conflicts.checkUser(found);
    } catch (BusinessRuleException ex) {
      throw new IdentityRefused(GROUP_CONFLICT, ex.getMessage(), ex);
    }
    return new HashSet<>(found);
  }

  private List<Role> existing(Set<String> wanted) {
    List<Role> found = roles.findByCodeIn(wanted);
    Set<String> unknown = new TreeSet<>(wanted);
    found.forEach(r -> unknown.remove(r.getCode()));
    if (!unknown.isEmpty()) {
      throw new IdentityRefused("UNKNOWN_GROUP", NO_GROUP + String.join(", ", unknown));
    }
    found.stream()
        .filter(r -> !r.isActive())
        .findFirst()
        .ifPresent(
            r -> {
              throw new IdentityRefused(
                  "GROUP_INACTIVE", "Group profile " + r.getCode() + " is not active");
            });
    return found;
  }

  private void requireNoSodConflict(Set<String> wanted) {
    for (SodRule rule : sodRules.findByRecordStatus(RecordStatus.ACTIVE)) {
      if (rule.getKind() == SodRuleKind.PROFILES && rule.forbids(wanted)) {
        throw new IdentityRefused(
            GROUP_CONFLICT,
            "One user may not hold both "
                + rule.getProfileA()
                + " and "
                + rule.getProfileB()
                + " (separation-of-duties rule "
                + rule.getRuleCode()
                + ")");
      }
    }
  }
}
