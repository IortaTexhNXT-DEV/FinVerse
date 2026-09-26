package com.iortatechnxt.brokerverse.screening.risk.service;

import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Evaluates the risk rules of a configuration version for one client (SNSRP-102, 302; FR-SS-012 R1,
 * FR-SS-033). Pure functions.
 *
 * <p>Each rule tests one attribute. The rules are taken by ascending priority and the first rule
 * that holds decides the category:
 *
 * <ul>
 *   <li>A rule on MATCH_LIST_TYPE or MATCH_STATUS holds on one live match of the client (status
 *       POTENTIAL or TRUE_MATCH; a false positive never counts). TRUE_MATCH matches are tried
 *       first, so the rule and match kept in the history are the strongest evidence.
 *   <li>A rule on a client attribute (PEP, NATIONALITY, OCCUPATION, SOURCE_OF_FUNDS, CLIENT_TYPE,
 *       MARKET_SEGMENT) holds on the client alone. PEP is TRUE when the client carries the PEP tag.
 *   <li><b>Combinations</b> (design 17.2, SQ03): a category that has MATCH_STATUS rules as well as
 *       other rules requires, for its other rules, a match that also satisfies all its MATCH_STATUS
 *       rules. "SANCTION and TRUE_MATCH" is therefore category X with the rules MATCH_LIST_TYPE EQ
 *       SANCTION and MATCH_STATUS EQ TRUE_MATCH: it qualifies only once the sanction match is
 *       confirmed. A category with MATCH_STATUS rules only qualifies on any match in that status.
 * </ul>
 */
public final class RiskRuleEvaluator {

  /** Value of the PEP attribute for a client carrying the PEP tag. */
  public static final String TRUE = "TRUE";

  /** Value of the PEP attribute for any other client. */
  public static final String FALSE = "FALSE";

  /** The client tag that makes the PEP attribute TRUE. */
  public static final String PEP_TAG = "PEP";

  private static final String TRUE_MATCH = "TRUE_MATCH";

  private static final Map<RiskAttribute, Function<ClientFacts, String>> CLIENT_ATTRIBUTES =
      new EnumMap<>(
          Map.of(
              RiskAttribute.PEP, c -> c.tags().contains(PEP_TAG) ? TRUE : FALSE,
              RiskAttribute.NATIONALITY, ClientFacts::nationality,
              RiskAttribute.OCCUPATION, ClientFacts::occupation,
              RiskAttribute.SOURCE_OF_FUNDS, ClientFacts::sourceOfFunds,
              RiskAttribute.CLIENT_TYPE, ClientFacts::clientType,
              RiskAttribute.MARKET_SEGMENT, ClientFacts::marketSegment));

  private RiskRuleEvaluator() {}

  /**
   * The category a client qualifies for.
   *
   * @param rules the risk rules of a version
   * @param client the client's attributes
   * @param matches the client's live matches
   * @return the qualification, empty when no rule holds
   */
  public static Optional<Qualification> evaluate(
      RiskRules rules, ClientFacts client, List<MatchFacts> matches) {
    List<MatchFacts> ordered = new ArrayList<>(matches);
    ordered.sort(
        Comparator.comparing((MatchFacts m) -> !TRUE_MATCH.equals(m.status()))
            .thenComparing(MatchFacts::matchId));
    for (RiskRules.Rule rule : rules.rules()) {
      if (isQualifierOnly(rules, rule)) {
        continue;
      }
      List<RiskRules.Rule> qualifiers = qualifiers(rules, rule);
      Optional<Qualification> hit = holds(rules, rule, qualifiers, client, ordered);
      if (hit.isPresent()) {
        return hit;
      }
    }
    return Optional.empty();
  }

  /** A MATCH_STATUS rule of a category that also has other rules only qualifies those. */
  private static boolean isQualifierOnly(RiskRules rules, RiskRules.Rule rule) {
    return rule.attribute() == RiskAttribute.MATCH_STATUS
        && rules.rules().stream()
            .anyMatch(
                r ->
                    r.categoryCode().equals(rule.categoryCode())
                        && r.attribute() != RiskAttribute.MATCH_STATUS);
  }

  private static List<RiskRules.Rule> qualifiers(RiskRules rules, RiskRules.Rule rule) {
    if (rule.attribute() == RiskAttribute.MATCH_STATUS) {
      return List.of();
    }
    return rules.rules().stream()
        .filter(r -> r.categoryCode().equals(rule.categoryCode()))
        .filter(r -> r.attribute() == RiskAttribute.MATCH_STATUS)
        .toList();
  }

  private static Optional<Qualification> holds(
      RiskRules rules,
      RiskRules.Rule rule,
      List<RiskRules.Rule> qualifiers,
      ClientFacts client,
      List<MatchFacts> matches) {
    Optional<RiskRules.Category> category = rules.category(rule.categoryCode());
    if (category.isEmpty()) {
      return Optional.empty();
    }
    boolean onMatch = isMatchAttribute(rule.attribute()) || !qualifiers.isEmpty();
    if (!onMatch) {
      return rule.test(clientValues(rule.attribute(), client))
          ? Optional.of(new Qualification(category.get(), rule, null))
          : Optional.empty();
    }
    return matches.stream()
        .filter(m -> rule.test(values(rule.attribute(), client, m)))
        .filter(m -> qualifiers.stream().allMatch(q -> q.test(Set.of(m.status()))))
        .findFirst()
        .map(m -> new Qualification(category.get(), rule, m.matchId()));
  }

  private static boolean isMatchAttribute(RiskAttribute attribute) {
    return attribute == RiskAttribute.MATCH_LIST_TYPE || attribute == RiskAttribute.MATCH_STATUS;
  }

  private static Set<String> values(RiskAttribute attribute, ClientFacts client, MatchFacts m) {
    return switch (attribute) {
      case MATCH_LIST_TYPE -> Set.of(m.listType());
      case MATCH_STATUS -> Set.of(m.status());
      default -> clientValues(attribute, client);
    };
  }

  private static Set<String> clientValues(RiskAttribute attribute, ClientFacts client) {
    Function<ClientFacts, String> reader = CLIENT_ATTRIBUTES.get(attribute);
    String value = reader == null ? null : reader.apply(client);
    return value == null || value.isBlank() ? Set.of() : Set.of(value);
  }

  /**
   * The client attributes the rules test.
   *
   * @param clientType INDIVIDUAL or CORPORATE
   * @param nationality nationality
   * @param occupation occupation code
   * @param sourceOfFunds source of funds code
   * @param marketSegment market segment code
   * @param tags the client's active tags
   */
  public record ClientFacts(
      String clientType,
      String nationality,
      String occupation,
      String sourceOfFunds,
      String marketSegment,
      Set<String> tags) {

    /** Defensive copy. */
    public ClientFacts {
      tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
  }

  /**
   * One live match of the client.
   *
   * @param matchId the match
   * @param listType the entry's list type
   * @param status POTENTIAL or TRUE_MATCH
   */
  public record MatchFacts(Long matchId, String listType, String status) {}

  /**
   * The category a client qualified for.
   *
   * @param category the category
   * @param rule the rule that held
   * @param matchId the match it held on, {@code null} for a client attribute rule
   */
  public record Qualification(RiskRules.Category category, RiskRules.Rule rule, Long matchId) {}
}
