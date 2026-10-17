package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Product Matrix (BDOI FRS FRPM.002.02 and FRPM.003.01): every maintained product with its Line
 * of Insurance, Sub-Line, Package Name and Description, insurers, status, effective and expiry
 * dates and who updated it last, in three tabs: Active Products, Expiring Products (active, ending
 * within PACKAGE_EXPIRY_NOTICE_DAYS, 90 days) and Expired Products (expired or deactivated).
 * Search, filter, sort and pagination run in the database.
 */
@Service
@Transactional(readOnly = true)
public class ProductMatrixQuery {

  /** Largest page. */
  public static final int MAX_PAGE = 200;

  /** Largest export. */
  public static final int MAX_EXPORT = 5000;

  private static final String NOTICE_DAYS = "PACKAGE_EXPIRY_NOTICE_DAYS";
  private static final int DEFAULT_NOTICE = 90;
  private static final String EXPIRING = "EXPIRING";
  private static final String EXPIRED = "EXPIRED";

  private static final String BASE =
      "select p.code, p.name, p.description, p.packaged, p.lifecycle_status,"
          + " l.name as line_name, p.line_code, ct.name as sub_line, v.version_no,"
          + " coalesce(v.effective_from, cast(p.authorized_at at time zone :zone as date),"
          + " cast(p.created_at at time zone :zone as date)) as effective_date,"
          + " v.package_end_date as expiry_date,"
          + " (select string_agg(distinct i.name, ', ') from cat_package_insurer pi"
          + " join cat_insurer i on i.party_code = pi.insurer_code and i.company_id = pi.company_id"
          + " where pi.version_id = v.id) as insurers,"
          + " case when coalesce(v.updated_at, v.created_at) > coalesce(p.updated_at, p.created_at)"
          + " then coalesce(v.updated_by, v.created_by) else coalesce(p.updated_by, p.created_by)"
          + " end as updated_by,"
          + " greatest(coalesce(v.updated_at, v.created_at), coalesce(p.updated_at, p.created_at))"
          + " as updated_at, d.id as deactivation_id, d.request_no as deactivation_no"
          + " from cat_product p"
          + " join cat_product_line l on l.code = p.line_code"
          + " left join cat_cover_type ct on ct.line_code = p.line_code and ct.code = p.cover_type_code"
          + " left join lateral (select x.* from cat_product_version x where x.product_code = p.code"
          + " and x.status in ('RELEASED', 'EXPIRED', 'SUPERSEDED')"
          + " order by case when x.status = 'RELEASED' and (x.effective_to is null"
          + " or x.effective_to >= :today) then 0 else 1 end, x.version_no desc limit 1) v on true"
          + " left join pm_deactivation_request d on d.product_code = p.code and d.status = 'PENDING'"
          + " where p.record_status = 'ACTIVE'"
          + " and (cast(:text as varchar) is null or lower(p.code) like lower(:text)"
          + " or lower(p.name) like lower(:text)"
          + " or lower(coalesce(p.description, '')) like lower(:text))"
          + " and (cast(:line as varchar) is null or p.line_code = :line)"
          + " and (cast(:packaged as boolean) is null or p.packaged = :packaged)";

  private static final String TAB =
      " where ((:tab = 'EXPIRED' and lifecycle_status in ('EXPIRED', 'RETIRED'))"
          + " or (:tab = 'EXPIRING' and lifecycle_status = 'ACTIVE'"
          + " and expiry_date between :today and :until)"
          + " or (:tab = 'ACTIVE' and lifecycle_status = 'ACTIVE'))";

  private static final String SORT_KEY =
      "case :sort when 'line' then line_name when 'subLine' then sub_line"
          + " when 'name' then name when 'status' then lifecycle_status"
          + " when 'effectiveDate' then to_char(effective_date, 'YYYY-MM-DD')"
          + " when 'expiryDate' then to_char(expiry_date, 'YYYY-MM-DD')"
          + " when 'updatedBy' then updated_by"
          + " when 'updatedAt' then to_char(updated_at, 'YYYY-MM-DD HH24:MI:SS')"
          + " else line_name || ' ' || name end";

  private static final String ORDER =
      " order by case when :dir = 'desc' then null else "
          + SORT_KEY
          + " end asc nulls last, case when :dir = 'desc' then "
          + SORT_KEY
          + " end desc nulls last, code";

  private static final String COUNT_SQL = "select count(*) from (" + BASE + ") m" + TAB;

  private static final String PAGE_SQL =
      "select * from (" + BASE + ") m" + TAB + ORDER + " limit :limit offset :offset";

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   * @param parameters business parameters (notice days)
   * @param clock clock
   */
  public ProductMatrixQuery(
      NamedParameterJdbcTemplate jdbc, SystemParameterService parameters, Clock clock) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * One page of the Product Matrix.
   *
   * @param f filter, sort and tab
   * @param page page (0 based)
   * @param size page size
   * @return rows and the total
   */
  public MatrixPage search(Filter f, int page, int size) {
    int limit = Math.min(Math.max(size, 1), MAX_PAGE);
    MapSqlParameterSource args = args(f);
    Long total = jdbc.queryForObject(COUNT_SQL, args, Long.class);
    args.addValue("limit", limit).addValue("offset", (long) Math.max(page, 0) * limit);
    List<MatrixRow> rows = jdbc.query(PAGE_SQL, args, (rs, i) -> row(rs));
    return new MatrixPage(rows, total == null ? 0 : total, noticeDays());
  }

  /**
   * Every row of the filter (export), up to {@value #MAX_EXPORT}.
   *
   * @param f filter
   * @return rows
   */
  public List<MatrixRow> all(Filter f) {
    MapSqlParameterSource args = args(f).addValue("limit", MAX_EXPORT).addValue("offset", 0L);
    return jdbc.query(PAGE_SQL, args, (rs, i) -> row(rs));
  }

  /**
   * Notice period of the Expiring Products tab.
   *
   * @return days
   */
  public int noticeDays() {
    return parameters.intValue(NOTICE_DAYS, DEFAULT_NOTICE);
  }

  private MapSqlParameterSource args(Filter f) {
    LocalDate today = BusinessClock.today(clock);
    String text = f.text() == null || f.text().isBlank() ? null : "%" + f.text().strip() + "%";
    return new MapSqlParameterSource()
        .addValue("zone", BusinessClock.zoneId())
        .addValue("today", today)
        .addValue("until", today.plusDays(noticeDays()))
        .addValue("text", text)
        .addValue("line", blankToNull(f.lineCode()))
        .addValue("packaged", f.packaged())
        .addValue("tab", tab(f.tab()))
        .addValue("sort", f.sort() == null ? "" : f.sort())
        .addValue("dir", "desc".equals(f.direction()) ? "desc" : "asc");
  }

  private static String tab(String tab) {
    return EXPIRED.equals(tab) || EXPIRING.equals(tab) ? tab : "ACTIVE";
  }

  private MatrixRow row(ResultSet rs) throws SQLException {
    LocalDate expiry = date(rs.getDate("expiry_date"));
    LocalDate today = BusinessClock.today(clock);
    String lifecycle = rs.getString("lifecycle_status");
    Timestamp updated = rs.getTimestamp("updated_at");
    Object version = rs.getObject("version_no");
    Object deactivation = rs.getObject("deactivation_id");
    return new MatrixRow(
        rs.getString("code"),
        rs.getString("line_code"),
        rs.getString("line_name"),
        rs.getString("sub_line"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getBoolean("packaged") ? "Package" : "Non-Package",
        rs.getString("insurers"),
        status(lifecycle, expiry, today),
        date(rs.getDate("effective_date")),
        expiry,
        expiry == null ? null : ChronoUnit.DAYS.between(today, expiry),
        rs.getString("updated_by"),
        updated == null ? null : updated.toInstant(),
        version == null ? null : ((Number) version).intValue(),
        deactivation == null ? null : ((Number) deactivation).longValue(),
        rs.getString("deactivation_no"));
  }

  private String status(String lifecycle, LocalDate expiry, LocalDate today) {
    if ("RETIRED".equals(lifecycle)) {
      return "Deactivated";
    }
    if (EXPIRED.equals(lifecycle)) {
      return "Expired";
    }
    if (expiry != null && !expiry.isAfter(today.plusDays(noticeDays()))) {
      return "Expiring";
    }
    return "Active";
  }

  private static LocalDate date(Date d) {
    return d == null ? null : d.toLocalDate();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * Filter of the Product Matrix.
   *
   * @param tab ACTIVE (default), EXPIRING or EXPIRED
   * @param text package code, name or description contains
   * @param lineCode line of insurance
   * @param packaged true for packages, false for non-package products, null for both
   * @param sort sort key (line, subLine, name, status, effectiveDate, expiryDate, updatedBy,
   *     updatedAt)
   * @param direction asc or desc
   */
  public record Filter(
      String tab, String text, String lineCode, Boolean packaged, String sort, String direction) {}

  /**
   * A row of the Product Matrix.
   *
   * @param productCode risk code
   * @param lineCode line code
   * @param lineOfInsurance line name
   * @param subLine cover type name
   * @param packageName product name
   * @param description package description
   * @param productType Package or Non-Package
   * @param insurers insurer names of the version in force
   * @param status Active, Expiring, Expired or Deactivated
   * @param effectiveDate effective date
   * @param expiryDate package expiry date
   * @param daysLeft days until the expiry date
   * @param lastUpdatedBy last user who changed the product or its version
   * @param lastUpdatedAt when
   * @param versionNo version in force (or last), null for a product without versions
   * @param deactivationId pending deactivation request, null when none
   * @param deactivationNo its number
   */
  public record MatrixRow(
      String productCode,
      String lineCode,
      String lineOfInsurance,
      String subLine,
      String packageName,
      String description,
      String productType,
      String insurers,
      String status,
      LocalDate effectiveDate,
      LocalDate expiryDate,
      Long daysLeft,
      String lastUpdatedBy,
      Instant lastUpdatedAt,
      Integer versionNo,
      Long deactivationId,
      String deactivationNo) {}

  /**
   * One page.
   *
   * @param rows rows
   * @param total rows of the filter
   * @param noticeDays notice period of the Expiring Products tab
   */
  public record MatrixPage(List<MatrixRow> rows, long total, int noticeDays) {}
}
