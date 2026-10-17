package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Disposition Report ({@value #CODE}): records per disposition bucket and reason from the
 * disposition step.
 */
@Component
public class DispositionReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-DISPOSITION";

  private static final String SQL =
      "select x.bucket, x.outcome, coalesce(x.reason_code, '') as reason_code, count(*) "
          + "as records from sbm_run_result x join sbm_run r on r.id = x.run_id join "
          + "sbm_policy p on p.id = x.policy_id where r.company_id = :company and x.step = "
          + "'DISPOSITION'"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "r.started_at")
          + " group by x.bucket, x.outcome, x.reason_code order by x.bucket, x.outcome";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public DispositionReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Disposition Report",
            "Records per disposition bucket and reason from the disposition step",
            "Run",
            SQL,
            List.of(
                ReportColumn.text("bucket", "Bucket"),
                ReportColumn.text("outcome", "Outcome"),
                ReportColumn.text("reason_code", "Reason"),
                ReportColumn.count("records", "Records"))));
  }
}
