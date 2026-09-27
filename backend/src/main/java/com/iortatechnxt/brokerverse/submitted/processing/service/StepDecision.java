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
    Merged m = new Merged();
    for (SbmRule r : rules) {
      if (SbmRuleEngine.matches(r, facts)) {
        m.add(r);
        if (r.isStop()) {
          break;
        }
      }
    }
    return m.decision();
  }

  /** The outcome of the matching rules: each part from the first rule that gives it. */
  private static final class Merged {
    private SbmRule first;
    private String bucket;
    private String tag;
    private String classification;
    private String template;
    private String flag;
    private String reason;

    void add(SbmRule r) {
      SbmRuleOutcome o = r.getOutcome();
      first = first == null ? r : first;
      bucket = firstOf(bucket, o.bucket());
      tag = firstOf(tag, o.tag());
      classification = firstOf(classification, o.classification());
      template = firstOf(template, o.raTemplate());
      flag = firstOf(flag, o.flag());
      reason = firstOf(reason, r.getReasonCode());
    }

    StepDecision decision() {
      return new StepDecision(
          first != null,
          new SbmRuleOutcome(bucket, tag, classification, template, flag),
          reason,
          first);
    }

    private static String firstOf(String kept, String next) {
      return kept == null ? next : kept;
    }
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
