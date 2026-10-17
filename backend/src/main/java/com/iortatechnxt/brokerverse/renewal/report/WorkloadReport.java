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
 * Workload (RNW-WORKLOAD; FR-RN-103, BRD 1.009.7, 3.010.7): the open renewals of each Marketing AO
 * and Processing Officer by stage.
 */
@Component
public class WorkloadReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-WORKLOAD";

  private static final String SQL =
      "select 'Account Officer' as role, coalesce(c.assigned_ao, '(unassigned)') as officer, c.stage,"
          + " count(*) as accounts, sum(c.gross_premium) as premium"
          + " from rnw_candidate c where c.stage not in ('RENEWED', 'CLOSED')"
          + " and c.stage in ('UNASSIGNED', 'FOR_DISPOSITION', 'TRANSFER_PENDING', 'FOR_TL_REVIEW', 'NB_PATH')"
          + RenewalReportSupport.FILTERS
          + " group by c.assigned_ao, c.stage"
          + " union all select 'Processing Officer', coalesce(c.assigned_po, '(unassigned)'), c.stage,"
          + " count(*), sum(c.gross_premium)"
          + " from rnw_candidate c where c.stage in ('FOR_PROCESSING', 'IN_PROCESSING', 'WITH_INSURER',"
          + " 'RA_READY', 'RA_GENERATED', 'RA_SENT', 'ACCEPTED', 'FOR_PLACEMENT_BOOKING', 'LETTER_PENDING')"
          + RenewalReportSupport.FILTERS
          + " group by c.assigned_po, c.stage"
          + " order by 1, 2, 3";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public WorkloadReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Workload",
        "Open renewals per Account Officer and Processing Officer by stage");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, support.args(p).map()).stream().map(RenewalReportSupport::labels).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("officer", "Officer"),
            ReportColumn.text("stage", "Stage"),
            ReportColumn.count("accounts", "Accounts"),
            ReportColumn.amount("premium", "Expiring Premium"))
        .groupBy("role", "Role")
        .rows(rows)
        .presorted()
        .build();
  }
}
