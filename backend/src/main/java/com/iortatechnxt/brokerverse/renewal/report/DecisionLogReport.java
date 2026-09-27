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
 * Decision log (RNW-DECISIONS; FR-RN-023, 051, BRRN.031/034, BRD 1.011): every disposition with its
 * source, matrix version and rule, and every override with its reason and remarks.
 */
@Component
public class DecisionLogReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-DECISIONS";

  private static final String SQL =
      "select c.renewal_ref, 'Disposition' as kind, d.code as decision, d.reason_code as reason,"
          + " d.source, d.matrix_version, d.rule_id, d.remarks, d.created_by as decided_by,"
          + " cast(d.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as decided_on, d.created_at as at"
          + " from rnw_disposition d join rnw_candidate c on c.id = d.candidate_id where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " union all select c.renewal_ref, 'Override', o.kind || coalesce(' ' || o.check_code, ''),"
          + " o.reason_code, coalesce(o.from_value, '') || ' to ' || coalesce(o.to_value, ''), null, null,"
          + " o.remarks, o.created_by, cast(o.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date), o.created_at"
          + " from rnw_override o join rnw_candidate c on c.id = o.candidate_id where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " order by 1, 11";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public DecisionLogReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Decision Log",
        "Dispositions with their source and matrix rule, and the overrides with their rationale");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("decided_on", "Date"),
            ReportColumn.text("kind", "Kind"),
            ReportColumn.text("decision", "Decision"),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("source", "Source / Change"),
            ReportColumn.count("matrix_version", "Matrix Version"),
            ReportColumn.count("rule_id", "Rule"),
            ReportColumn.text("remarks", "Remarks"),
            ReportColumn.text("decided_by", "By"))
        .groupBy("renewal_ref", "Renewal")
        .rows(jdbc.rows(SQL, support.args(p).map()))
        .presorted()
        .build();
  }
}
