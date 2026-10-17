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

/** FIN-INV-RGL: realized gains and losses on maturities and sales in a date range. */
@Component
public class RealizedGainsReport implements ReportDefinition {

  private static final String SQL =
      """
      select t.txn_type, t.txn_date, h.holding_no, h.description, h.instrument_type,
             p.classification, t.cash_amount, t.final_tax, t.amount, t.gain_loss, t.batch_no
      from inv_transaction t
      join inv_holding h on h.id = t.holding_id
      join inv_portfolio p on p.id = h.portfolio_id
      where t.company_id = :companyId and t.txn_type in ('MATURITY', 'SALE')
        and t.txn_date between :fromDate and :toDate
        and (cast(:branchId as bigint) is null or h.branch_id = :branchId)
      order by t.txn_type, t.txn_date, h.holding_no
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC template
   */
  public RealizedGainsReport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-INV-RGL",
        "Realized Gains and Losses on Investments",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Maturities and sales with proceeds, carrying amount and realized gain or loss",
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
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("type", rs.getString("txn_type"));
              m.put("date", InvestmentPositions.isoDate(rs, "txn_date"));
              m.put("holdingNo", rs.getString("holding_no"));
              m.put("description", rs.getString("description"));
              m.put("instrument", rs.getString("instrument_type"));
              m.put("classification", rs.getString("classification"));
              m.put("proceeds", rs.getBigDecimal("cash_amount"));
              m.put("finalTax", rs.getBigDecimal("final_tax"));
              m.put("carrying", rs.getBigDecimal("amount"));
              m.put("gainLoss", rs.getBigDecimal("gain_loss"));
              m.put("batchNo", rs.getString("batch_no"));
              return m;
            });
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.date("date", "Date"),
            ReportColumn.text("holdingNo", "Holding No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text("classification", "Class"),
            ReportColumn.amount("proceeds", "Proceeds"),
            ReportColumn.amount("finalTax", "Final Tax"),
            ReportColumn.amount("carrying", "Carrying Amount"),
            ReportColumn.amount("gainLoss", "Gain / (Loss)"),
            ReportColumn.text("batchNo", "Journal"))
        .groupBy("type", "Event")
        .rows(rows)
        .presorted()
        .note(
            "Gain = proceeds + final tax + recycled FVOCI reserve - carrying amount - accrued interest.")
        .build();
  }
}
