package com.iortatechnxt.brokerverse.fixedasset.report;

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
import org.springframework.stereotype.Component;

/** FIN-FA-DEPR: depreciation charged per asset in each posted run, grouped by period. */
@Component
public class DepreciationScheduleReport implements ReportDefinition {

  private static final String SQL =
      """
      select r.period, a.tag_no, a.description, c.code as category_code, b.code as branch_code,
             l.months, l.amount, l.accumulated_after, l.net_book_value_after, l.batch_no
      from fa_depreciation_line l
      join fa_depreciation_run r on r.id = l.run_id
      join fa_asset a on a.id = l.asset_id
      join fa_category c on c.id = a.category_id
      join org_branch b on b.id = l.branch_id
      where r.company_id = :companyId and r.period_end between :fromDate and :toDate
        and (cast(:branchId as bigint) is null or l.branch_id = :branchId)
      order by r.period, c.code, a.tag_no
      """;

  private final FixedAssetReportQueries queries;

  /**
   * Creates the report.
   *
   * @param queries report queries
   */
  public DepreciationScheduleReport(FixedAssetReportQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-FA-DEPR",
        "Depreciation Schedule",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Monthly depreciation per asset for the runs of a date range",
        FixedAssetReportQueries.dateRangeParams(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    var sqlParams = FixedAssetReportQueries.dateRange(params);
    List<Map<String, Object>> rows =
        queries.query(
            SQL,
            sqlParams,
            rs -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("period", rs.getString("period"));
              m.put("tagNo", rs.getString("tag_no"));
              m.put("description", rs.getString("description"));
              m.put(FixedAssetReportQueries.CATEGORY, rs.getString("category_code"));
              m.put(FixedAssetReportQueries.BRANCH, rs.getString("branch_code"));
              m.put("months", rs.getInt("months"));
              m.put("amount", rs.getBigDecimal("amount"));
              m.put(FixedAssetReportQueries.ACCUMULATED, rs.getBigDecimal("accumulated_after"));
              m.put(
                  FixedAssetReportQueries.NET_BOOK_VALUE, rs.getBigDecimal("net_book_value_after"));
              m.put("batchNo", rs.getString("batch_no"));
              return m;
            });
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("tagNo", "Tag No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text(FixedAssetReportQueries.CATEGORY, "Category"),
            ReportColumn.text(FixedAssetReportQueries.BRANCH, "Branch"),
            ReportColumn.text("batchNo", "Journal"),
            ReportColumn.amount("amount", "Depreciation"),
            ReportColumn.amountNoTotal(FixedAssetReportQueries.ACCUMULATED, "Accumulated"),
            ReportColumn.amountNoTotal(FixedAssetReportQueries.NET_BOOK_VALUE, "Net Book Value"))
        .groupBy("period", "Period")
        .rows(rows)
        .presorted()
        .build();
  }
}
