package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.config.domain.ApproverKind;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ApprovalMatrix.Route;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routes a case by the approval and escalation matrix it started with (SNSRP-103, 703; FR-SS-013):
 * the first route of the stage whose case type, risk category, marketing unit and disposition match
 * (blank = any) gives the next stage and the approver. A UNIT_HEAD route names the unit head of the
 * client's marketing unit, a USER route a user, a ROLE route the role's only eligible member (with
 * several members the case waits in the stage queue). An approver who does not hold the stage
 * permission, who investigated the case or who is the client's account officer is not assigned; the
 * case then waits in the queue of the stage (FR-SS-061 R1, FR-SS-043).
 */
@Component
@Transactional(readOnly = true)
public class CaseRouter {

  private final ActiveConfig config;
  private final RoleMembers members;

  /**
   * Creates the router.
   *
   * @param config active configuration
   * @param members role and permission holders
   */
  public CaseRouter(ActiveConfig config, RoleMembers members) {
    this.config = config;
    this.members = members;
  }

  /**
   * The route leaving a stage for a disposition.
   *
   * @param c the case
   * @param from the stage the case leaves
   * @param disposition the disposition, may be null
   * @return the first matching route, empty without a matrix or a matching route
   */
  public Optional<Route> route(ScreeningCase c, CaseStage from, String disposition) {
    if (c.getApprovalVersionId() == null) {
      return Optional.empty();
    }
    return config.approvalMatrix(c.getApprovalVersionId()).from(from.name()).stream()
        .filter(r -> matches(r.caseType(), c.getCaseType()))
        .filter(r -> matches(r.riskCategory(), c.getRiskCategory()))
        .filter(r -> matches(r.marketingUnit(), c.getMarketingUnit()))
        .filter(r -> matches(r.disposition(), disposition))
        .findFirst();
  }

  private static boolean matches(String condition, String value) {
    return condition == null || condition.isBlank() || condition.equals(value);
  }

  /**
   * The approver of the stage a route leads to.
   *
   * @param c the case
   * @param route the route, may be null (no route: the stage queue)
   * @param target the stage entered
   * @return the approver and how it was chosen
   */
  public Approver approver(ScreeningCase c, Route route, CaseStage target) {
    String permission = CaseAccess.ownerOf(target);
    if (route == null || route.approverKind() == null || permission == null) {
      return new Approver(null, "queue of " + target);
    }
    List<String> named = named(c, route.approverKind(), route.approverValue(), permission);
    List<String> eligible = named.stream().filter(u -> eligible(c, u, permission)).toList();
    if (eligible.size() == 1) {
      return new Approver(
          eligible.get(0), route.approverKind() + " " + describe(route) + " -> " + eligible.get(0));
    }
    return new Approver(
        null,
        route.approverKind()
            + " "
            + describe(route)
            + (named.isEmpty() ? ": nobody named" : ": " + String.join(", ", named))
            + "; the case waits in the queue of "
            + target);
  }

  private List<String> named(ScreeningCase c, ApproverKind kind, String value, String permission) {
    return switch (kind) {
      case USER -> value == null ? List.of() : List.of(value);
      case UNIT_HEAD -> c.getUnitHead() == null ? List.of() : List.of(c.getUnitHead());
      case ROLE ->
          value == null
              ? List.of()
              : members.ofRole(value).stream().filter(u -> members.holds(u, permission)).toList();
    };
  }

  /**
   * Whether a user may take the case in a stage: holds the stage permission, did not investigate it
   * and is not the client's account officer.
   *
   * @param c the case
   * @param user the user
   * @param permission the stage permission
   * @return true when eligible
   */
  public boolean eligible(ScreeningCase c, String user, String permission) {
    return members.holds(user, permission)
        && !CurrentUser.sameUser(user, c.getAccountOfficer())
        && (Objects.equals(permission, CaseCodes.INVESTIGATE)
            || !CurrentUser.sameUser(user, c.getInvestigator()));
  }

  private static String describe(Route route) {
    return route.approverValue() == null ? "(route " + route.order() + ")" : route.approverValue();
  }

  /**
   * The approver chosen for a stage.
   *
   * @param user the user assigned, null when the case waits in the stage queue
   * @param note how it was chosen (for the timeline)
   */
  public record Approver(String user, String note) {}
}
