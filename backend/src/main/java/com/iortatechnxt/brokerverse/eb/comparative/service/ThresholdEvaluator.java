package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRuleRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates the value threshold rules (BRID-016; FR-EB-042) on the TSI and annual premium of the
 * recommended proposal of each benefit line (at sign-off) or of the chosen proposal (at client
 * confirmation). A rule for every line applies to each line.
 */
@Component
@Transactional(readOnly = true)
public class ThresholdEvaluator {

  private final EbThresholdRuleRepository rules;

  /**
   * Creates the evaluator.
   *
   * @param rules threshold rules
   */
  public ThresholdEvaluator(EbThresholdRuleRepository rules) {
    this.rules = rules;
  }

  /**
   * The rules met.
   *
   * @param companyId company
   * @param measures TSI and annual premium per benefit line
   * @param date business date
   * @return result, empty when no rule is met
   */
  public Result evaluate(Long companyId, Map<String, Measures> measures, LocalDate date) {
    List<String> met = new ArrayList<>();
    List<EbThresholdRule> matched = new ArrayList<>();
    for (EbThresholdRule rule : rules.findByCompanyIdOrderByIdAsc(companyId)) {
      measures.forEach(
          (line, m) -> {
            if (rule.appliesTo(line, date) && rule.isMetBy(value(rule, m))) {
              met.add(describe(rule, line));
              matched.add(rule);
            }
          });
    }
    String permission =
        matched.stream()
            .max(Comparator.comparingInt(EbThresholdRule::getApprovalLevel))
            .map(EbThresholdRule::getApproverPermission)
            .orElse(null);
    return new Result(met.stream().distinct().toList(), permission);
  }

  private static BigDecimal value(EbThresholdRule rule, Measures m) {
    return rule.getMeasure() == EbThresholdRule.Measure.TSI ? m.sumInsured() : m.annualPremium();
  }

  private static String describe(EbThresholdRule rule, String line) {
    DecimalFormat amount =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    String measure = rule.getMeasure() == EbThresholdRule.Measure.TSI ? "TSI" : "annual premium";
    return line
        + " "
        + measure
        + " at or above "
        + rule.getCurrency()
        + " "
        + amount.format(rule.getAmount());
  }

  /**
   * The values of a benefit line.
   *
   * @param sumInsured total sum insured, zero when none
   * @param annualPremium annual premium
   */
  public record Measures(BigDecimal sumInsured, BigDecimal annualPremium) {}

  /**
   * Rules met.
   *
   * @param rules descriptions of the rules met
   * @param approverPermission permission of the highest level met, null when none
   */
  public record Result(List<String> rules, String approverPermission) {

    /**
     * Whether a rule is met.
     *
     * @return true when at least one
     */
    public boolean met() {
      return !rules.isEmpty();
    }

    /**
     * The rules as one text (kept on the comparative).
     *
     * @return text, null when none
     */
    public String text() {
      return rules.isEmpty() ? null : String.join("; ", rules);
    }

    /**
     * The approver permission, the default one when the rule names none.
     *
     * @return permission
     */
    public String approver() {
      return approverPermission == null ? EbCodes.PERMISSION_THRESHOLD_APPROVE : approverPermission;
    }
  }
}
