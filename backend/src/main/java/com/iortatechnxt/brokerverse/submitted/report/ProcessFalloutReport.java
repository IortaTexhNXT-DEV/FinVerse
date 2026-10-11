package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Processing Fallout ({@value #CODE}): records that fell out of sanitation, matching,
 * classification or disposition, with the reason.
 */
@Component
public class ProcessFalloutReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-PROCESS-FALLOUT";

  private static final String SQL =
      "select r.run_no, r.started_at, p.sbm_no, p.segment, p.assured_name, x.step, "
          + "x.reason_code, x.rule_name, x.message from sbm_run_result x join sbm_run r on "
          + "r.id = x.run_id join sbm_policy p on p.id = x.policy_id where r.company_id = "
          + ":company and x.outcome = 'FALLOUT'"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "r.started_at")
          + " order by r.run_no, p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public ProcessFalloutReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Processing Fallout",
            "Records that fell out of sanitation, matching, classification or disposition, with the reason",
            "Run",
            SQL,
            List.of(
                ReportColumn.text("run_no", "Run"),
                ReportColumn.date("started_at", "Run Date"),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("step", "Step"),
                ReportColumn.text("reason_code", "Reason"),
                ReportColumn.text("rule_name", "Rule"),
                ReportColumn.text("message", "Message"))));
  }
}
