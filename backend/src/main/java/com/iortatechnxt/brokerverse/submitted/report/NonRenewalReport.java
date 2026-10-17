package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Non-renewal Report ({@value #CODE}): records excluded, tagged non-renewable or not renewed, with
 * the reason.
 */
@Component
public class NonRenewalReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-NON-RENEWAL";

  private static final String SQL =
      "select p.sbm_no, p.segment, p.assured_name, p.insurer_code, p.policy_no, "
          + "p.expiry_date, p.sum_insured, p.status, coalesce(p.renewal_tag_reason, "
          + "p.status_reason, p.bucket_reason) as reason, p.renewal_tagged_by from sbm_policy "
          + "p where p.company_id = :company and (p.status in ('EXCLUDED','NOT_RENEWED') or "
          + "p.renewal_tag = 'NON_RENEWABLE')"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date")
          + " order by p.expiry_date, p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public NonRenewalReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Non-renewal Report",
            "Records excluded, tagged non-renewable or not renewed, with the reason",
            "Expiry",
            SQL,
            List.of(
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("policy_no", "Policy No."),
                ReportColumn.date("expiry_date", "Expiry"),
                ReportColumn.amount("sum_insured", "Sum Insured"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("reason", "Reason"),
                ReportColumn.text("renewal_tagged_by", "Tagged By"))));
  }
}
