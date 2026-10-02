package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Programmes work list (design 10.1; FR-EB-021, FR-EB-022): the programmes of a company in the
 * tabs Renewal Due (flagged for renewal, a line expiring within {@code EB_RA_LEAD_DAYS} and no
 * renewal advice sent yet for it), In Progress, With Client, In Placement, Placed and Lost (by the
 * stage of the latest cycle), and All; searched by programme number, client code or name, or
 * programme name. Each row carries the latest cycle.
 */
@Service
@Transactional(readOnly = true)
public class ProgrammeQuery {

  private static final String LATEST_CYCLE =
      " left join lateral (select c.id, c.cycle_no, c.business_type, c.stage, c.policy_year,"
          + " c.closed_at from eb_cycle c where c.programme_id = p.id"
          + " order by (c.closed_at is null) desc, c.policy_year desc, c.id desc limit 1) lc on true";

  private static final String RENEWAL_DUE =
      "p.renewal_eligible and p.status = 'ACTIVE'"
          + " and exists (select 1 from eb_programme_line l where l.programme_id = p.id"
          + " and l.active and l.period_to between :today and :leadUntil)"
          + " and (lc.id is null or lc.closed_at is not null or lc.stage = 'OPEN')";

  private static final String WHERE =
      " from eb_programme p"
          + LATEST_CYCLE
          + " where p.company_id = :companyId"
          + " and (:tab <> 'RENEWAL_DUE' or ("
          + RENEWAL_DUE
          + "))"
          + " and (:tab <> 'IN_PROGRESS' or (lc.closed_at is null and lc.stage in ('OPEN',"
          + " 'RA_SENT', 'REQUIREMENTS', 'INCUMBENT_TERMS', 'FRANCHISE', 'PROPOSALS', 'COMPARATIVE',"
          + " 'FOR_SIGNOFF', 'THRESHOLD_APPROVAL', 'READY_TO_PRESENT', 'REVISION')))"
          + " and (:tab <> 'WITH_CLIENT' or lc.stage in ('WITH_CLIENT', 'CONFIRMED'))"
          + " and (:tab <> 'IN_PLACEMENT' or lc.stage = 'IN_PLACEMENT')"
          + " and (:tab <> 'PLACED' or lc.stage = 'PLACED')"
          + " and (:tab <> 'LOST' or p.status in ('LOST', 'LAPSED')"
          + " or lc.stage in ('CLOSED_LOST', 'NOT_RENEWED'))"
          + " and (cast(:stage as varchar) is null or (lc.closed_at is null and lc.stage = :stage))"
          + " and (cast(:ao as varchar) is null or lower(p.account_officer) = lower(:ao))"
          + " and (cast(:team as varchar) is null or p.team_code = :team)"
          + " and (cast(:q as varchar) is null or p.programme_no ilike :like"
          + " or p.client_code ilike :like or p.client_name ilike :like or p.name ilike :like)";

  private static final String SELECT =
      "select p.id, p.programme_no, p.client_id, p.client_code, p.client_name, p.name,"
          + " p.team_code, p.funding, p.account_officer, p.status, p.renewal_eligible,"
          + " (select string_agg(l.benefit_line, ', ' order by l.line_no) from eb_programme_line l"
          + " where l.programme_id = p.id and l.active) as lines,"
          + " (select min(l.period_to) from eb_programme_line l where l.programme_id = p.id"
          + " and l.active and l.period_to >= :today) as next_expiry,"
          + " lc.id as cycle_id, lc.cycle_no, lc.business_type, lc.stage, lc.policy_year,"
          + " (select r.sent_at from eb_renewal_advice r where r.cycle_id = lc.id) as ra_sent_at"
          + WHERE
          + " order by next_expiry nulls last, p.programme_no desc limit :size offset :offset";

  private final NamedParameterJdbcTemplate jdbc;
  private final EbParameters parameters;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc named-parameter JDBC
   * @param parameters renewal advice lead time
   * @param clock clock
   */
  public ProgrammeQuery(NamedParameterJdbcTemplate jdbc, EbParameters parameters, Clock clock) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * A page of the work list.
   *
   * @param companyId company
   * @param criteria tab, stage, AO, team and search text
   * @param page zero-based page
   * @param size page size
   * @return programmes
   */
  public PageResponse<ProgrammeRow> search(
      Long companyId, ProgrammeCriteria criteria, int page, int size) {
    Map<String, Object> args = args(companyId, criteria);
    args.put("size", size);
    args.put("offset", (long) page * size);
    List<ProgrammeRow> rows = jdbc.query(SELECT, args, (rs, n) -> row(rs));
    Long total = jdbc.queryForObject("select count(*)" + WHERE, args, Long.class);
    long count = total == null ? 0 : total;
    return new PageResponse<>(rows, page, size, count, (int) ((count + size - 1) / size));
  }

  /**
   * Number of programmes in a tab (EB Home tiles).
   *
   * @param companyId company
   * @param criteria tab and filters
   * @return count
   */
  public long count(Long companyId, ProgrammeCriteria criteria) {
    Long total =
        jdbc.queryForObject("select count(*)" + WHERE, args(companyId, criteria), Long.class);
    return total == null ? 0 : total;
  }

  private Map<String, Object> args(Long companyId, ProgrammeCriteria criteria) {
    LocalDate today = BusinessClock.today(clock);
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("today", today);
    args.put("leadUntil", today.plusDays(parameters.raLeadDays()));
    args.put("tab", criteria.tab() == null ? Tab.ALL.name() : criteria.tab().name());
    args.put("stage", blankToNull(criteria.stage()));
    args.put("ao", blankToNull(criteria.accountOfficer()));
    args.put("team", blankToNull(criteria.teamCode()));
    String q = blankToNull(criteria.text());
    args.put("q", q);
    args.put("like", q == null ? "" : "%" + escape(q) + "%");
    return args;
  }

  private static ProgrammeRow row(ResultSet rs) throws SQLException {
    Date expiry = rs.getDate("next_expiry");
    Timestamp raSent = rs.getTimestamp("ra_sent_at");
    long cycleId = rs.getLong("cycle_id");
    ProgrammeRow.CycleRef cycle =
        rs.wasNull()
            ? null
            : new ProgrammeRow.CycleRef(
                cycleId,
                rs.getString("cycle_no"),
                rs.getString("business_type"),
                rs.getString("stage"),
                rs.getInt("policy_year"),
                raSent == null ? null : raSent.toInstant());
    return new ProgrammeRow(
        rs.getLong("id"),
        rs.getString("programme_no"),
        rs.getLong("client_id"),
        rs.getString("client_code"),
        rs.getString("client_name"),
        rs.getString("name"),
        rs.getString("team_code"),
        rs.getString("funding"),
        rs.getString("account_officer"),
        rs.getString("status"),
        rs.getBoolean("renewal_eligible"),
        rs.getString("lines"),
        expiry == null ? null : expiry.toLocalDate(),
        cycle);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String escape(String text) {
    return text.toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  /** Tabs of the work list. */
  public enum Tab {
    /** Renewal advice due. */
    RENEWAL_DUE,
    /** Cycle in marketing. */
    IN_PROGRESS,
    /** With the client or confirmed. */
    WITH_CLIENT,
    /** Accounts in placement. */
    IN_PLACEMENT,
    /** Placed. */
    PLACED,
    /** Lost, lapsed or not renewed. */
    LOST,
    /** Every programme. */
    ALL
  }

  /**
   * Filters of the work list.
   *
   * @param tab tab, ALL when null
   * @param stage stage of the open cycle, may be null
   * @param accountOfficer AO user name, may be null
   * @param teamCode team, may be null
   * @param text programme number, client code or name, or programme name, may be null
   */
  public record ProgrammeCriteria(
      Tab tab, String stage, String accountOfficer, String teamCode, String text) {}
}
