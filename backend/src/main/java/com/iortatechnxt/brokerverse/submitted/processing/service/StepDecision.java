package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleOutcome;
import java.util.List;
import java.util.Map;

/**
 * What the rules of one step decided for a record (BRIDSP-08): the rules tried highest priority
 * first, each matching rule adding the parts of its outcome not yet decided, until a matching rule
 * with {@code stop}. No matching rule means fallout with reason {@code SBM_NO_RULE}.
 *
 * @param matched whether a rule matched
 * @param outcome merged outcome
 * @param reasonCode reason of the first matching rule that gives one
 * @param rule the rule that decided (the first match), null when none
 */
public record StepDecision(
    boolean matched, SbmRuleOutcome outcome, String reasonCode, SbmRule rule) {

  /** Reason of a record no rule could place. */
  public static final String NO_RULE = "SBM_NO_RULE";

  /** Fallout flag of an outcome. */
  public static final String FALLOUT = "FALLOUT";

  /** Review flag of an outcome. */
  public static final String REVIEW = "REVIEW";

  /**
   * Evaluates the rules of a step.
   *
   * @param rules rules, highest priority first
   * @param facts facts of the record
   * @return decision
   */
  public static StepDecision evaluate(List<SbmRule> rules, Map<String, Object> facts) {
    SbmRule first = null;
    String bucket = null;
    String tag = null;
    String classification = null;
    String template = null;
    String flag = null;
    String reason = null;
    for (SbmRule r : rules) {
      if (!SbmRuleEngine.matches(r, facts)) {
        continue;
      }
      SbmRuleOutcome o = r.getOutcome();
      first = first == null ? r : first;
      bucket = bucket == null ? o.bucket() : bucket;
      tag = tag == null ? o.tag() : tag;
      classification = classification == null ? o.classification() : classification;
      template = template == null ? o.raTemplate() : template;
      flag = flag == null ? o.flag() : flag;
      reason = reason == null ? r.getReasonCode() : reason;
      if (r.isStop()) {
        break;
      }
    }
    return new StepDecision(
        first != null,
        new SbmRuleOutcome(bucket, tag, classification, template, flag),
        reason,
        first);
  }

  /**
   * Whether the step sends the record to the fallout.
   *
   * @return true when no rule matched or the outcome says so
   */
  public boolean fallout() {
    return !matched || FALLOUT.equals(outcome.flag());
  }

  /**
   * The fallout reason.
   *
   * @return reason, {@code SBM_NO_RULE} when no rule matched
   */
  public String falloutReason() {
    return matched ? reasonCode : NO_RULE;
  }
}
