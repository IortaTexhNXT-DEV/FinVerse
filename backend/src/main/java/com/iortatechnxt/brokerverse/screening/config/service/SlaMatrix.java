package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.SlaCalendar;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The SLA matrix of one configuration version (SNSRP-108, FR-SS-015).
 *
 * @param version the version the rows belong to
 * @param rules the SLA rows
 */
public record SlaMatrix(ConfigVersionRef version, List<Rule> rules) {

  /** Defensive copy. */
  public SlaMatrix {
    rules = List.copyOf(rules);
  }

  /**
   * The most specific row for a stage, case type and risk category (FR-SS-015 R1): rows whose blank
   * conditions match any value qualify; a row naming the case type and the risk category beats a
   * row naming one of them, which beats a row naming neither.
   *
   * @param stage the {@code SCR_CASE} stage
   * @param caseType the case type
   * @param riskCategory the risk category, may be {@code null}
   * @return the row, empty when the matrix has no row for the stage
   */
  public Optional<Rule> ruleFor(String stage, String caseType, String riskCategory) {
    return rules.stream()
        .filter(r -> r.stage().equals(stage))
        .filter(r -> r.caseType() == null || r.caseType().equals(caseType))
        .filter(r -> r.riskCategory() == null || r.riskCategory().equals(riskCategory))
        .max(Comparator.comparingInt(Rule::specificity));
  }

  /**
   * One SLA row.
   *
   * @param id the row id
   * @param stage the {@code SCR_CASE} stage
   * @param caseType condition on {@code SCR_CASE_TYPE}, {@code null} = any
   * @param riskCategory condition on the risk category, {@code null} = any
   * @param slaHours the SLA in hours (&gt; 0)
   * @param reminderLeadHours hours before the due time when the reminder goes out
   * @param escalateToRole the role notified on a breach
   * @param calendar calendar or working hours
   */
  public record Rule(
      Long id,
      String stage,
      String caseType,
      String riskCategory,
      int slaHours,
      int reminderLeadHours,
      String escalateToRole,
      SlaCalendar calendar) {

    /**
     * How specific the row is: 2 for the case type, 1 for the risk category.
     *
     * @return 0 to 3
     */
    public int specificity() {
      return (Objects.isNull(caseType) ? 0 : 2) + (Objects.isNull(riskCategory) ? 0 : 1);
    }
  }
}
