package com.iortatechnxt.brokerverse.investment.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * FIN-INV-ACCR: interest accrued and premium / discount amortized per holding in a date range
 * (month-end runs and catch-ups before receipts, maturities and sales).
 */
@Component
public class InterestAccrualRegisterReport implements ReportDefinition {

  private static final String SQL =
      """
      select p.code as portfolio, h.holding_no, h.description, h.day_count, t.txn_type,
             t.from_date, t.txn_date, t.days, t.amount, t.batch_no
      from inv_transaction t
      join inv_holding h on h.id = t.holding_id
      join inv_portfolio p on p.id = h.portfolio_id
      where t.company_id = :companyId and t.txn_type in ('ACCRUAL', 'AMORTIZATION')
        and t.txn_date between :fromDate and :toDate
        and (cast(:branchId as bigint) is null or h.branch_id = :branchId)
      order by p.code, h.holding_no, t.txn_date, t.id
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC template
   */
  public InterestAccrualRegisterReport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-INV-ACCR",
        "Interest Accrual Register",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Interest accrued and premium / discount amortized per holding in a date range",
        InvestmentPositions.periodParams(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    var sqlParams = InvestmentPositions.periodSqlParams(params);
    List<Map<String, Object>> rows =
        jdbc.query(
            SQL,
            sqlParams,
            (rs, i) -> {
              boolean accrual = "ACCRUAL".equals(rs.getString("txn_type"));
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("portfolio", rs.getString("portfolio"));
              m.put("holdingNo", rs.getString("holding_no"));
              m.put("description", rs.getString("description"));
              m.put("dayCount", accrual ? rs.getString("day_count") : "ACT_365");
              m.put("fromDate", InvestmentPositions.isoDate(rs, "from_date"));
              m.put("toDate", InvestmentPositions.isoDate(rs, "txn_date"));
              m.put("days", rs.getInt("days"));
              m.put("interest", accrual ? rs.getBigDecimal("amount") : null);
              m.put("amortization", accrual ? null : rs.getBigDecimal("amount"));
              m.put("batchNo", rs.getString("batch_no"));
              return m;
            });
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("holdingNo", "Holding No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text("dayCount", "Basis"),
            ReportColumn.date("fromDate", "From"),
            ReportColumn.date("toDate", "To"),
            ReportColumn.count("days", "Days"),
            ReportColumn.amount("interest", "Coupon Interest"),
            ReportColumn.amount("amortization", "Amortization"),
            ReportColumn.text("batchNo", "Journal"))
        .groupBy("portfolio", "Portfolio")
        .rows(rows)
        .presorted()
        .build();
  }
}
