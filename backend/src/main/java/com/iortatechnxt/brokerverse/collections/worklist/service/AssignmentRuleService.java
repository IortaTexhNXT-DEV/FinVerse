package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule.Details;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRuleRepository;
import com.iortatechnxt.brokerverse.collections.worklist.domain.RuleCriteria;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default assignment rules (BRCLXN.052), maintained by team leads under {@code CLX_ASSIGN}: the
 * handler of new accounts by segment, sales unit, client, amount and aging, in priority order.
 * Changes are logged field by field (BRCLXN.043).
 */
@Service
@Transactional
public class AssignmentRuleService {

  private static final String ENTITY = "AssignmentRule";

  private final AssignmentRuleRepository rules;
  private final AssignmentService assignments;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param assignments assignment (handler check)
   * @param changes change recorder
   * @param audit audit trail
   */
  public AssignmentRuleService(
      AssignmentRuleRepository rules,
      AssignmentService assignments,
      ChangeRecorder changes,
      AuditTrailService audit) {
    this.rules = rules;
    this.assignments = assignments;
    this.changes = changes;
    this.audit = audit;
  }

  /**
   * The rules of a company in priority order.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<AssignmentRule> list(Long companyId) {
    return rules.findByCompanyIdOrderByPriorityAscIdAsc(companyId);
  }

  /**
   * Adds a rule.
   *
   * @param companyId company
   * @param details priority, name, criteria and handler
   * @return rule
   */
  public AssignmentRule create(Long companyId, Details details) {
    validate(details);
    AssignmentRule saved = rules.save(new AssignmentRule(companyId, details));
    changes.record(target(saved), "rule", null, describe(saved), null);
    audit.record(ENTITY, saved.getId(), AuditAction.CREATE, describe(saved));
    return saved;
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param details new details
   * @return rule
   */
  public AssignmentRule update(Long id, Details details) {
    validate(details);
    AssignmentRule rule = require(id);
    String before = describe(rule);
    rule.update(details);
    changes.record(target(rule), "rule", before, describe(rule), null);
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(rule));
    return rule;
  }

  /**
   * Activates or deactivates a rule.
   *
   * @param id rule
   * @param active new state
   * @return rule
   */
  public AssignmentRule activate(Long id, boolean active) {
    AssignmentRule rule = require(id);
    boolean before = rule.isActive();
    rule.activate(active);
    changes.record(target(rule), "active", before, active, null);
    audit.record(ENTITY, id, AuditAction.UPDATE, active ? "Activated" : "Deactivated");
    return rule;
  }

  private AssignmentRule require(Long id) {
    return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private void validate(Details d) {
    if (d.name() == null || d.name().isBlank()) {
      throw new BusinessRuleException("CLX_RULE_NAME", "Give the rule a name");
    }
    RuleCriteria c = d.criteria() == null ? RuleCriteria.NONE : d.criteria();
    boolean amountReversed =
        c.amountFrom() != null
            && c.amountTo() != null
            && c.amountFrom().compareTo(c.amountTo()) > 0;
    boolean agingReversed =
        c.agingFrom() != null && c.agingTo() != null && c.agingFrom() > c.agingTo();
    if (amountReversed || agingReversed) {
      throw new BusinessRuleException("CLX_RULE_RANGE", "A range must start before it ends");
    }
    assignments.requireHandler(d.handlerUsername());
  }

  private static String describe(AssignmentRule r) {
    RuleCriteria c = r.getCriteria();
    return "#"
        + r.getPriority()
        + " "
        + r.getName()
        + " -> "
        + r.getHandlerUsername()
        + " [segment "
        + c.segment()
        + ", unit "
        + c.salesUnit()
        + ", client "
        + c.clientCode()
        + ", amount "
        + c.amountFrom()
        + "-"
        + c.amountTo()
        + ", aging "
        + c.agingFrom()
        + "-"
        + c.agingTo()
        + "]";
  }

  private static Target target(AssignmentRule r) {
    return new Target(r.getCompanyId(), ENTITY, String.valueOf(r.getId()), null);
  }
}
