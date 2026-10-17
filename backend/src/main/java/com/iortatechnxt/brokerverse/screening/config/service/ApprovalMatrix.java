package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ApproverKind;
import java.util.Comparator;
import java.util.List;

/**
 * The approval and escalation matrix of one configuration version (SNSRP-103, 703, FR-SS-013).
 * Routes are held in their configured order; blank ({@code null}) conditions match any value.
 *
 * @param version the version the routes belong to
 * @param routes the routes, sorted by ascending order
 */
public record ApprovalMatrix(ConfigVersionRef version, List<Route> routes) {

  /** Sorted copy. */
  public ApprovalMatrix {
    routes = routes.stream().sorted(Comparator.comparingInt(Route::order)).toList();
  }

  /**
   * The routes leaving a stage, in order.
   *
   * @param fromStage the {@code SCR_CASE} stage
   * @return the routes of that stage
   */
  public List<Route> from(String fromStage) {
    return routes.stream().filter(r -> r.fromStage().equals(fromStage)).toList();
  }

  /**
   * One route: from a stage, for the given conditions, to a stage and approver.
   *
   * @param id the route id
   * @param order evaluation order, unique in the version
   * @param fromStage the stage the case leaves
   * @param caseType condition on {@code SCR_CASE_TYPE}, {@code null} = any
   * @param riskCategory condition on the risk category, {@code null} = any
   * @param marketingUnit condition on the marketing unit code, {@code null} = any
   * @param disposition condition on the {@code SCR_DISPOSITION} code, {@code null} = any
   * @param toStage the next stage
   * @param approverKind how the approver is named, {@code null} for a stage without approver
   * @param approverValue the role code or user name for ROLE / USER
   */
  public record Route(
      Long id,
      int order,
      String fromStage,
      String caseType,
      String riskCategory,
      String marketingUnit,
      String disposition,
      String toStage,
      ApproverKind approverKind,
      String approverValue) {}
}
