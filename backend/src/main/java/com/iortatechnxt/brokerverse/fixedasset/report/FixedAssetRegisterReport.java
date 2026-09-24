package com.iortatechnxt.brokerverse.fixedasset.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import org.springframework.stereotype.Component;

/** FIN-FA-REG: every asset carried on a date, by category, with cost and net book value. */
@Component
public class FixedAssetRegisterReport implements ReportDefinition {

  private final FixedAssetReportQueries queries;

  /**
   * Creates the report.
   *
   * @param queries report queries
   */
  public FixedAssetRegisterReport(FixedAssetReportQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-FA-REG",
        "Fixed Asset Register",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Assets in service on a date with cost, accumulated depreciation and net book value",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("tagNo", "Tag No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text(FixedAssetReportQueries.BRANCH, "Branch"),
            ReportColumn.date("acquisitionDate", "Acquired"),
            ReportColumn.text("location", "Location"),
            ReportColumn.text("custodian", "Custodian"),
            ReportColumn.text("status", "Status"),
            ReportColumn.amount(FixedAssetReportQueries.COST, "Cost"),
            ReportColumn.amount(FixedAssetReportQueries.ACCUMULATED, "Accum. Depreciation"),
            ReportColumn.amount(FixedAssetReportQueries.NET_BOOK_VALUE, "Net Book Value"))
        .groupBy(FixedAssetReportQueries.CATEGORY, "Category")
        .rows(queries.assetsAsOf(params))
        .presorted()
        .build();
  }
}
