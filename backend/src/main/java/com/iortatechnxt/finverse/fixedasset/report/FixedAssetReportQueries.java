package com.iortatechnxt.finverse.fixedasset.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * SQL used by the fixed asset reports. Accumulated depreciation "as of" a date is the take-on
 * opening amount plus the depreciation of runs whose period ended on or before that date.
 */
@Component
public class FixedAssetReportQueries {

  /** Row key: category code. */
  public static final String CATEGORY = "category";

  /** Row key: branch code. */
  public static final String BRANCH = "branch";

  /** Row key: cost. */
  public static final String COST = "cost";

  /** Row key: accumulated depreciation. */
  public static final String ACCUMULATED = "accumulated";

  /** Row key: net book value. */
  public static final String NET_BOOK_VALUE = "nbv";

  private static final String COMPANY_ID = "companyId";
  private static final String BRANCH_ID = "branchId";

  private static final String ASSETS_AS_OF =
      """
      select a.tag_no, a.description, c.code as category_code, c.name as category_name,
             b.code as branch_code, a.acquisition_date, a.acquisition_cost, a.status,
             a.location, a.custodian,
             a.opening_accumulated_depreciation + coalesce((select sum(l.amount)
                 from fa_depreciation_line l join fa_depreciation_run r on r.id = l.run_id
                 where l.asset_id = a.id and r.period_end <= :asOf), 0) as accumulated
      from fa_asset a
      join fa_category c on c.id = a.category_id
      join org_branch b on b.id = a.branch_id
      where a.company_id = :companyId and a.status <> 'PENDING_CAPITALIZATION'
        and a.capitalization_date <= :asOf
        and (a.disposal_date is null or a.disposal_date > :asOf)
        and (cast(:branchId as bigint) is null or a.branch_id = :branchId)
      order by c.code, a.tag_no
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc JDBC template
   */
  public FixedAssetReportQueries(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Standard parameters: company and optional branch.
   *
   * @param p report parameters
   * @return SQL parameters
   */
  public static MapSqlParameterSource companyAndBranch(ReportParameters p) {
    return new MapSqlParameterSource()
        .addValue(COMPANY_ID, p.longValue(GlReportSupport.COMPANY))
        .addValue(BRANCH_ID, p.optionalLong(GlReportSupport.BRANCH).orElse(null));
  }

  /**
   * Parameters of a date-range report: company, branch, from (year start) and to (today).
   *
   * @return parameter specs
   */
  public static List<ParameterSpec> dateRangeParams() {
    return List.of(
        GlReportSupport.companyParam(),
        GlReportSupport.branchParam(),
        ParameterSpec.required(GlReportSupport.FROM, "From Date", ParameterType.DATE)
            .withDefault("YEAR_START"),
        GlReportSupport.toParam());
  }

  /**
   * SQL parameters of a date-range report.
   *
   * @param p report parameters
   * @return company, branch, fromDate and toDate
   */
  public static MapSqlParameterSource dateRange(ReportParameters p) {
    return companyAndBranch(p)
        .addValue(GlReportSupport.FROM, p.date(GlReportSupport.FROM))
        .addValue(GlReportSupport.TO, p.date(GlReportSupport.TO));
  }

  /**
   * Assets carried in the books on a date with their accumulated depreciation on that date.
   *
   * @param p report parameters (company, branch, as-of date)
   * @return rows ordered by category and tag number
   */
  public List<Map<String, Object>> assetsAsOf(ReportParameters p) {
    MapSqlParameterSource params =
        companyAndBranch(p).addValue("asOf", p.date(GlReportSupport.AS_OF));
    return jdbc.query(ASSETS_AS_OF, params, (rs, i) -> assetRow(rs));
  }

  /**
   * Runs a query returning one map per row.
   *
   * @param sql SQL
   * @param params parameters
   * @param mapper row mapper
   * @return rows
   */
  public List<Map<String, Object>> query(
      String sql, MapSqlParameterSource params, RowReader mapper) {
    return jdbc.query(sql, params, (rs, i) -> mapper.read(rs));
  }

  private static Map<String, Object> assetRow(ResultSet rs) throws SQLException {
    BigDecimal cost = rs.getBigDecimal("acquisition_cost");
    BigDecimal accumulated = rs.getBigDecimal(ACCUMULATED);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(CATEGORY, rs.getString("category_code"));
    m.put("categoryName", rs.getString("category_name"));
    m.put("tagNo", rs.getString("tag_no"));
    m.put("description", rs.getString("description"));
    m.put(BRANCH, rs.getString("branch_code"));
    m.put("acquisitionDate", date(rs, "acquisition_date"));
    m.put(COST, cost);
    m.put(ACCUMULATED, accumulated);
    m.put(NET_BOOK_VALUE, cost.subtract(accumulated));
    m.put("status", rs.getString("status"));
    m.put("location", rs.getString("location"));
    m.put("custodian", rs.getString("custodian"));
    return m;
  }

  /**
   * Reads a date column as ISO text (reports display dates as text).
   *
   * @param rs result set
   * @param column column
   * @return ISO date or null
   * @throws SQLException on read failure
   */
  public static String date(ResultSet rs, String column) throws SQLException {
    LocalDate d = rs.getObject(column, LocalDate.class);
    return d == null ? null : d.toString();
  }

  /** Maps the current result set row. */
  @FunctionalInterface
  public interface RowReader {

    /**
     * Reads a row.
     *
     * @param rs result set positioned on the row
     * @return row values
     * @throws SQLException on read failure
     */
    Map<String, Object> read(ResultSet rs) throws SQLException;
  }
}
