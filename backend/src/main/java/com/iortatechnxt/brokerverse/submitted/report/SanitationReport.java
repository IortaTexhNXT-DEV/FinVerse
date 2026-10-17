package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Sanitation Report ({@value #CODE}): records checked by the sanitation step per outcome and reason
 * (certification of the sanitation).
 */
@Component("sbmSanitationReport")
public class SanitationReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-SANITATION";

  private static final String SQL =
      "select x.outcome, coalesce(x.reason_code, '') as reason_code, count(*) as "
          + "records from sbm_run_result x join sbm_run r on r.id = x.run_id join sbm_policy "
          + "p on p.id = x.policy_id where r.company_id = :company and x.step = 'SANITATION'"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "r.started_at")
          + " group by x.outcome, x.reason_code order by x.outcome, x.reason_code";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public SanitationReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Sanitation Report",
            "Records checked by the sanitation step per outcome and reason (certification of the sanitation)",
            "Run",
            SQL,
            List.of(
                ReportColumn.text("outcome", "Outcome"),
                ReportColumn.text("reason_code", "Reason"),
                ReportColumn.count("records", "Records"))));
  }
}
