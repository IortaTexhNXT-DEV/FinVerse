package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ValidationKind;
import java.util.List;

/**
 * The case validation rules of one configuration version (SNSRP-701, 802, FR-SS-060).
 *
 * @param version the version the rules belong to
 * @param rules the rules
 */
public record ValidationRules(ConfigVersionRef version, List<Rule> rules) {

  /** Defensive copy. */
  public ValidationRules {
    rules = List.copyOf(rules);
  }

  /**
   * The rules for a stage and case type; blank conditions match any value.
   *
   * @param stage the {@code SCR_CASE} stage the case leaves
   * @param caseType the case type
   * @return the rules that apply
   */
  public List<Rule> rulesFor(String stage, String caseType) {
    return rules.stream()
        .filter(r -> r.stage() == null || r.stage().equals(stage))
        .filter(r -> r.caseType() == null || r.caseType().equals(caseType))
        .toList();
  }

  /**
   * One validation rule.
   *
   * @param id the rule id
   * @param stage condition on the stage, {@code null} = any
   * @param caseType condition on {@code SCR_CASE_TYPE}, {@code null} = any
   * @param rule the check
   * @param parameters check parameters (for DOCUMENT_TYPES_PRESENT a comma-separated list of {@code
   *     SCR_DOCUMENT_TYPE} codes), may be {@code null}
   * @param blocking whether a failure stops the submission (otherwise it is a warning)
   */
  public record Rule(
      Long id,
      String stage,
      String caseType,
      ValidationKind rule,
      String parameters,
      boolean blocking) {}
}
