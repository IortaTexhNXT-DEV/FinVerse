package com.iortatechnxt.brokerverse.investment.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
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
 * Holdings held on a date with their carrying amount and accrued interest on that date. Every
 * posted holding transaction records the position after it, so the position on a date is the one
 * after the holding's last transaction on or before that date.
 */
@Component
public class InvestmentPositions {

  /** Row key: carrying amount. */
  public static final String CARRYING = "carrying";

  /** Row key: accrued interest. */
  public static final String ACCRUED = "accrued";

  /** Row key: face value. */
  public static final String FACE = "face";

  /** Row key: maturity date (ISO text, null for equities). */
  public static final String MATURITY = "maturity";

  private static final String POSITIONS =
      """
      select h.holding_no, h.security_code, h.description, h.instrument_type, p.classification,
             p.code as portfolio, h.issuer_code, h.custodian, h.currency, h.face_value,
             h.coupon_rate, h.maturity_date, h.security_deposit,
             t.carrying_after, t.accrued_after
      from inv_holding h
      join inv_portfolio p on p.id = h.portfolio_id
      join lateral (select x.carrying_after, x.accrued_after from inv_transaction x
                    where x.holding_id = h.id and x.txn_date <= :asOf
                    order by x.txn_date desc, x.id desc limit 1) t on true
      where h.company_id = :companyId and h.status <> 'PENDING_APPROVAL'
        and (h.closed_date is null or h.closed_date > :asOf)
        and (cast(:branchId as bigint) is null or h.branch_id = :branchId)
        and (h.security_deposit or not :depositsOnly)
      order by h.holding_no
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc JDBC template
   */
  public InvestmentPositions(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Company and optional branch SQL parameters.
   *
   * @param p report parameters
   * @return SQL parameters
   */
  public static MapSqlParameterSource sqlParams(ReportParameters p) {
    return new MapSqlParameterSource()
        .addValue("companyId", p.longValue(GlReportSupport.COMPANY))
        .addValue("branchId", p.optionalLong(GlReportSupport.BRANCH).orElse(null));
  }

  /**
   * Parameters of a date-range report: company, branch, from (year start) and to (today).
   *
   * @return parameter specs
   */
  public static List<ParameterSpec> periodParams() {
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
  public static MapSqlParameterSource periodSqlParams(ReportParameters p) {
    return sqlParams(p)
        .addValue(GlReportSupport.FROM, p.date(GlReportSupport.FROM))
        .addValue(GlReportSupport.TO, p.date(GlReportSupport.TO));
  }

  /**
   * Positions on the report's as-of date.
   *
   * @param p report parameters (company, branch, as-of date)
   * @param depositsOnly only security deposits with the Insurance Commission
   * @return rows by holding number
   */
  public List<Map<String, Object>> asOf(ReportParameters p, boolean depositsOnly) {
    MapSqlParameterSource params =
        sqlParams(p)
            .addValue("asOf", p.date(GlReportSupport.AS_OF))
            .addValue("depositsOnly", depositsOnly);
    return jdbc.query(POSITIONS, params, (rs, i) -> row(rs));
  }

  /**
   * Reads a date column as ISO text.
   *
   * @param rs result set
   * @param column column
   * @return ISO date or null
   * @throws SQLException on read failure
   */
  public static String isoDate(ResultSet rs, String column) throws SQLException {
    LocalDate d = rs.getObject(column, LocalDate.class);
    return d == null ? null : d.toString();
  }

  private static Map<String, Object> row(ResultSet rs) throws SQLException {
    BigDecimal carrying = rs.getBigDecimal("carrying_after");
    BigDecimal accrued = rs.getBigDecimal("accrued_after");
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("holdingNo", rs.getString("holding_no"));
    m.put("securityCode", rs.getString("security_code"));
    m.put("description", rs.getString("description"));
    m.put("instrumentType", rs.getString("instrument_type"));
    m.put("classification", rs.getString("classification"));
    m.put("portfolio", rs.getString("portfolio"));
    m.put("issuer", rs.getString("issuer_code"));
    m.put("custodian", rs.getString("custodian"));
    m.put("currency", rs.getString("currency"));
    m.put(FACE, rs.getBigDecimal("face_value"));
    m.put("couponRate", rs.getBigDecimal("coupon_rate"));
    m.put(MATURITY, isoDate(rs, "maturity_date"));
    m.put(CARRYING, carrying);
    m.put(ACCRUED, accrued);
    m.put("total", carrying.add(accrued));
    return m;
  }
}
