package com.iortatechnxt.brokerverse.screening;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.ClientFacts;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.MatchFacts;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The risk rule evaluator (SNSRP-102, 302; FR-SS-012 R1, FR-SS-033): first rule by priority,
 * TRUE_MATCH evidence first, client attribute rules, and MATCH_STATUS rules qualifying the other
 * rules of their category (design 17.2, SQ03).
 */
class RiskRuleEvaluatorTest {

  private static final String SANCTION = "SANCTION";
  private static final String PEP = "PEP";
  private static final String FILIPINO = "FILIPINO";

  private static RiskRules rules(List<RiskRules.Rule> rules) {
    return new RiskRules(
        new ConfigVersionRef(2L, ConfigType.RISK_RULES, null, 1, LocalDate.of(2026, 1, 1)),
        List.of(
            new RiskRules.Category(
                "HIGH_SANCTION",
                "High",
                1,
                "HIGH",
                Set.of("WATCHLIST_REVIEW"),
                "NAME_MATCH",
                false),
            new RiskRules.Category("PEP", "PEP", 2, "HIGH", Set.of("PEP"), "PEP", true),
            new RiskRules.Category(
                "CONFIRMED_SANCTION",
                "Confirmed",
                1,
                "HIGH",
                Set.of("WATCHLIST_REVIEW"),
                "HIGH_RISK",
                true),
            new RiskRules.Category("STATUS_ONLY", "Status", 3, "STANDARD", null, null, false),
            new RiskRules.Category("FOREIGN", "Foreign", 3, "STANDARD", null, null, false)),
        rules);
  }

  private static RiskRules.Rule risk(
      long id,
      int priority,
      String category,
      RiskAttribute attr,
      RuleOperator op,
      String... values) {
    return new RiskRules.Rule(id, priority, category, attr, op, Set.of(values));
  }

  private static final ClientFacts CLIENT =
      new ClientFacts("INDIVIDUAL", FILIPINO, null, null, "CBG", Set.of());

  @Test
  void theFirstRuleByPriorityDecidesAndTrueMatchesComeFirst() {
    RiskRules demo =
        rules(
            List.of(
                risk(
                    1,
                    10,
                    "HIGH_SANCTION",
                    RiskAttribute.MATCH_LIST_TYPE,
                    RuleOperator.EQ,
                    SANCTION),
                risk(2, 20, "PEP", RiskAttribute.PEP, RuleOperator.EQ, "TRUE"),
                risk(3, 30, "PEP", RiskAttribute.MATCH_LIST_TYPE, RuleOperator.EQ, PEP)));
    List<MatchFacts> matches =
        List.of(
            new MatchFacts(5L, SANCTION, "POTENTIAL"), new MatchFacts(7L, SANCTION, "TRUE_MATCH"));
    RiskRuleEvaluator.Qualification q =
        RiskRuleEvaluator.evaluate(demo, CLIENT, matches).orElseThrow();
    assertThat(q.category().code()).isEqualTo("HIGH_SANCTION");
    assertThat(q.matchId()).isEqualTo(7L);

    Optional<RiskRuleEvaluator.Qualification> pepTag =
        RiskRuleEvaluator.evaluate(
            demo,
            new ClientFacts("INDIVIDUAL", FILIPINO, null, null, null, Set.of("PEP")),
            List.of());
    assertThat(pepTag.orElseThrow().category().code()).isEqualTo("PEP");
    assertThat(pepTag.get().matchId()).isNull();
    assertThat(RiskRuleEvaluator.evaluate(demo, CLIENT, List.of())).isEmpty();
    assertThat(
            RiskRuleEvaluator.evaluate(demo, CLIENT, List.of(new MatchFacts(9L, PEP, "POTENTIAL")))
                .orElseThrow()
                .rule()
                .id())
        .isEqualTo(3L);
  }

  @Test
  void aMatchStatusRuleQualifiesTheOtherRulesOfItsCategory() {
    RiskRules combined =
        rules(
            List.of(
                risk(
                    1,
                    10,
                    "CONFIRMED_SANCTION",
                    RiskAttribute.MATCH_LIST_TYPE,
                    RuleOperator.EQ,
                    SANCTION),
                risk(
                    2,
                    20,
                    "CONFIRMED_SANCTION",
                    RiskAttribute.MATCH_STATUS,
                    RuleOperator.EQ,
                    "TRUE_MATCH"),
                risk(
                    3, 30, "STATUS_ONLY", RiskAttribute.MATCH_STATUS, RuleOperator.IN, "POTENTIAL"),
                risk(4, 40, "FOREIGN", RiskAttribute.NATIONALITY, RuleOperator.NOT_IN, FILIPINO)));
    assertThat(
            RiskRuleEvaluator.evaluate(
                    combined, CLIENT, List.of(new MatchFacts(1L, SANCTION, "POTENTIAL")))
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("STATUS_ONLY");
    assertThat(
            RiskRuleEvaluator.evaluate(
                    combined, CLIENT, List.of(new MatchFacts(2L, SANCTION, "TRUE_MATCH")))
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("CONFIRMED_SANCTION");
    assertThat(
            RiskRuleEvaluator.evaluate(
                combined, CLIENT, List.of(new MatchFacts(3L, PEP, "TRUE_MATCH"))))
        .isEmpty();
    ClientFacts foreigner =
        new ClientFacts("INDIVIDUAL", "AMERICAN", "Trader", "BUSINESS", "RETAIL", null);
    assertThat(
            RiskRuleEvaluator.evaluate(combined, foreigner, List.of())
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("FOREIGN");
    RiskRules unknownCategory =
        rules(
            List.of(risk(9, 1, "NOPE", RiskAttribute.CLIENT_TYPE, RuleOperator.EQ, "INDIVIDUAL")));
    assertThat(RiskRuleEvaluator.evaluate(unknownCategory, CLIENT, List.of())).isEmpty();
    RiskRules byAttributes =
        rules(
            List.of(
                risk(5, 1, "FOREIGN", RiskAttribute.OCCUPATION, RuleOperator.EQ, "Trader"),
                risk(6, 2, "FOREIGN", RiskAttribute.SOURCE_OF_FUNDS, RuleOperator.EQ, "BUSINESS"),
                risk(7, 3, "FOREIGN", RiskAttribute.MARKET_SEGMENT, RuleOperator.EQ, "RETAIL")));
    assertThat(
            RiskRuleEvaluator.evaluate(byAttributes, foreigner, List.of())
                .orElseThrow()
                .rule()
                .id())
        .isEqualTo(5L);
  }
}
