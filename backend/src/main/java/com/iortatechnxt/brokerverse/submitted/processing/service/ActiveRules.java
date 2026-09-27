package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStep;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The active rules of a company for one run (BRIDSP-08): the ACTIVE rule sets in force at the
 * business date, and per step the rules of the sets whose segment and business type fit the
 * record, highest priority first, each with the rule set that holds it.
 *
 * @param sets active rule sets by id
 * @param rules active rules of those sets
 */
public record ActiveRules(Map<Long, SbmRuleSet> sets, List<SbmRule> rules) {

  /** Defensive copies. */
  public ActiveRules {
    sets = Map.copyOf(sets);
    rules = List.copyOf(rules);
  }

  /**
   * Builds the rules of a run.
   *
   * @param sets active rule sets in force
   * @param rules their rules
   * @return rules of the run
   */
  public static ActiveRules of(List<SbmRuleSet> sets, List<SbmRule> rules) {
    return new ActiveRules(
        sets.stream().collect(Collectors.toMap(SbmRuleSet::getId, Function.identity())),
        rules.stream().filter(SbmRule::isActive).toList());
  }

  /**
   * The rules of a step for a record, highest priority first.
   *
   * @param step step
   * @param p record
   * @return rules
   */
  public List<SbmRule> of(SbmStep step, SbmPolicy p) {
    return rules.stream()
        .filter(r -> fits(sets.get(r.getRuleSetId()), step, p))
        .sorted(Comparator.comparingInt(SbmRule::getPriority).reversed())
        .toList();
  }

  /**
   * The rule set of a rule.
   *
   * @param rule rule
   * @return rule set
   */
  public SbmRuleSet setOf(SbmRule rule) {
    return sets.get(rule.getRuleSetId());
  }

  private static boolean fits(SbmRuleSet set, SbmStep step, SbmPolicy p) {
    return set != null
        && set.getStep() == step
        && (set.getSegment() == null || set.getSegment().equals(p.getSegment()))
        && (set.getBusinessType() == null
            || set.getBusinessType().equals(p.getBusinessType().name()));
  }
}
