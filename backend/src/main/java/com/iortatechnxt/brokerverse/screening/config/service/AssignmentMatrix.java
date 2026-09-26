package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.Balancing;
import java.util.Comparator;
import java.util.List;

/**
 * The case assignment matrix of one configuration version (SNSRP-106, FR-SS-014). It is evaluated
 * only when a case is created; the first matching scenario wins. Blank ({@code null}) conditions
 * match any value.
 *
 * @param version the version the rules belong to
 * @param rules the scenarios, sorted by ascending order
 */
public record AssignmentMatrix(ConfigVersionRef version, List<Rule> rules) {

  /** Sorted copy. */
  public AssignmentMatrix {
    rules = rules.stream().sorted(Comparator.comparingInt(Rule::order)).toList();
  }

  /**
   * One scenario and its target: a team role (with balancing) or a named user.
   *
   * @param id the rule id
   * @param order evaluation order, unique in the version
   * @param caseType condition on {@code SCR_CASE_TYPE}, {@code null} = any
   * @param trigger condition on the screening trigger, {@code null} = any
   * @param riskCategory condition on the risk category, {@code null} = any
   * @param marketingUnit condition on the marketing unit code, {@code null} = any
   * @param clientType condition on the client type (INDIVIDUAL / CORPORATE), {@code null} = any
   * @param teamRole the role whose users share the cases, {@code null} when a user is named
   * @param user the named user, {@code null} when a team role is named
   * @param balancing the balancing among the team role's users
   */
  public record Rule(
      Long id,
      int order,
      String caseType,
      String trigger,
      String riskCategory,
      String marketingUnit,
      String clientType,
      String teamRole,
      String user,
      Balancing balancing) {}
}
