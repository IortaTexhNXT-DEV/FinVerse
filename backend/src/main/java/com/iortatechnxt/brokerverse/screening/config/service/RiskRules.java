package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The risk-profile categories and tagging rules of one configuration version (SNSRP-102,
 * FR-SS-012). Rules are held in priority order; the first rule that matches decides the category.
 *
 * @param version the version the categories and rules belong to
 * @param categories the risk categories
 * @param rules the rules, sorted by ascending priority
 */
public record RiskRules(ConfigVersionRef version, List<Category> categories, List<Rule> rules) {

  /** Defensive copies; the rules are sorted by priority. */
  public RiskRules {
    categories = List.copyOf(categories);
    rules = rules.stream().sorted(Comparator.comparingInt(Rule::priority)).toList();
  }

  /**
   * Finds a category of this version.
   *
   * @param code the category code
   * @return the category, empty when the version does not define it
   */
  public Optional<Category> category(String code) {
    return categories.stream().filter(c -> c.code().equals(code)).findFirst();
  }

  /**
   * A risk-profile category: the client risk rating it sets, the client tags it adds, the case type
   * it opens and whether it requires EDD.
   *
   * @param code unique code in the version
   * @param name display name
   * @param tier 1 = highest risk
   * @param kycRiskRating a {@code KYC_RISK_RATING} code (LOW, STANDARD, HIGH ...)
   * @param tags {@code CLIENT_TAG} codes to add (PEP, WATCHLIST_REVIEW)
   * @param caseType the {@code SCR_CASE_TYPE} to open, {@code null} = no case
   * @param requiresEdd whether an EDD case is required when the client has an active policy
   */
  public record Category(
      String code,
      String name,
      int tier,
      String kycRiskRating,
      Set<String> tags,
      String caseType,
      boolean requiresEdd) {

    /** Defensive copy. */
    public Category {
      tags = Set.copyOf(tags);
    }
  }

  /**
   * A tagging rule: when {@code attribute} satisfies {@code operator values}, the client gets
   * {@code categoryCode}.
   *
   * @param id the rule id, kept on the client risk-profile history (FR-SS-012 R1)
   * @param priority evaluation order, unique in the version
   * @param categoryCode the category assigned
   * @param attribute the attribute tested
   * @param operator EQ, IN or NOT_IN
   * @param values the codes compared (at least one)
   */
  public record Rule(
      Long id,
      int priority,
      String categoryCode,
      RiskAttribute attribute,
      RuleOperator operator,
      Set<String> values) {

    /** Defensive copy. */
    public Rule {
      values = Set.copyOf(values);
    }

    /**
     * Tests the rule against the values the subject has for {@link #attribute()} (one value for
     * most attributes; several for example for the list types of several matches). EQ and IN hold
     * when any actual value is one of the rule values; NOT_IN holds when none is.
     *
     * @param actual the subject's values of the attribute (empty when unknown)
     * @return whether the condition holds
     */
    public boolean test(Collection<String> actual) {
      boolean any = actual.stream().anyMatch(values::contains);
      return operator == RuleOperator.NOT_IN ? !any : any;
    }
  }
}
