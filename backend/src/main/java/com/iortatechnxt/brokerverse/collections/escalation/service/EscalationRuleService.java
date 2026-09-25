package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule.Terms;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRuleRepository;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escalation rules (BRCLXN.049, CQ14): created and changed on Collections Setup ({@code
 * CLX_SETUP}), authorized by another user ({@code MASTER_AUTHORIZE}, four eyes) before the job and
 * the broken-promise check use them, deactivated instead of deleted.
 */
@Service
@Transactional
public class EscalationRuleService {

  /** Audit entity type. */
  public static final String ENTITY = "EscalationRule";

  private final EscalationRuleRepository rules;
  private final EscalationNotices notices;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param notices target and reason checks
   * @param audit audit trail
   * @param currentUser checker
   * @param clock clock
   */
  public EscalationRuleService(
      EscalationRuleRepository rules,
      EscalationNotices notices,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.notices = notices;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules by code
   */
  @Transactional(readOnly = true)
  public List<EscalationRule> list(Long companyId) {
    return rules.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * A rule.
   *
   * @param id rule
   * @return rule
   */
  @Transactional(readOnly = true)
  public EscalationRule get(Long id) {
    return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a rule, pending authorization.
   *
   * @param companyId company
   * @param code code
   * @param terms terms
   * @return the rule
   */
  public EscalationRule create(Long companyId, String code, Terms terms) {
    String key = code.strip().toUpperCase(Locale.ROOT);
    if (rules.findByCompanyIdAndCode(companyId, key).isPresent()) {
      throw new DuplicateResourceException(ENTITY, key);
    }
    check(terms);
    EscalationRule rule = rules.save(new EscalationRule(companyId, key, terms));
    audit.record(ENTITY, key, AuditAction.CREATE, describe(rule));
    return rule;
  }

  /**
   * Changes a rule; it must be authorized again.
   *
   * @param id rule
   * @param terms new terms
   * @return the rule
   */
  public EscalationRule update(Long id, Terms terms) {
    EscalationRule rule = get(id);
    check(terms);
    rule.change(terms);
    audit.record(ENTITY, rule.getCode(), AuditAction.UPDATE, describe(rule));
    return rule;
  }

  /**
   * Authorizes a rule (checker, not its maker).
   *
   * @param id rule
   * @return the rule
   */
  public EscalationRule authorize(Long id) {
    EscalationRule rule = get(id);
    rule.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, rule.getCode(), AuditAction.AUTHORIZE, "Authorized");
    return rule;
  }

  /**
   * Deactivates a rule.
   *
   * @param id rule
   * @return the rule
   */
  public EscalationRule deactivate(Long id) {
    EscalationRule rule = get(id);
    rule.deactivate();
    audit.record(ENTITY, rule.getCode(), AuditAction.DEACTIVATE, "Deactivated");
    return rule;
  }

  private void check(Terms terms) {
    notices.requireReason(terms.reasonCode());
    notices.requireTarget(terms.targetLevel(), terms.targetUsername());
  }

  private static String describe(EscalationRule rule) {
    return rule.getName()
        + ": "
        + rule.getBasis()
        + " >= "
        + rule.getThreshold()
        + " -> "
        + rule.getTargetLevel()
        + (rule.getTargetUsername() == null ? "" : " " + rule.getTargetUsername());
  }
}
