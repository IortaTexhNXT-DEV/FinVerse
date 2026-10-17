package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/** IAAF Tracking ({@value #CODE}): iAAFs with their reviews, approval level and sending. */
@Component
public class IaafReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-IAAF";

  private static final String SQL =
      "select i.iaaf_no, p.sbm_no, p.segment, p.assured_name, p.sum_insured, i.status, "
          + "i.current_level, i.total_levels, (select count(*) from sbm_iaaf_review v where "
          + "v.policy_id = p.id) as reviews, (select v.adequacy from sbm_iaaf_review v where "
          + "v.policy_id = p.id order by v.review_no desc limit 1) as adequacy, i.created_by, "
          + "i.created_at, i.submitted_at, i.sent_to, i.sent_at from sbm_iaaf i join "
          + "sbm_policy p on p.id = i.policy_id where i.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "i.created_at")
          + " order by i.iaaf_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public IaafReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "IAAF Tracking",
            "IAAFs with their reviews, approval level and sending",
            "Prepared",
            SQL,
            List.of(
                ReportColumn.text("iaaf_no", "IAAF No."),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.amount("sum_insured", "Sum Insured"),
                ReportColumn.text("status", "Status"),
                ReportColumn.count("current_level", "Level"),
                ReportColumn.count("total_levels", "Levels"),
                ReportColumn.count("reviews", "Reviews"),
                ReportColumn.text("adequacy", "Last Review"),
                ReportColumn.text("created_by", "Prepared By"),
                ReportColumn.date("created_at", "Prepared"),
                ReportColumn.date("submitted_at", "Submitted"),
                ReportColumn.text("sent_to", "Sent To"),
                ReportColumn.date("sent_at", "Sent"))));
  }
}
