package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationStatus;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Product Maintenance dashboard (BDOI FRS FRPM.001.01): the KPIs Incoming Requests, In-Progress
 * Requests, For Approval, Expiring Packages, Issued Proposals and Deactivation Requests for the
 * filters Period From and To, TSU Officer, Product Line and Package Type, with the drill-down rows
 * (Request Number, Request Type, Product Line, Requested By, Assigned TSU Officer, Current Status,
 * Submission Date and Aging, oldest first). Users of TSU, MBS and management see every request;
 * other users (Marketing) see the requests they raised or hold.
 */
@Service
@Transactional(readOnly = true)
public class PmDashboardQuery {

  /** The KPIs in screen order. */
  public static final List<Kpi> KPIS = List.of(Kpi.values());

  /** Largest page of the drill-down. */
  public static final int MAX_PAGE = 200;

  /** Largest export per KPI. */
  public static final int MAX_EXPORT = 5000;

  private static final int DEFAULT_NOTICE = 90;
  private static final int DEFAULT_PERIOD = 90;

  private static final List<String> SEE_ALL =
      List.of("PKG_REPORT_VIEW", "PKG_NEGOTIATE", "PKG_TSU_APPROVE", "PRODUCT_MAINTAIN");

  private static final String ITEMS =
      "with items as ("
          + "select 'PACKAGE' as kind, r.id, r.request_no, r.request_type as type_code, r.line_code,"
          + " r.created_by as requested_by, c.assignee, r.status, h.action as last_action,"
          + " false as terms_closed, r.submitted_at, coalesce(c.stage_entered_at, r.created_at)"
          + " as stage_entered_at, cast(null as date) as expiry_date, r.submitted_at as incoming_at,"
          + " r.terms_final_at as issued_at,"
          + " r.status in ('FOR_TSU_REVIEW', 'NEGOTIATION', 'TERMS_REVIEW', 'REQUIREMENTS_PREP',"
          + " 'WITH_MBS') as in_progress,"
          + " r.status in ('FOR_MKT_APPROVAL', 'FOR_TSU_APPROVAL', 'FOR_MKT_REVIEW', 'FOR_MANCOM',"
          + " 'FOR_VALIDATION') as for_approval, false as expiring,"
          + " r.request_type = 'RETIRE' and r.status not in ('DRAFT', 'RELEASED', 'RETIRED',"
          + " 'NOT_PROCEEDED', 'VOIDED') as deactivation"
          + " from pm_request r"
          + " left join wf_case c on c.entity_type = 'PackageRequest'"
          + " and c.entity_id = cast(r.id as varchar)"
          + " left join lateral (select x.action from wf_case_history x where x.case_id = c.id"
          + " order by x.id desc limit 1) h on true"
          + " where r.company_id = :company"
          + " union all "
          + "select 'QUOTATION', p.id, p.prf_no, case when p.renewal_ref is null"
          + " then 'NEW_BUSINESS' else 'RENEWAL' end, p.line_code, p.created_by, c.assignee,"
          + " p.status, h.action, p.terms_closed, p.submitted_at,"
          + " coalesce(c.stage_entered_at, p.created_at), cast(null as date), p.submitted_at,"
          + " (select max(y.occurred_at) from wf_case_history y where y.case_id = c.id"
          + " and y.action = 'approve_ps'),"
          + " p.status in ('WITH_TSU', 'QS_PREPARATION', 'QS_SENT', 'TERMS_RECEIVED'),"
          + " p.status in ('FOR_MKT_APPROVAL', 'QS_FOR_APPROVAL', 'PS_FOR_APPROVAL'), false, false"
          + " from npk_proposal p"
          + " left join wf_case c on c.entity_type = 'ProposalRequest'"
          + " and c.entity_id = cast(p.id as varchar)"
          + " left join lateral (select x.action from wf_case_history x where x.case_id = c.id"
          + " order by x.id desc limit 1) h on true"
          + " where p.company_id = :company"
          + " union all "
          + "select 'DEACTIVATION', d.id, d.request_no, 'DEACTIVATION', pr.line_code, d.created_by,"
          + " d.approver, d.status, null, false, d.created_at, d.created_at, d.effective_date, null,"
          + " null, false, false, false, d.status = 'PENDING'"
          + " from pm_deactivation_request d join cat_product pr on pr.code = d.product_code"
          + " where d.company_id = :company"
          + " union all "
          + "select 'EXPIRY', null, v.product_code, 'EXPIRY', pr.line_code, null, null, 'EXPIRING',"
          + " null, false, null, null, v.package_end_date, null, null, false, false, true, false"
          + " from cat_product_version v join cat_product pr on pr.code = v.product_code"
          + " where v.status = 'RELEASED' and v.effective_to is null"
          + " and pr.lifecycle_status = 'ACTIVE'"
          + " and v.package_end_date between :today and :until)";

  private static final String FILTERS =
      " where (cast(:officer as varchar) is null or lower(i.assignee) = lower(:officer))"
          + " and (cast(:line as varchar) is null or i.line_code = :line)"
          + " and (cast(:packaged as boolean) is null"
          + " or (i.kind = 'QUOTATION') = not cast(:packaged as boolean))"
          + " and (:all or lower(i.requested_by) = lower(:me) or lower(i.assignee) = lower(:me)"
          + " or i.kind = 'EXPIRY')";

  private static final String INCOMING = "i.incoming_at >= :from and i.incoming_at < :to";
  private static final String ISSUED = "i.issued_at >= :from and i.issued_at < :to";

  private static final String COUNTS_SQL =
      ITEMS
          + " select count(*) filter (where "
          + INCOMING
          + ") as incoming, count(*) filter (where i.in_progress) as in_progress,"
          + " count(*) filter (where i.for_approval) as for_approval,"
          + " count(*) filter (where i.expiring) as expiring,"
          + " count(*) filter (where "
          + ISSUED
          + ") as issued, count(*) filter (where i.deactivation) as deactivation"
          + " from items i"
          + FILTERS;

  private static final String KPI_FILTER =
      " and case :kpi when 'INCOMING' then coalesce("
          + INCOMING
          + ", false) when 'IN_PROGRESS' then i.in_progress"
          + " when 'FOR_APPROVAL' then i.for_approval when 'EXPIRING' then i.expiring"
          + " when 'ISSUED' then coalesce("
          + ISSUED
          + ", false) when 'DEACTIVATION' then i.deactivation else false end";

  private static final String ROWS_SQL =
      ITEMS
          + " select i.*, l.name as line_name, count(*) over () as total from items i"
          + " left join cat_product_line l on l.code = i.line_code"
          + FILTERS
          + KPI_FILTER
          + " order by coalesce(i.stage_entered_at, cast(i.expiry_date as timestamptz)) asc,"
          + " i.request_no limit :limit offset :offset";

  private static final Map<String, String> TYPES =
      Map.ofEntries(
          Map.entry("NEW", "New Package"),
          Map.entry("AMEND", "Update Package (terms)"),
          Map.entry("UPDATE", "Update Package"),
          Map.entry("RENEW", "Renew Package"),
          Map.entry("RETIRE", "Retire Package"),
          Map.entry("REACTIVATE", "Reactivate Package"),
          Map.entry("NEW_BUSINESS", "Quotation Request - New Business"),
          Map.entry("RENEWAL", "Quotation Request - Renewal"),
          Map.entry("DEACTIVATION", "Package Deactivation"),
          Map.entry("EXPIRY", "Package Expiry"));

  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   * @param currentUser the viewer (what the user may see)
   * @param parameters business parameters (expiry notice days)
   * @param clock clock
   */
  public PmDashboardQuery(
      NamedParameterJdbcTemplate jdbc,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The KPI counts of the filters.
   *
   * @param f filters
   * @return counts
   */
  public Counts counts(Filter f) {
    return jdbc.queryForObject(
        COUNTS_SQL,
        args(f),
        (rs, i) ->
            new Counts(
                rs.getLong("incoming"),
                rs.getLong("in_progress"),
                rs.getLong("for_approval"),
                rs.getLong("expiring"),
                rs.getLong("issued"),
                rs.getLong("deactivation")));
  }

  /**
   * The drill-down rows of one KPI, oldest first.
   *
   * @param f filters
   * @param kpi KPI
   * @param page page (0 based)
   * @param size page size
   * @return rows and total
   */
  public DrillPage rows(Filter f, Kpi kpi, int page, int size) {
    int limit = Math.min(Math.max(size, 1), MAX_PAGE);
    MapSqlParameterSource args =
        args(f)
            .addValue("kpi", kpi.name())
            .addValue("limit", limit)
            .addValue("offset", (long) Math.max(page, 0) * limit);
    long[] total = {0};
    List<DrillRow> rows =
        jdbc.query(
            ROWS_SQL,
            args,
            (rs, i) -> {
              total[0] = rs.getLong("total");
              return row(rs);
            });
    return new DrillPage(rows, total[0]);
  }

  /**
   * Every drill-down row of one KPI (export), up to {@value #MAX_EXPORT}.
   *
   * @param f filters
   * @param kpi KPI
   * @return rows
   */
  public List<DrillRow> all(Filter f, Kpi kpi) {
    return rows(f, kpi, 0, MAX_EXPORT).rows();
  }

  private MapSqlParameterSource args(Filter f) {
    LocalDate today = BusinessClock.today(clock);
    LocalDate from = f.from() == null ? today.minusDays(DEFAULT_PERIOD) : f.from();
    LocalDate to = f.to() == null ? today : f.to();
    int notice = parameters.intValue("PACKAGE_EXPIRY_NOTICE_DAYS", DEFAULT_NOTICE);
    boolean all = SEE_ALL.stream().anyMatch(currentUser::hasAuthority);
    return new MapSqlParameterSource()
        .addValue("company", f.companyId())
        .addValue("today", today)
        .addValue("until", today.plusDays(notice))
        .addValue("from", Timestamp.from(BusinessClock.startOf(from)))
        .addValue("to", Timestamp.from(BusinessClock.startOf(to.plusDays(1))))
        .addValue("officer", blank(f.tsuOfficer()))
        .addValue("line", blank(f.lineCode()))
        .addValue("packaged", f.packaged())
        .addValue("all", all)
        .addValue("me", currentUser.username());
  }

  private DrillRow row(ResultSet rs) throws SQLException {
    String kind = rs.getString("kind");
    String status = rs.getString("status");
    Timestamp entered = rs.getTimestamp("stage_entered_at");
    Timestamp submitted = rs.getTimestamp("submitted_at");
    Date expiry = rs.getDate("expiry_date");
    Object id = rs.getObject("id");
    LocalDate today = BusinessClock.today(clock);
    return new DrillRow(
        kind,
        id == null ? null : ((Number) id).longValue(),
        rs.getString("request_no"),
        TYPES.getOrDefault(rs.getString("type_code"), rs.getString("type_code")),
        rs.getString("line_code"),
        rs.getString("line_name"),
        rs.getString("requested_by"),
        rs.getString("assignee"),
        statusName(kind, status, rs.getString("last_action"), rs.getBoolean("terms_closed")),
        submitted == null ? null : submitted.toInstant(),
        entered == null
            ? null
            : ChronoUnit.DAYS.between(BusinessClock.dateOf(entered.toInstant()), today),
        expiry == null ? null : expiry.toLocalDate());
  }

  private static String statusName(String kind, String status, String action, boolean closed) {
    return switch (kind) {
      case "PACKAGE" -> PmStatusNames.packageRequest(RequestStage.valueOf(status), action);
      case "QUOTATION" -> PmStatusNames.quotation(status, action, closed);
      case "DEACTIVATION" -> DeactivationStatus.valueOf(status).label();
      default -> "Expiring";
    };
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /** The KPIs of the dashboard. */
  public enum Kpi {
    /** Package and quotation requests received in the period. */
    INCOMING("Incoming Requests"),
    /** Requests being worked on by TSU. */
    IN_PROGRESS("In-Progress Requests"),
    /** Requests pending approval. */
    FOR_APPROVAL("For Approval"),
    /** Packages approaching expiry. */
    EXPIRING("Expiring Packages"),
    /** Proposals released to Marketing in the period. */
    ISSUED("Issued Proposals"),
    /** Deactivation requests awaiting approval. */
    DEACTIVATION("Deactivation Requests");

    private final String label;

    Kpi(String label) {
      this.label = label;
    }

    /**
     * Business label.
     *
     * @return label
     */
    public String label() {
      return label;
    }
  }

  /**
   * Dashboard filters.
   *
   * @param companyId company
   * @param from period from (default 90 days before today)
   * @param to period to (default today)
   * @param tsuOfficer assigned TSU officer (username)
   * @param lineCode product line
   * @param packaged true for packages, false for non-package quotation requests, null for both
   */
  public record Filter(
      Long companyId,
      LocalDate from,
      LocalDate to,
      String tsuOfficer,
      String lineCode,
      Boolean packaged) {}

  /**
   * KPI counts.
   *
   * @param incoming incoming requests
   * @param inProgress in-progress requests
   * @param forApproval requests for approval
   * @param expiring expiring packages
   * @param issued issued proposals
   * @param deactivation deactivation requests awaiting approval
   */
  public record Counts(
      long incoming,
      long inProgress,
      long forApproval,
      long expiring,
      long issued,
      long deactivation) {}

  /**
   * A drill-down row.
   *
   * @param kind PACKAGE, QUOTATION, DEACTIVATION or EXPIRY (the record to open)
   * @param id record id (null for an expiring package)
   * @param requestNo request number (product code for an expiring package)
   * @param requestType request type
   * @param lineCode product line code
   * @param productLine product line name
   * @param requestedBy requestor (username)
   * @param assignee assigned TSU officer or approver (username)
   * @param status current status
   * @param submittedAt submission date and time
   * @param agingDays days in the current stage
   * @param expiryDate expiry date (expiring packages) or effective date (deactivations)
   */
  public record DrillRow(
      String kind,
      Long id,
      String requestNo,
      String requestType,
      String lineCode,
      String productLine,
      String requestedBy,
      String assignee,
      String status,
      Instant submittedAt,
      Long agingDays,
      LocalDate expiryDate) {}

  /**
   * A page of drill-down rows.
   *
   * @param rows rows
   * @param total rows of the KPI
   */
  public record DrillPage(List<DrillRow> rows, long total) {}
}
