package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rows of the Consolidated Placement Update Report (BDOI FRS FRPM.007.01): every quotation
 * request active at the end of the reporting period or processed within it (created, or moved in
 * its workflow), with the insured, the marketing segment, the team and account officer, the TSU
 * handler, line and sub-line, the date and time the request was received, the aging in days and the
 * latest status. With the scope QUOTATION_AND_PACKAGE the package requests follow.
 */
@Service
@Transactional(readOnly = true)
public class PlacementUpdateQuery {

  /** Quotation requests only (BDOI's text). */
  public static final String QUOTATION = "QUOTATION";

  /** Quotation and package requests. */
  public static final String QUOTATION_AND_PACKAGE = "QUOTATION_AND_PACKAGE";

  private static final String COMMON =
      " left join sec_user u on lower(u.username) = lower(r.created_by)"
          + " left join lov_value tv on tv.type_code = 'UAM_BUSINESS_UNIT'"
          + " and tv.code = u.business_unit_code"
          + " left join cat_product_line l on l.code = r.line_code"
          + " left join wf_case c on c.entity_type = :entity and c.entity_id = cast(r.id as varchar)"
          + " left join lateral (select x.action from wf_case_history x where x.case_id = c.id"
          + " order by x.id desc limit 1) h on true";

  private static final String ACTIVE_OR_PROCESSED =
      " and r.created_at < :until"
          + " and (r.status not in (:closed) or r.created_at >= :since"
          + " or exists (select 1 from wf_case_history x where x.case_id = c.id"
          + " and x.occurred_at >= :since and x.occurred_at < :until))";

  private static final String QUOTATION_SQL =
      "select r.id, r.prf_no as reference, r.client_name as insured, sv.label as segment,"
          + " tv.label as team, r.created_by, c.assignee, l.name as line_name,"
          + " ct.name as sub_line, coalesce(r.submitted_at, r.created_at) as received_at,"
          + " r.status, r.terms_closed, h.action as last_action, c.stage_entered_at"
          + " from npk_proposal r"
          + " left join crm_client cl on cl.id = r.client_id"
          + " left join lov_value sv on sv.type_code = 'MARKET_SEGMENT'"
          + " and sv.code = cl.market_segment"
          + " left join cat_product p on p.code = r.product_code"
          + " left join cat_cover_type ct on ct.line_code = p.line_code"
          + " and ct.code = p.cover_type_code"
          + COMMON
          + " where r.company_id = :company"
          + ACTIVE_OR_PROCESSED
          + " order by received_at, r.id";

  private static final String PACKAGE_SQL =
      "select r.id, r.request_no as reference, coalesce(r.client_name, r.title) as insured,"
          + " r.market_segments as segment, tv.label as team, r.created_by, c.assignee,"
          + " l.name as line_name, ct.name as sub_line,"
          + " coalesce(r.submitted_at, r.created_at) as received_at, r.status,"
          + " false as terms_closed, h.action as last_action, c.stage_entered_at"
          + " from pm_request r"
          + " left join cat_cover_type ct on ct.line_code = r.line_code"
          + " and ct.code = r.cover_type_code"
          + COMMON
          + " where r.company_id = :company"
          + ACTIVE_OR_PROCESSED
          + " order by received_at, r.id";

  private static final List<String> QUOTATION_CLOSED =
      List.of("CONVERTED", "NOT_PROCEEDED", "VOIDED");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   */
  public PlacementUpdateQuery(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The rows of a reporting period, numbered from 1.
   *
   * @param companyId company
   * @param from first day of the period
   * @param to last day of the period
   * @param scope QUOTATION or QUOTATION_AND_PACKAGE
   * @return rows
   */
  public List<PlacementRow> rows(Long companyId, LocalDate from, LocalDate to, String scope) {
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("company", companyId)
            .addValue("since", java.sql.Timestamp.from(BusinessClock.startOf(from)))
            .addValue("until", java.sql.Timestamp.from(BusinessClock.startOf(to.plusDays(1))));
    List<PlacementRow> out = new ArrayList<>();
    args.addValue("entity", "ProposalRequest").addValue("closed", QUOTATION_CLOSED);
    out.addAll(jdbc.query(QUOTATION_SQL, args, (rs, i) -> row(rs, to, true)));
    if (QUOTATION_AND_PACKAGE.equals(scope)) {
      args.addValue("entity", PackageRequests.ENTITY)
          .addValue("closed", RequestStage.CLOSED.stream().map(RequestStage::name).toList());
      out.addAll(jdbc.query(PACKAGE_SQL, args, (rs, i) -> row(rs, to, false)));
    }
    List<PlacementRow> numbered = new ArrayList<>();
    for (int i = 0; i < out.size(); i++) {
      numbered.add(out.get(i).numbered(i + 1));
    }
    return numbered;
  }

  private static PlacementRow row(ResultSet rs, LocalDate to, boolean quotation)
      throws SQLException {
    ZonedDateTime received =
        rs.getTimestamp("received_at").toInstant().atZone(BusinessClock.zone());
    String status = rs.getString("status");
    String lastAction = rs.getString("last_action");
    return new PlacementRow(
        0,
        rs.getString("reference"),
        rs.getString("insured"),
        rs.getString("segment"),
        rs.getString("team"),
        rs.getString("created_by"),
        rs.getString("assignee"),
        rs.getString("line_name"),
        rs.getString("sub_line"),
        received.toLocalDate(),
        received.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
        Math.max(0, ChronoUnit.DAYS.between(received.toLocalDate(), to)),
        quotation
            ? PmStatusNames.quotation(status, lastAction, rs.getBoolean("terms_closed"))
            : PmStatusNames.packageRequest(RequestStage.valueOf(status), lastAction),
        stageEntered(rs.getTimestamp("stage_entered_at")));
  }

  private static LocalDate stageEntered(Timestamp t) {
    return t == null ? null : BusinessClock.dateOf(t.toInstant());
  }

  /**
   * A row of the report.
   *
   * @param itemNo item number
   * @param reference request number
   * @param insuredName insured's name
   * @param segment marketing segment
   * @param team team of the account officer
   * @param accountOfficer account officer (username)
   * @param tsuHandler TSU handler (username)
   * @param line line of insurance
   * @param subLine sub-line
   * @param receivedDate date the request was received
   * @param receivedTime time the request was received
   * @param agingDays days from the receipt to the end of the period
   * @param status latest status
   * @param statusSince date of the latest status
   */
  public record PlacementRow(
      int itemNo,
      String reference,
      String insuredName,
      String segment,
      String team,
      String accountOfficer,
      String tsuHandler,
      String line,
      String subLine,
      LocalDate receivedDate,
      LocalTime receivedTime,
      long agingDays,
      String status,
      LocalDate statusSince) {

    PlacementRow numbered(int no) {
      return new PlacementRow(
          no,
          reference,
          insuredName,
          segment,
          team,
          accountOfficer,
          tsuHandler,
          line,
          subLine,
          receivedDate,
          receivedTime,
          agingDays,
          status,
          statusSince);
    }
  }
}
