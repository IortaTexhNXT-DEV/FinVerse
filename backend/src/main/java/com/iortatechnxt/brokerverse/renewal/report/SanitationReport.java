package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Sanitation (RNW-SANITATION; FR-RN-020, 022, BRRN.020/023): the results of the latest check run of
 * each renewal with its severity, and the bucket it gave.
 */
@Component
public class SanitationReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-SANITATION";

  private static final String SQL =
      "select c.renewal_ref, c.client_name, c.bucket, c.bucket_rule_version, r.check_code,"
          + " r.outcome, r.severity, r.message, c.expiry_date"
          + " from rnw_candidate c join rnw_check_result r on r.run_id = c.last_check_run_id"
          + " where r.outcome in ('FAIL', 'WARN')"
          + RenewalReportSupport.FILTERS
          + " order by c.renewal_ref, r.check_code";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public SanitationReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE, "Renewal Sanitation", "Results of the latest checks of each renewal with its bucket");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, support.args(p).map()).stream()
            .map(RenewalReportSupport::labels)
            .map(SanitationReport::named)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("client_name", "Client"),
            ReportColumn.text("bucket", "Classification"),
            ReportColumn.count("bucket_rule_version", "Rules Version"),
            ReportColumn.text("check_code", "Check"),
            ReportColumn.text("outcome", "Outcome"),
            ReportColumn.text("severity", "Severity"),
            ReportColumn.text("message", "Message"),
            ReportColumn.date("expiry_date", "Expiry Date"))
        .groupBy("renewal_ref", "Renewal")
        .rows(rows)
        .presorted()
        .build();
  }

  private static java.util.Map<String, Object> named(java.util.Map<String, Object> r) {
    r.put("check_code", CheckNames.of((String) r.get("check_code")));
    return r;
  }
}
