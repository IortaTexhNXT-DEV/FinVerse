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
 * Go-live extraction (RNW-GOLIVE; FR-RN-016, DMQ37): the renewals of migrated policies per expiry
 * month, urgent ones, those whose Renewal Advice was already sent before go-live, and their
 * progress.
 */
@Component
public class GoLiveExtractionReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-GOLIVE";

  private static final String SQL =
      "select to_char(c.expiry_date, 'YYYY-MM') as month, count(*) as candidates,"
          + " count(*) filter (where c.urgent) as urgent,"
          + " count(*) filter (where exists (select 1 from rnw_letter l where l.candidate_id = c.id"
          + " and l.source = 'LEGACY_MANUAL')) as ra_sent,"
          + " count(*) filter (where c.initiated_at is not null) as initiated,"
          + " count(*) filter (where c.stage = 'RENEWED') as renewed,"
          + " count(*) filter (where c.bucket = 'EXCEPTION') as exceptions"
          + " from rnw_candidate c where c.source = 'LEGACY'"
          + RenewalReportSupport.FILTERS
          + " group by 1 order by 1";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public GoLiveExtractionReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Go-live Extraction",
        "Migrated policies of the go-live window per expiry month");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("month", "Expiry Month"),
            ReportColumn.count("candidates", "Renewals"),
            ReportColumn.count("urgent", "Urgent"),
            ReportColumn.count("ra_sent", "RA Already Sent"),
            ReportColumn.count("initiated", "Initiated"),
            ReportColumn.count("exceptions", "Exceptions"),
            ReportColumn.count("renewed", "Renewed"))
        .rows(jdbc.rows(SQL, support.args(p).map()))
        .presorted()
        .build();
  }
}
