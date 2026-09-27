package com.iortatechnxt.brokerverse.submitted.setup.api;

import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.processing.service.RuleSetService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmFacts;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmRuleEngine;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.CreateRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.DecisionRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.HeaderRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.RuleRequest;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.RuleSetView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.RuleView;
import com.iortatechnxt.brokerverse.submitted.setup.api.dto.RuleSetDtos.VersionRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Submitted Policies Setup, Rule Sets (FRS FR-SP-020): the versions with their rules, drafts and
 * their rules (maker, {@code SBM_RULE_MAINTAIN}), approval and rejection (checker, {@code
 * SBM_RULE_APPROVE}), and the facts and operators a rule may use.
 */
@RestController
@RequestMapping("/api/v1/submitted/setup/rule-sets")
public class RuleSetController {

  /** Setup readers: makers and checkers of the rules. */
  public static final String READ =
      "hasAnyAuthority('SBM_RULE_MAINTAIN','SBM_RULE_APPROVE','SBM_PROCESS')";

  /** Makers. */
  public static final String MAINTAIN = "hasAuthority('SBM_RULE_MAINTAIN')";

  /** Checkers. */
  public static final String APPROVE = "hasAuthority('SBM_RULE_APPROVE')";

  private final RuleSetService ruleSets;

  /**
   * Creates the controller.
   *
   * @param ruleSets rule sets
   */
  public RuleSetController(RuleSetService ruleSets) {
    this.ruleSets = ruleSets;
  }

  /**
   * Every version of a company.
   *
   * @param companyId company
   * @return versions
   */
  @GetMapping
  @PreAuthorize(READ)
  public List<RuleSetView> list(@RequestParam Long companyId) {
    return ruleSets.list(companyId).stream().map(s -> RuleSetView.from(s, List.of())).toList();
  }

  /**
   * A version with its rules.
   *
   * @param id version
   * @return version
   */
  @GetMapping("/{id}")
  @PreAuthorize(READ)
  @Transactional(readOnly = true)
  public RuleSetView get(@PathVariable Long id) {
    return view(ruleSets.get(id));
  }

  /**
   * The active version of the same code (comparison before approval).
   *
   * @param id version
   * @return active version, or no content
   */
  @GetMapping("/{id}/active")
  @PreAuthorize(READ)
  @Transactional(readOnly = true)
  public List<RuleSetView> active(@PathVariable Long id) {
    return ruleSets.activeOf(id).map(this::view).stream().toList();
  }

  /**
   * The facts and operators of the conditions.
   *
   * @return facts and operators
   */
  @GetMapping("/vocabulary")
  @PreAuthorize(READ)
  public Map<String, List<String>> vocabulary() {
    return Map.of("facts", SbmFacts.NAMES, "operators", SbmRuleEngine.OPERATORS);
  }

  /**
   * Creates a rule set.
   *
   * @param r code, scope and header
   * @return draft
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleSetView create(@Valid @RequestBody CreateRequest r) {
    return view(
        ruleSets.create(
            r.companyId(),
            r.code().strip(),
            new SbmRuleSet.Scope(r.step(), blankToNull(r.segment()), blankToNull(r.businessType())),
            r.effectiveFrom(),
            r.description()));
  }

  /**
   * A new version of a code.
   *
   * @param id version to copy
   * @param r effective date
   * @return draft
   */
  @PostMapping("/{id}/versions")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleSetView newVersion(@PathVariable Long id, @RequestBody VersionRequest r) {
    return view(ruleSets.newVersion(id, r.effectiveFrom()));
  }

  /**
   * Changes the header of a draft.
   *
   * @param id draft
   * @param r header
   * @return draft
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleSetView describe(@PathVariable Long id, @Valid @RequestBody HeaderRequest r) {
    return view(ruleSets.describe(id, r.effectiveFrom(), r.description()));
  }

  /**
   * Adds a rule.
   *
   * @param id draft
   * @param r rule
   * @return rule
   */
  @PostMapping("/{id}/rules")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleView addRule(@PathVariable Long id, @Valid @RequestBody RuleRequest r) {
    return RuleView.from(ruleSets.addRule(id, r.content()));
  }

  /**
   * Changes a rule.
   *
   * @param ruleId rule
   * @param r rule
   * @return rule
   */
  @PutMapping("/rules/{ruleId}")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleView changeRule(@PathVariable Long ruleId, @Valid @RequestBody RuleRequest r) {
    return RuleView.from(ruleSets.changeRule(ruleId, r.content()));
  }

  /**
   * Removes a rule.
   *
   * @param ruleId rule
   */
  @DeleteMapping("/rules/{ruleId}")
  @PreAuthorize(MAINTAIN)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeRule(@PathVariable Long ruleId) {
    ruleSets.removeRule(ruleId);
  }

  /**
   * Submits a draft.
   *
   * @param id draft
   * @return rule set
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  @Transactional
  public RuleSetView submit(@PathVariable Long id) {
    return view(ruleSets.submit(id));
  }

  /**
   * Approves a version.
   *
   * @param id version
   * @param r remarks
   * @return rule set
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(APPROVE)
  @Transactional
  public RuleSetView approve(@PathVariable Long id, @RequestBody DecisionRequest r) {
    return view(ruleSets.approve(id, r.remarks()));
  }

  /**
   * Rejects a version.
   *
   * @param id version
   * @param r reason
   * @return rule set
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(APPROVE)
  @Transactional
  public RuleSetView reject(@PathVariable Long id, @RequestBody DecisionRequest r) {
    return view(ruleSets.reject(id, r.remarks()));
  }

  private RuleSetView view(SbmRuleSet s) {
    return RuleSetView.from(s, ruleSets.rulesOf(s.getId()));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
