package com.iortatechnxt.brokerverse.reserves.report;

import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_PRODUCT;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_SOURCE;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR080 OSLR Processing Report: outstanding loss reserve = Σ (approved estimate − paid) of open
 * claims at the processed date, per branch, line of business, channel and product, with the
 * reinsurers' share and the net reserve. Zero when the claims module is not deployed.
 */
@Component
public class OslrProcessingReport implements ReportDefinition {

  private final ReserveReportSupport support;

  /**
   * Creates the report.
   *
   * @param support report support
   */
  public OslrProcessingReport(ReserveReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "PGIBR080",
        "OSLR Processing Report",
        ReportCategory.ACTUARIAL,
        "Outstanding loss reserve of open claims per class and product",
        ReserveReportSupport.baseParams("OSLR Processed Date"),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_SOURCE, "Source Type"),
            ReportColumn.text(K_PRODUCT, "Product"),
            ReportColumn.amount("oslr", "OSLR Amount"),
            ReportColumn.amount("ri", "Reinsurers' Share"),
            ReportColumn.amount("net", "Net OSLR"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Line of Business")
        .rows(
            ReserveReportSupport.unitRows(
                support.lines(p),
                ReserveType.OSLR,
                false,
                support.branchCodes(p.longValue(ReserveReportSupport.COMPANY)),
                l -> Map.of("oslr", l.gross(), "ri", l.ri(), "net", l.net())))
        .note("Company share, base currency, gross of reinsurance; open claims only.")
        .build();
  }
}
