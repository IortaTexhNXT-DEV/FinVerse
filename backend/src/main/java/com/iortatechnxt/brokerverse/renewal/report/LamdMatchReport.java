package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * LAMD match (RNW-LAMD-MATCH; FR-RN-025, BRRN.029): the lines of the LAMD reports with the renewal
 * each PN matched, or unmatched and ambiguous lines, and the routing applied.
 */
@Component
public class LamdMatchReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-LAMD-MATCH";

  private static final String SQL =
      "select r.report_no, r.report_type, r.period, l.row_no, l.pn_no, l.borrower, l.status_date,"
          + " l.match_outcome, c.renewal_ref, l.routing, l.message"
          + " from rnw_lamd_line l join rnw_lamd_report r on r.id = l.report_id"
          + " left join rnw_candidate c on c.id = l.candidate_id"
          + " where r.company_id = :company"
          + " and (cast(:from as date) is null or c.expiry_date is null or c.expiry_date >= :from)"
          + " and (cast(:to as date) is null or c.expiry_date is null or c.expiry_date <= :to)"
          + " order by r.report_no, l.row_no";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public LamdMatchReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "LAMD Match",
        "LAMD loan report lines matched and unmatched with the routing applied");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("report_type", "Report"),
            ReportColumn.text("period", "Period"),
            ReportColumn.count("row_no", "Row"),
            ReportColumn.text("pn_no", "PN"),
            ReportColumn.text("borrower", "Borrower"),
            ReportColumn.date("status_date", "Status Date"),
            ReportColumn.text("match_outcome", "Match"),
            ReportColumn.text("renewal_ref", "Renewal"),
            ReportColumn.text("routing", "Routing"),
            ReportColumn.text("message", "Message"))
        .groupBy("report_no", "LAMD Report")
        .rows(jdbc.rows(SQL, support.args(p).map()))
        .presorted()
        .build();
  }
}
