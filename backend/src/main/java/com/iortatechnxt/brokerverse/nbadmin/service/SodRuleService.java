package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleRepository;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separation-of-duties rules (V1065): pairs of group profiles one user may not hold, maintained by
 * the Business Administrator and authorised by Information Security (maker-checker), and checked on
 * every user request and bulk line that gives group profiles.
 */
@Service
@Transactional
public class SodRuleService {

  /** Audit entity of the rules. */
  public static final String ENTITY = "SodRule";

  /** Permission of the authorisers. */
  public static final String AUTHORIZE = "UAM_SOD_AUTHORIZE";

  /** Screen of the rules. */
  public static final String SCREEN = "/user-access/sod-rules";

  private final SodRuleRepository rules;
  private final RoleRepository roles;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param roles group profiles
   * @param numbers rule numbers
   * @param audit audit trail
   * @param notifications notices to the authorisers
   * @param currentUser current user
   * @param clock clock
   */
  public SodRuleService(
      SodRuleRepository rules,
      RoleRepository roles,
      DocumentNumberService numbers,
      AuditTrailService audit,
      NotificationService notifications,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.roles = roles;
    this.numbers = numbers;
    this.audit = audit;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every rule, by number.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SodRule> list() {
    return rules.findAllByOrderByRuleCodeAsc();
  }

  /**
   * Rules waiting for an authorisation.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SodRule> pending() {
    return rules.findAll().stream().filter(SodRule::isPending).toList();
  }

  /**
   * Creates a rule, pending authorisation.
   *
   * @param profileA first group profile
   * @param profileB second group profile
   * @param description why they may not be held together
   * @return the rule
   */
  public SodRule create(String profileA, String profileB, String description) {
    String a = requireProfile(profileA);
    String b = requireProfile(profileB);
    if (a.equals(b)) {
      throw new BusinessRuleException("SOD_SAME_PROFILE", "Choose two different group profiles");
    }
    if (description == null || description.isBlank()) {
      throw new BusinessRuleException(
          "SOD_DESCRIPTION", "Enter why the two profiles are not held together");
    }
    boolean exists =
        rules.findAll().stream()
            .anyMatch(r -> r.getRecordStatus() != RecordStatus.INACTIVE && r.samePair(a, b));
    if (exists) {
      throw new BusinessRuleException(
          "SOD_RULE_EXISTS", "A rule for these two group profiles already exists");
    }
    SodRule rule = rules.save(new SodRule(numbers.next("SOD"), a, b, description.trim()));
    audit.record(ENTITY, rule.getRuleCode(), AuditAction.CREATE, text("Created", rule));
    tellAuthorisers(rule, "new rule");
    return rule;
  }

  /**
   * Asks for the deactivation of an active rule; it waits for the authorisation.
   *
   * @param id rule
   * @return the rule
   */
  public SodRule requestDeactivation(Long id) {
    SodRule rule = get(id);
    rule.requestDeactivation();
    rules.saveAndFlush(rule);
    audit.record(
        ENTITY, rule.getRuleCode(), AuditAction.SUBMIT, text("Deactivation requested", rule));
    tellAuthorisers(rule, "deactivation");
    return rule;
  }

  /**
   * Authorises the pending creation or deactivation of a rule (not by its maker).
   *
   * @param id rule
   * @return the rule
   */
  public SodRule authorize(Long id) {
    SodRule rule = get(id);
    rule.authorizePending(currentUser.username(), rule.getMaker(), clock.instant());
    audit.record(ENTITY, rule.getRuleCode(), AuditAction.AUTHORIZE, text("Authorised", rule));
    return rule;
  }

  /**
   * Rejects the pending creation or deactivation of a rule (not by its maker), with its reason.
   *
   * @param id rule
   * @param reason why the change is rejected (mandatory, kept in the audit trail)
   * @return the rule
   */
  public SodRule reject(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the rejection");
    }
    SodRule rule = get(id);
    rule.rejectPending(currentUser.username(), rule.getMaker());
    audit.record(
        ENTITY,
        rule.getRuleCode(),
        AuditAction.REJECT,
        text("Rejected", rule) + ". Reason: " + reason.strip());
    return rule;
  }

  private SodRule get(Long id) {
    return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private String requireProfile(String code) {
    String clean = code == null ? "" : code.trim();
    if (roles.findByCode(clean).isEmpty()) {
      throw new BusinessRuleException(
          "SOD_UNKNOWN_PROFILE", "Choose the group profiles from the list");
    }
    return clean;
  }

  private void tellAuthorisers(SodRule rule, String what) {
    notifications.notifyPermission(
        AUTHORIZE,
        new Notice(
            "Separation-of-duties rule " + rule.getRuleCode() + " to authorise",
            "The " + what + " of rule " + rule.getRuleCode() + ": " + rule.getDescription(),
            SCREEN,
            ENTITY,
            String.valueOf(rule.getId())),
        "UAM_SOD_TO_AUTHORIZE");
  }

  private static String text(String what, SodRule rule) {
    return what
        + " rule "
        + rule.getRuleCode()
        + ": "
        + rule.getProfileA()
        + " and "
        + rule.getProfileB()
        + " not held together";
  }
}
