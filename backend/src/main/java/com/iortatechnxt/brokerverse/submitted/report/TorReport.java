package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TOR Status and Approval ({@value #CODE}): terms of Reference pending, approved and released, with
 * the Account Officer.
 */
@Component
public class TorReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-TOR";

  private static final String SQL =
      "select t.tor_no, p.sbm_no, p.assured_name, t.arn, t.status, t.current_level, "
          + "t.total_levels, t.ao_username, t.created_by, t.created_at, t.approved_at, "
          + "t.released_at from sbm_tor t join sbm_policy p on p.id = t.policy_id where "
          + "t.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "t.created_at")
          + " order by t.tor_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public TorReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "TOR Status and Approval",
            "Terms of Reference pending, approved and released, with the Account Officer",
            "Prepared",
            SQL,
            List.of(
                ReportColumn.text("tor_no", "TOR No."),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("arn", "Account"),
                ReportColumn.text("status", "Status"),
                ReportColumn.count("current_level", "Level"),
                ReportColumn.count("total_levels", "Levels"),
                ReportColumn.text("ao_username", "Account Officer"),
                ReportColumn.text("created_by", "Prepared By"),
                ReportColumn.date("created_at", "Prepared"),
                ReportColumn.date("approved_at", "Approved"),
                ReportColumn.date("released_at", "Released"))));
  }
}
