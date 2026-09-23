package com.iortatechnxt.finverse.fixedasset.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** FIN-FA-NBV: cost, accumulated depreciation and net book value by branch and category. */
@Component
public class NetBookValueReport implements ReportDefinition {

  private static final String COUNT = "count";

  private final FixedAssetReportQueries queries;

  /**
   * Creates the report.
   *
   * @param queries report queries
   */
  public NetBookValueReport(FixedAssetReportQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-FA-NBV",
        "Net Book Value by Category",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Property and equipment summary by branch and asset category on a date",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    Map<String, Map<String, Object>> totals = new TreeMap<>();
    for (Map<String, Object> asset : queries.assetsAsOf(params)) {
      String key =
          asset.get(FixedAssetReportQueries.BRANCH)
              + "|"
              + asset.get(FixedAssetReportQueries.CATEGORY);
      Map<String, Object> row = totals.computeIfAbsent(key, k -> summaryRow(asset));
      row.merge(COUNT, 1, (a, b) -> (Integer) a + (Integer) b);
      for (String amount :
          List.of(
              FixedAssetReportQueries.COST,
              FixedAssetReportQueries.ACCUMULATED,
              FixedAssetReportQueries.NET_BOOK_VALUE)) {
        row.merge(amount, asset.get(amount), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
      }
    }
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text(FixedAssetReportQueries.CATEGORY, "Category"),
            ReportColumn.text("categoryName", "Description"),
            ReportColumn.count(COUNT, "Assets"),
            ReportColumn.amount(FixedAssetReportQueries.COST, "Cost"),
            ReportColumn.amount(FixedAssetReportQueries.ACCUMULATED, "Accum. Depreciation"),
            ReportColumn.amount(FixedAssetReportQueries.NET_BOOK_VALUE, "Net Book Value"))
        .groupBy(FixedAssetReportQueries.BRANCH, "Branch")
        .rows(List.copyOf(totals.values()))
        .presorted()
        .build();
  }

  private static Map<String, Object> summaryRow(Map<String, Object> asset) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(FixedAssetReportQueries.BRANCH, asset.get(FixedAssetReportQueries.BRANCH));
    row.put(FixedAssetReportQueries.CATEGORY, asset.get(FixedAssetReportQueries.CATEGORY));
    row.put("categoryName", asset.get("categoryName"));
    return row;
  }
}
