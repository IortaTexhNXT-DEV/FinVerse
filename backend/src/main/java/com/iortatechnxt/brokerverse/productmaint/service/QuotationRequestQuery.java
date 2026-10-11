package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Quotation Request List of Product Maintenance (BDOI FRS FRPM.002.02 and FRPM.005.01): the
 * quotation requests of non-package products raised by Marketing, with Request Number, Request
 * Date, Related Reference Number, Business Type, Assured's Name, Product Line, Account Officer,
 * Assigned TSU User, Submission Date, Effective and Expiry Date, the current status in BDOI's names
 * and the aging in days. Search, filter, sort and pagination run in the database.
 */
@Service
@Transactional(readOnly = true)
public class QuotationRequestQuery {

  /** Largest page. */
  public static final int MAX_PAGE = 200;

  private static final String SQL =
      "select * from (select p.id, p.prf_no, p.arn, p.renewal_of_ref, p.renewal_ref,"
          + " p.client_name, p.line_code, l.name as line_name, p.created_by, p.created_at,"
          + " p.submitted_at, p.period_from, p.period_to, p.status, p.terms_closed, c.assignee,"
          + " coalesce(c.stage_entered_at, p.created_at) as stage_entered_at, h.action as last_action"
          + " from npk_proposal p"
          + " left join cat_product_line l on l.code = p.line_code"
          + " left join wf_case c on c.entity_type = 'ProposalRequest'"
          + " and c.entity_id = cast(p.id as varchar)"
          + " left join lateral (select x.action from wf_case_history x where x.case_id = c.id"
          + " order by x.id desc limit 1) h on true"
          + " where p.company_id = :company"
          + " and (cast(:text as varchar) is null or lower(p.prf_no) like lower(:text)"
          + " or lower(p.arn) like lower(:text) or lower(p.client_name) like lower(:text))"
          + " and (cast(:line as varchar) is null or p.line_code = :line)"
          + " and (cast(:status as varchar) is null or p.status = :status)"
          + " and (cast(:officer as varchar) is null or lower(c.assignee) = lower(:officer))"
          + " and (cast(:open as boolean) is null or (p.status not in ('CONVERTED',"
          + " 'NOT_PROCEEDED', 'VOIDED')) = cast(:open as boolean))) q"
          + " order by case when :dir = 'desc' then null else"
          + " case :sort when 'requestNo' then prf_no when 'assured' then client_name"
          + " when 'line' then line_name when 'status' then status"
          + " when 'submittedAt' then to_char(submitted_at, 'YYYY-MM-DD HH24:MI:SS')"
          + " when 'aging' then to_char(stage_entered_at, 'YYYY-MM-DD HH24:MI:SS')"
          + " else to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') end end asc nulls last,"
          + " case when :dir = 'desc' then"
          + " case :sort when 'requestNo' then prf_no when 'assured' then client_name"
          + " when 'line' then line_name when 'status' then status"
          + " when 'submittedAt' then to_char(submitted_at, 'YYYY-MM-DD HH24:MI:SS')"
          + " when 'aging' then to_char(stage_entered_at, 'YYYY-MM-DD HH24:MI:SS')"
          + " else to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') end end desc nulls last, id desc"
          + " limit :limit offset :offset";

  private static final String COUNT_SQL =
      "select count(*) from npk_proposal p"
          + " left join wf_case c on c.entity_type = 'ProposalRequest'"
          + " and c.entity_id = cast(p.id as varchar)"
          + " where p.company_id = :company"
          + " and (cast(:text as varchar) is null or lower(p.prf_no) like lower(:text)"
          + " or lower(p.arn) like lower(:text) or lower(p.client_name) like lower(:text))"
          + " and (cast(:line as varchar) is null or p.line_code = :line)"
          + " and (cast(:status as varchar) is null or p.status = :status)"
          + " and (cast(:officer as varchar) is null or lower(c.assignee) = lower(:officer))"
          + " and (cast(:open as boolean) is null or (p.status not in ('CONVERTED',"
          + " 'NOT_PROCEEDED', 'VOIDED')) = cast(:open as boolean))";

  private final NamedParameterJdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   * @param clock clock
   */
  public QuotationRequestQuery(NamedParameterJdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * One page of the list.
   *
   * @param f filters and sort
   * @param page page (0 based)
   * @param size page size
   * @return rows and total
   */
  public QuotationPage search(Filter f, int page, int size) {
    int limit = Math.min(Math.max(size, 1), MAX_PAGE);
    String text = f.text() == null || f.text().isBlank() ? null : "%" + f.text().strip() + "%";
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("company", f.companyId())
            .addValue("text", text)
            .addValue("line", blank(f.lineCode()))
            .addValue("status", blank(f.status()))
            .addValue("officer", blank(f.tsuUser()))
            .addValue("open", f.open())
            .addValue("sort", f.sort() == null ? "" : f.sort())
            .addValue("dir", "asc".equals(f.direction()) ? "asc" : "desc")
            .addValue("limit", limit)
            .addValue("offset", (long) Math.max(page, 0) * limit);
    Long total = jdbc.queryForObject(COUNT_SQL, args, Long.class);
    LocalDate today = BusinessClock.today(clock);
    List<QuotationRow> rows = jdbc.query(SQL, args, (rs, i) -> row(rs, today));
    return new QuotationPage(rows, total == null ? 0 : total);
  }

  private static QuotationRow row(ResultSet rs, LocalDate today) throws SQLException {
    Timestamp submitted = rs.getTimestamp("submitted_at");
    Timestamp entered = rs.getTimestamp("stage_entered_at");
    String renewalOf = rs.getString("renewal_of_ref");
    String status = rs.getString("status");
    return new QuotationRow(
        rs.getLong("id"),
        rs.getString("prf_no"),
        BusinessClock.dateOf(rs.getTimestamp("created_at").toInstant()),
        renewalOf == null ? rs.getString("arn") : renewalOf,
        rs.getString("renewal_ref") == null ? "New Business" : "Renewal",
        rs.getString("client_name"),
        rs.getString("line_code"),
        rs.getString("line_name"),
        rs.getString("created_by"),
        rs.getString("assignee"),
        submitted == null ? null : submitted.toInstant(),
        date(rs.getDate("period_from")),
        date(rs.getDate("period_to")),
        status,
        PmStatusNames.quotation(status, rs.getString("last_action"), rs.getBoolean("terms_closed")),
        entered == null
            ? null
            : ChronoUnit.DAYS.between(BusinessClock.dateOf(entered.toInstant()), today));
  }

  private static LocalDate date(Date d) {
    return d == null ? null : d.toLocalDate();
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * Filters.
   *
   * @param companyId company
   * @param text request number, reference or assured's name contains
   * @param lineCode product line
   * @param status status code
   * @param tsuUser assigned TSU user
   * @param open true for open requests, false for closed, null for both
   * @param sort sort key (requestNo, assured, line, status, submittedAt, aging; default request
   *     date)
   * @param direction asc or desc (default desc)
   */
  public record Filter(
      Long companyId,
      String text,
      String lineCode,
      String status,
      String tsuUser,
      Boolean open,
      String sort,
      String direction) {}

  /**
   * A row of the list.
   *
   * @param id quotation request id
   * @param requestNo quotation request number
   * @param requestDate request date
   * @param referenceNo related reference number (account or expiring policy)
   * @param businessType New Business or Renewal
   * @param assuredName assured's name
   * @param lineCode product line code
   * @param productLine product line
   * @param accountOfficer Marketing account officer (username)
   * @param tsuUser assigned TSU user (username)
   * @param submittedAt submission date and time
   * @param effectiveDate period from
   * @param expiryDate period to
   * @param statusCode status code
   * @param status status name
   * @param agingDays days in the current stage
   */
  public record QuotationRow(
      Long id,
      String requestNo,
      LocalDate requestDate,
      String referenceNo,
      String businessType,
      String assuredName,
      String lineCode,
      String productLine,
      String accountOfficer,
      String tsuUser,
      Instant submittedAt,
      LocalDate effectiveDate,
      LocalDate expiryDate,
      String statusCode,
      String status,
      Long agingDays) {}

  /**
   * One page.
   *
   * @param rows rows
   * @param total rows of the filter
   */
  public record QuotationPage(List<QuotationRow> rows, long total) {}
}
