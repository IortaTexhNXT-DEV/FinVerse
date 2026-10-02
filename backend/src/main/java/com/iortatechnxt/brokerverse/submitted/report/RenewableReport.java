package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renewable Accounts ({@value #CODE}): renewable records by bucket and expiry month. */
@Component
public class RenewableReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-RENEWABLE";

  private static final String SQL =
      "select p.bucket, to_char(p.expiry_date, 'YYYY-MM') as expiry_month, count(*) as "
          + "records, sum(p.sum_insured) as sum_insured, sum(p.total_premium) as "
          + "total_premium from sbm_policy p where p.company_id = :company and (p.renewal_tag "
          + "= 'RENEWABLE' or p.status in ('FOR_RENEWAL','RENEWAL_IN_PROGRESS'))"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date")
          + " group by p.bucket, 2 order by 2, p.bucket";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public RenewableReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Renewable Accounts",
            "Renewable records by bucket and expiry month",
            "Expiry",
            SQL,
            List.of(
                ReportColumn.text("bucket", "Bucket"),
                ReportColumn.text("expiry_month", "Expiry Month"),
                ReportColumn.count("records", "Records"),
                ReportColumn.amount("sum_insured", "Sum Insured"),
                ReportColumn.amount("total_premium", "Total Premium"))));
  }
}
