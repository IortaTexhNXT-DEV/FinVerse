package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Package choices (RNW-PACKAGE-REMAP; FR-RN-028, DMQ36): the BIBS package version chosen for each
 * migrated policy whose legacy package did not resolve, with the reason, the maker and the checker.
 */
@Component
public class PackageRemapReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-PACKAGE-REMAP";

  private static final String SQL =
      "select c.renewal_ref, c.client_name, pc.legacy_package_code, pc.legacy_package_version,"
          + " pc.product_code, pc.product_version_no, pc.reason, pc.status, pc.created_by as maker,"
          + " pc.decided_by as checker, cast(pc.decided_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as decided_on,"
          + " pc.decision_remarks"
          + " from rnw_package_choice pc join rnw_candidate c on c.id = pc.candidate_id where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " order by pc.id";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public PackageRemapReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Package Choices",
        "Package choices of migrated policies with the maker and the checker");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("renewal_ref", "Renewal"),
            ReportColumn.text("client_name", "Client"),
            ReportColumn.text("legacy_package_code", "Legacy Package"),
            ReportColumn.text("legacy_package_version", "Legacy Version"),
            ReportColumn.text("product_code", "BIBS Package"),
            ReportColumn.count("product_version_no", "BIBS Version"),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("maker", "Chosen By"),
            ReportColumn.text("checker", "Decided By"),
            ReportColumn.date("decided_on", "Decided On"),
            ReportColumn.text("decision_remarks", "Remarks"))
        .rows(jdbc.rows(SQL, support.args(p).map()))
        .presorted()
        .build();
  }
}
