package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.Balancing;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.AssignmentMatrix.Rule;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns a new case by the assignment matrix it started with (SNSRP-106; FR-SS-014): the first
 * scenario whose case type, trigger, risk category, marketing unit and client type match (blank =
 * any) names a user or a team role; among the eligible members of the role (SCR_INVESTIGATE, not
 * the client's account officer) ROUND_ROBIN picks the one who least recently received a case and
 * LEAST_OPEN the one with the fewest open cases; NONE leaves the case in the investigation queue.
 * The matrix is evaluated only when the case is created.
 */
@Component
@Transactional(readOnly = true)
public class CaseAssigner {

  private final ActiveConfig config;
  private final RoleMembers members;
  private final CaseRouter router;
  private final ScreeningCaseRepository cases;

  /**
   * Creates the assigner.
   *
   * @param config active configuration
   * @param members role holders
   * @param router eligibility
   * @param cases cases (balancing counts)
   */
  public CaseAssigner(
      ActiveConfig config, RoleMembers members, CaseRouter router, ScreeningCaseRepository cases) {
    this.config = config;
    this.members = members;
    this.router = router;
    this.cases = cases;
  }

  /**
   * The investigator of a new case.
   *
   * @param c the case
   * @return the assignee (null = investigation queue) and the scenario applied
   */
  public CaseRouter.Approver assign(ScreeningCase c) {
    Optional<Rule> rule = scenario(c);
    if (rule.isEmpty()) {
      return new CaseRouter.Approver(null, "No assignment scenario; investigation queue");
    }
    Rule r = rule.get();
    String scenario = "Scenario " + r.order();
    if (r.user() != null) {
      return router.eligible(c, r.user(), CaseCodes.INVESTIGATE)
          ? new CaseRouter.Approver(r.user(), scenario + ": user " + r.user())
          : new CaseRouter.Approver(null, scenario + ": " + r.user() + " not eligible; queue");
    }
    List<String> eligible =
        r.teamRole() == null
            ? List.of()
            : members.ofRole(r.teamRole()).stream()
                .filter(u -> router.eligible(c, u, CaseCodes.INVESTIGATE))
                .toList();
    String team = scenario + ": team " + r.teamRole() + " (" + r.balancing() + ")";
    if (eligible.isEmpty() || r.balancing() == Balancing.NONE) {
      return new CaseRouter.Approver(null, team + "; investigation queue");
    }
    String pick =
        r.balancing() == Balancing.LEAST_OPEN ? leastOpen(eligible) : leastRecent(eligible);
    return new CaseRouter.Approver(pick, team + " -> " + pick);
  }

  private Optional<Rule> scenario(ScreeningCase c) {
    if (c.getAssignmentVersionId() == null) {
      return Optional.empty();
    }
    return config.assignmentMatrix(c.getAssignmentVersionId()).rules().stream()
        .filter(r -> matches(r.caseType(), c.getCaseType()))
        .filter(r -> matches(r.trigger(), c.getTriggerCode()))
        .filter(r -> matches(r.riskCategory(), c.getRiskCategory()))
        .filter(r -> matches(r.marketingUnit(), c.getMarketingUnit()))
        .filter(r -> matches(r.clientType(), c.getClientType()))
        .findFirst();
  }

  private static boolean matches(String condition, String value) {
    return condition == null || condition.isBlank() || condition.equals(value);
  }

  private String leastOpen(List<String> users) {
    Map<String, Long> open = new HashMap<>();
    for (Object[] row : cases.openCountsOf(lower(users))) {
      open.put((String) row[0], (Long) row[1]);
    }
    return users.stream()
        .min(
            Comparator.<String>comparingLong(u -> open.getOrDefault(key(u), 0L))
                .thenComparing(Comparator.naturalOrder()))
        .orElseThrow();
  }

  private String leastRecent(List<String> users) {
    Map<String, Instant> last = new HashMap<>();
    for (Object[] row : cases.lastAssignedOf(lower(users))) {
      last.put((String) row[0], (Instant) row[1]);
    }
    return users.stream()
        .min(
            Comparator.<String, Instant>comparing(u -> last.getOrDefault(key(u), Instant.EPOCH))
                .thenComparing(Comparator.naturalOrder()))
        .orElseThrow();
  }

  private static List<String> lower(List<String> users) {
    return users.stream().map(CaseAssigner::key).toList();
  }

  private static String key(String user) {
    return user.toLowerCase(Locale.ROOT);
  }
}
