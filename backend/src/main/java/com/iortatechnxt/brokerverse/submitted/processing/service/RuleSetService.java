package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleCondition;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetStatus;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule sets with maker and checker (BRIDSP-08; FRS FR-SP-020): a draft version is created (with the
 * rules of the current version), edited, submitted and approved by someone other than its maker
 * with an effective date; the approval retires the version it replaces. A rejected version goes
 * back to its maker as a draft with the reason. Every rule is checked: known facts and operators,
 * an outcome, a unique priority, buckets and reasons of their lists.
 */
@Service
@Transactional
public class RuleSetService {

  /** Audit entity. */
  public static final String ENTITY = "SubmittedRuleSet";

  private final SbmRuleSetRepository sets;
  private final SbmRuleRepository rules;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sets rule sets
   * @param rules rules
   * @param lovs lists of values
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RuleSetService(
      SbmRuleSetRepository sets,
      SbmRuleRepository rules,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.sets = sets;
    this.rules = rules;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Rule sets of a company.
   *
   * @param companyId company
   * @return every version, newest first per code
   */
  @Transactional(readOnly = true)
  public List<SbmRuleSet> list(Long companyId) {
    return sets.findByCompanyIdOrderByStepAscCodeAscVersionNoDesc(companyId);
  }

  /**
   * A rule set.
   *
   * @param id rule set
   * @return rule set
   */
  @Transactional(readOnly = true)
  public SbmRuleSet get(Long id) {
    return sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rule set", id));
  }

  /**
   * The rules of a rule set, highest priority first.
   *
   * @param id rule set
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SbmRule> rulesOf(Long id) {
    return rules.findByRuleSetIdOrderByPriorityDesc(id);
  }

  /**
   * The active version of the code of a rule set (comparison before approval).
   *
   * @param id rule set
   * @return active version, empty when none
   */
  @Transactional(readOnly = true)
  public Optional<SbmRuleSet> activeOf(Long id) {
    SbmRuleSet s = get(id);
    return sets
        .findByCompanyIdAndCodeAndStatusIn(
            s.getCompanyId(), s.getCode(), List.of(SbmRuleSetStatus.ACTIVE))
        .stream()
        .findFirst();
  }

  /**
   * Creates a new rule set (version 1).
   *
   * @param companyId company
   * @param code code
   * @param scope step, segment and business type
   * @param effectiveFrom effective date
   * @param description description
   * @return draft
   */
  public SbmRuleSet create(
      Long companyId, String code, SbmRuleSet.Scope scope, LocalDate effectiveFrom, String description) {
    if (sets.findFirstByCompanyIdAndCodeOrderByVersionNoDesc(companyId, code).isPresent()) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_EXISTS", "Rule set " + code + " exists; create a new version of it");
    }
    if (scope.segment() != null) {
      lovs.requireValid(SubmittedCodes.LOV_SEGMENT, scope.segment(), BusinessClock.today(clock));
    }
    SbmRuleSet s = sets.save(new SbmRuleSet(companyId, code, 1, scope, effectiveFrom, description));
    audit.record(ENTITY, code + " v1", AuditAction.CREATE, "Rule set created");
    return s;
  }

  /**
   * A new draft version from an existing version, with a copy of its rules.
   *
   * @param id version to copy
   * @param effectiveFrom effective date of the new version
   * @return draft
   */
  public SbmRuleSet newVersion(Long id, LocalDate effectiveFrom) {
    SbmRuleSet from = get(id);
    if (!sets.findByCompanyIdAndCodeAndStatusIn(
            from.getCompanyId(),
            from.getCode(),
            List.of(SbmRuleSetStatus.DRAFT, SbmRuleSetStatus.SUBMITTED))
        .isEmpty()) {
      throw new BusinessRuleException(
          "SBM_RULE_SET_DRAFT_OPEN",
          "Rule set " + from.getCode() + " already has a version being prepared");
    }
    int next =
        sets.findFirstByCompanyIdAndCodeOrderByVersionNoDesc(from.getCompanyId(), from.getCode())
                .map(SbmRuleSet::getVersionNo)
                .orElse(0)
            + 1;
    SbmRuleSet draft =
        sets.save(
            new SbmRuleSet(
                from.getCompanyId(),
                from.getCode(),
                next,
                new SbmRuleSet.Scope(from.getStep(), from.getSegment(), from.getBusinessType()),
                effectiveFrom == null ? BusinessClock.today(clock) : effectiveFrom,
                from.getDescription()));
    for (SbmRule r : rulesOf(from.getId())) {
      rules.save(
          new SbmRule(
              draft.getId(),
              new SbmRule.Content(
                  r.getPriority(),
                  r.getName(),
                  r.getConditions(),
                  r.getOutcome(),
                  r.getReasonCode(),
                  r.isStop(),
                  r.isActive())));
    }
    audit.record(ENTITY, draft.getCode() + " v" + next, AuditAction.CREATE, "New version");
    return draft;
  }

  /**
   * Changes the header of a draft.
   *
   * @param id draft
   * @param effectiveFrom effective date
   * @param description description
   * @return draft
   */
  public SbmRuleSet describe(Long id, LocalDate effectiveFrom, String description) {
    SbmRuleSet s = get(id);
    s.describe(effectiveFrom, description);
    return s;
  }

  /**
   * Adds a rule to a draft.
   *
   * @param id draft
   * @param content rule
   * @return rule
   */
  public SbmRule addRule(Long id, SbmRule.Content content) {
    SbmRuleSet s = get(id);
    s.requireDraft();
    validate(s, content, null);
    return rules.save(new SbmRule(s.getId(), content));
  }

  /**
   * Changes a rule of a draft.
   *
   * @param ruleId rule
   * @param content rule
   * @return rule
   */
  public SbmRule changeRule(Long ruleId, SbmRule.Content content) {
    SbmRule r = rule(ruleId);
    SbmRuleSet s = get(r.getRuleSetId());
    s.requireDraft();
    validate(s, content, r.getId());
    r.change(content);
    return r;
  }

  /**
   * Removes a rule of a draft.
   *
   * @param ruleId rule
   */
  public void removeRule(Long ruleId) {
    SbmRule r = rule(ruleId);
    get(r.getRuleSetId()).requireDraft();
    rules.delete(r);
  }

  /**
   * Submits a draft for approval; the approvers are notified.
   *
   * @param id draft
   * @return rule set
   */
  public SbmRuleSet submit(Long id) {
    SbmRuleSet s = get(id);
    if (rulesOf(id).isEmpty()) {
      throw new BusinessRuleException("SBM_RULE_SET_EMPTY", "Add at least one rule");
    }
    s.submit(currentUser.username(), clock.instant());
    notifications.notifyPermission(
        "SBM_RULE_APPROVE",
        new Notice(
            "Rule set " + s.getCode() + " version " + s.getVersionNo() + " to approve",
            s.getDescription(),
            "/submitted/setup?ruleSet=" + s.getId(),
            ENTITY,
            s.getId().toString()));
    audit.record(ENTITY, s.getCode() + " v" + s.getVersionNo(), AuditAction.SUBMIT, "Submitted");
    return s;
  }

  /**
   * Approves a version, which replaces the active one of its code.
   *
   * @param id submitted version
   * @param remarks remarks, may be null
   * @return rule set
   */
  public SbmRuleSet approve(Long id, String remarks) {
    SbmRuleSet s = get(id);
    s.approve(currentUser.username(), clock.instant(), remarks);
    sets.findByCompanyIdAndCodeAndStatusIn(
            s.getCompanyId(), s.getCode(), List.of(SbmRuleSetStatus.ACTIVE))
        .stream()
        .filter(other -> !other.getId().equals(s.getId()))
        .forEach(SbmRuleSet::retire);
    audit.record(ENTITY, s.getCode() + " v" + s.getVersionNo(), AuditAction.AUTHORIZE, "Approved");
    return s;
  }

  /**
   * Rejects a version back to its maker.
   *
   * @param id submitted version
   * @param reason reason
   * @return rule set
   */
  public SbmRuleSet reject(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("SBM_REJECT_REASON_REQUIRED", "Enter the reason of the rejection");
    }
    SbmRuleSet s = get(id);
    s.reject(currentUser.username(), clock.instant(), reason.strip());
    audit.record(ENTITY, s.getCode() + " v" + s.getVersionNo(), AuditAction.REJECT, reason.strip());
    return s;
  }

  private void validate(SbmRuleSet s, SbmRule.Content c, Long ruleId) {
    if (c.conditions().isEmpty()) {
      throw new BusinessRuleException("SBM_RULE_NO_CONDITION", "Add at least one condition");
    }
    for (SbmRuleCondition cond : c.conditions()) {
      if (!SbmFacts.NAMES.contains(cond.field())) {
        throw new BusinessRuleException(
            "SBM_RULE_FIELD_UNKNOWN", "Field " + cond.field() + " cannot be used in a rule");
      }
      if (!SbmRuleEngine.OPERATORS.contains(cond.operator())) {
        throw new BusinessRuleException(
            "SBM_RULE_OPERATOR_UNKNOWN", "Operator " + cond.operator() + " is not an operator");
      }
    }
    if (c.outcome() == null || c.outcome().isEmpty()) {
      throw new BusinessRuleException("SBM_RULE_NO_OUTCOME", "Select the outcome of the rule");
    }
    LocalDate today = BusinessClock.today(clock);
    if (c.outcome().bucket() != null) {
      lovs.requireValid(SubmittedCodes.LOV_BUCKET, c.outcome().bucket(), today);
    }
    if (c.reasonCode() != null) {
      lovs.requireValid(SubmittedCodes.LOV_REASON, c.reasonCode(), today);
    }
    boolean samePriority =
        rulesOf(s.getId()).stream()
            .anyMatch(r -> r.getPriority() == c.priority() && !r.getId().equals(ruleId));
    if (samePriority) {
      throw new BusinessRuleException(
          "SBM_RULE_PRIORITY_TAKEN", "Priority " + c.priority() + " is already used in this version");
    }
  }

  private SbmRule rule(Long ruleId) {
    return rules.findById(ruleId).orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId));
  }
}
