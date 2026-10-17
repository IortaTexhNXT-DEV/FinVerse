package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Policy Review Monitoring ({@value #CODE}): records in review with their age since receipt and the
 * state of their IAAF.
 */
@Component
public class PolicyReviewMonitoringReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-PR-MONITORING";

  private static final String SQL =
      "select p.sbm_no, p.segment, p.assured_name, p.date_received, (current_date - "
          + "p.date_received) as age_days, case when current_date - p.date_received <= 5 then "
          + "'0-5 days' when current_date - p.date_received <= 15 then '6-15 days' when "
          + "current_date - p.date_received <= 30 then '16-30 days' else 'Over 30 days' end "
          + "as ageing, p.adequacy_status, i.iaaf_no, i.status, p.handler_username from "
          + "sbm_policy p left join sbm_iaaf i on i.policy_id = p.id where p.company_id = "
          + ":company and p.status = 'IN_REVIEW'"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.date_received")
          + " order by p.date_received, p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public PolicyReviewMonitoringReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Policy Review Monitoring",
            "Records in review with their age since receipt and the state of their IAAF",
            "Date Received",
            SQL,
            List.of(
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.date("date_received", "Date Received"),
                ReportColumn.count("age_days", "Age (days)"),
                ReportColumn.text("ageing", "Ageing"),
                ReportColumn.text("adequacy_status", "Adequacy"),
                ReportColumn.text("iaaf_no", "IAAF No."),
                ReportColumn.text("status", "IAAF Status"),
                ReportColumn.text("handler_username", "Handler"))));
  }
}
