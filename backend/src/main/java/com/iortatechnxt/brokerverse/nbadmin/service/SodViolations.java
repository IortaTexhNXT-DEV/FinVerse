package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The current holders of access that breaches an active separation-of-duties rule: an active user
 * holding both group profiles of a PROFILES rule, or both permissions of a PERMISSIONS rule through
 * the active group profiles held. UIDM-ISC assigns the group profiles, so a breach is corrected
 * there; the daily job records each breach found and tells Information Security of the new ones.
 */
@Service
@Transactional(readOnly = true)
public class SodViolations {

  private final SodRuleRepository rules;
  private final AppUserRepository users;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param rules separation-of-duties rules
   * @param users users
   * @param jdbc database (first time each breach was found)
   */
  public SodViolations(SodRuleRepository rules, AppUserRepository users, JdbcTemplate jdbc) {
    this.rules = rules;
    this.users = users;
    this.jdbc = jdbc;
  }

  /**
   * Every breach of an active rule by an active user, by rule and user.
   *
   * @return breaches
   */
  public List<Violation> current() {
    List<SodRule> active = rules.findByRecordStatus(RecordStatus.ACTIVE);
    List<Violation> found = new ArrayList<>();
    for (AppUser u : users.findAll()) {
      if (!u.isEnabled()) {
        continue;
      }
      Set<String> profiles =
          u.getRoles().stream()
              .filter(Role::isActive)
              .map(Role::getCode)
              .collect(Collectors.toCollection(TreeSet::new));
      Set<String> permissions =
          u.effectivePermissions().stream()
              .map(Permission::name)
              .collect(Collectors.toCollection(TreeSet::new));
      for (SodRule rule : active) {
        boolean breach =
            rule.getKind() == SodRuleKind.PERMISSIONS
                ? rule.forbids(permissions)
                : rule.forbids(profiles);
        if (breach) {
          found.add(
              new Violation(
                  rule.getRuleCode(),
                  rule.getKind().name(),
                  rule.getProfileA(),
                  rule.getProfileB(),
                  rule.getDescription(),
                  u.getUsername(),
                  u.getFullName(),
                  String.join(", ", profiles),
                  firstFound(rule.getRuleCode(), u.getUsername())));
        }
      }
    }
    found.sort(Comparator.comparing(Violation::ruleCode).thenComparing(Violation::username));
    return found;
  }

  private Instant firstFound(String ruleCode, String username) {
    List<java.sql.Timestamp> at =
        jdbc.queryForList(
            "select found_at from nba_sod_violation where rule_code = ? and username = ?"
                + " and cleared_at is null",
            java.sql.Timestamp.class,
            ruleCode,
            username);
    return at.isEmpty() ? null : at.get(0).toInstant();
  }

  /**
   * A breach of a rule by a user.
   *
   * @param ruleCode rule
   * @param kind PROFILES or PERMISSIONS
   * @param first first group profile or permission of the rule
   * @param second second group profile or permission of the rule
   * @param description why the two may not be held together
   * @param username user ID
   * @param fullName name
   * @param groupProfiles the active group profiles the user holds
   * @param foundAt when the daily check first found the breach; null before the next check
   */
  public record Violation(
      String ruleCode,
      String kind,
      String first,
      String second,
      String description,
      String username,
      String fullName,
      String groupProfiles,
      Instant foundAt) {}
}
