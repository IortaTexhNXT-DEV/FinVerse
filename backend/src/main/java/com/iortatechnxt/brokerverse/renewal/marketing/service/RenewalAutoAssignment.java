package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignmentRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatic assignment after the sanitation and the classification (FRRN.010.01, Annex F): a
 * renewal routed to Marketing goes to an Account Officer of its team (a Team Leader only when the
 * team has no other officer), and a renewal routed to Processing to a Processing Officer, the one
 * with the fewest open renewals first and in turn when they have as many (round robin and workload
 * balancing). Marketing assignment is switched per market segment by {@value #SEGMENTS} (none by
 * default: the Team Leader assigns, as the BRD reads); Processing by {@value #PROCESSING}.
 */
@Component
public class RenewalAutoAssignment {

  /** Parameter: market segments whose renewals are assigned automatically to an AO. */
  public static final String SEGMENTS = "RNW_AUTO_ASSIGN_SEGMENTS";

  /** Parameter: true to assign the renewals routed to Processing automatically. */
  public static final String PROCESSING = "RNW_AUTO_ASSIGN_PROCESSING";

  private static final String LOAD_SQL =
      "select o.officer,"
          + " (select count(*) from rnw_candidate c where c.company_id = :companyId"
          + "  and c.stage in (:stages)"
          + "  and (case when :role = 'AO' then c.assigned_ao else c.assigned_po end) = o.officer)"
          + "  as open_count,"
          + " (select max(x.id) from rnw_assignment x where x.username = o.officer"
          + "  and x.role = :role) as last_id"
          + " from unnest(cast(:officers as varchar[])) as o(officer)";

  private static final Set<String> MARKETING_OPEN =
      Set.of(RenewalStage.UNASSIGNED.name(), RenewalStage.FOR_DISPOSITION.name());
  private static final Set<String> PROCESSING_OPEN =
      Set.of(RenewalStage.FOR_PROCESSING.name(), RenewalStage.IN_PROCESSING.name());

  private final SystemParameterService parameters;
  private final SalesOrganisationService sales;
  private final AppUserRepository users;
  private final RenewalAssignmentRepository assignments;
  private final RenewalFlow flow;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the assignment.
   *
   * @param parameters system parameters
   * @param sales sales organisation (officers of a team)
   * @param users users
   * @param assignments assignment history
   * @param flow workflow
   * @param notices notifications
   * @param audit audit trail
   * @param jdbc workload of the officers
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RenewalAutoAssignment(
      SystemParameterService parameters,
      SalesOrganisationService sales,
      AppUserRepository users,
      RenewalAssignmentRepository assignments,
      RenewalFlow flow,
      RenewalNotices notices,
      AuditTrailService audit,
      NamedParameterJdbcTemplate jdbc) {
    this.parameters = parameters;
    this.sales = sales;
    this.users = users;
    this.assignments = assignments;
    this.flow = flow;
    this.notices = notices;
    this.audit = audit;
    this.jdbc = jdbc;
  }

  /**
   * Assigns a renewal just routed when the setting of its stage and segment asks for it.
   *
   * @param c renewal after routing
   * @return the officer assigned, empty when none
   */
  public Optional<String> afterRouting(RenewalCandidate c) {
    if (c.getStage() == RenewalStage.UNASSIGNED && marketingSegment(c)) {
      return marketing(c);
    }
    if (c.getStage() == RenewalStage.FOR_PROCESSING
        && c.getAssignedPo() == null
        && "true".equals(parameters.text(PROCESSING, "false").strip())) {
      return processing(c);
    }
    return Optional.empty();
  }

  private boolean marketingSegment(RenewalCandidate c) {
    var product = c.getSnapshot().product();
    String segment = product == null ? null : product.segment();
    return segment != null
        && parameters.items(SEGMENTS).stream()
            .map(String::strip)
            .anyMatch(s -> s.equals(segment) || "*".equals(s));
  }

  private Optional<String> marketing(RenewalCandidate c) {
    String unit = c.getOwnerUnit();
    Set<String> aos = Set.copyOf(users.findUsernamesWithPermission(Permission.RNW_DISPOSE));
    List<String> officers =
        sales.officers(c.getCompanyId()).stream()
            .filter(o -> unit != null && unit.equals(o.getTeamCode()))
            .map(SalesOfficer::getUsername)
            .filter(aos::contains)
            .filter(this::enabled)
            .sorted()
            .toList();
    Set<String> leaders = Set.copyOf(users.findUsernamesWithPermission(Permission.RNW_REVIEW));
    List<String> plain = officers.stream().filter(o -> !leaders.contains(o)).toList();
    Optional<String> ao =
        next(c.getCompanyId(), plain.isEmpty() ? officers : plain, RenewalAssignment.Role.AO);
    ao.ifPresent(
        officer -> {
          c.assignAo(officer);
          assignments.save(
              new RenewalAssignment(c.getId(), RenewalAssignment.Role.AO, officer, null, null));
          flow.system(c, "assign", "Assigned automatically to " + name(officer));
          flow.assign(c, officer);
          announce(c, officer, " assigned to you", "Give the disposition of the renewal of ");
        });
    return ao;
  }

  private Optional<String> processing(RenewalCandidate c) {
    List<String> officers =
        users.findUsernamesWithPermission(Permission.RNW_PROCESS).stream()
            .filter(this::enabled)
            .sorted()
            .toList();
    Optional<String> po = next(c.getCompanyId(), officers, RenewalAssignment.Role.PO);
    po.ifPresent(
        officer -> {
          c.assignPo(officer);
          assignments.save(
              new RenewalAssignment(c.getId(), RenewalAssignment.Role.PO, officer, null, null));
          flow.system(c, "assign_po", "Assigned automatically to " + name(officer));
          flow.assign(c, officer);
          announce(c, officer, " assigned to you for processing", "Process the renewal of ");
        });
    return po;
  }

  /**
   * The officer with the fewest open renewals; between equals, the one assigned least recently,
   * then by user name (round robin).
   */
  private Optional<String> next(Long companyId, List<String> officers, RenewalAssignment.Role r) {
    if (officers.isEmpty()) {
      return Optional.empty();
    }
    Map<String, Object> args = new HashMap<>();
    args.put("officers", officers.toArray(String[]::new));
    args.put("companyId", companyId);
    args.put("role", r.name());
    args.put("stages", r == RenewalAssignment.Role.AO ? MARKETING_OPEN : PROCESSING_OPEN);
    List<Load> loads =
        jdbc.query(
            LOAD_SQL,
            args,
            (rs, i) ->
                new Load(rs.getString("officer"), rs.getLong("open_count"), rs.getLong("last_id")));
    return loads.stream()
        .min(
            Comparator.comparingLong(Load::open)
                .thenComparingLong(Load::lastAt)
                .thenComparing(Load::officer))
        .map(Load::officer);
  }

  private void announce(RenewalCandidate c, String officer, String title, String body) {
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Assigned automatically to " + officer);
    notices.users(
        List.of(officer),
        RenewalCodes.EVENT_ASSIGNED,
        c,
        new RenewalNotices.Text(c.getRenewalRef() + title, body + c.getSnapshot().clientName()));
  }

  private boolean enabled(String username) {
    return users.findByUsernameIgnoreCase(username).map(AppUser::isEnabled).orElse(false);
  }

  private String name(String username) {
    return users.findByUsernameIgnoreCase(username).map(AppUser::getFullName).orElse(username);
  }

  private record Load(String officer, long open, long lastAt) {}
}
