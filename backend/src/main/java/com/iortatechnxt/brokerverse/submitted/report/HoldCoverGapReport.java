package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Hold Cover with Gap ({@value #CODE}): renewal accounts whose hold cover starts after the expiry
 * of the submitted policy or is not confirmed.
 */
@Component
public class HoldCoverGapReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-HOLD-COVER-GAP";

  private static final String SQL =
      "select p.sbm_no, p.assured_name, p.expiry_date, r.arn, h.insurer_code, h.status, "
          + "h.start_date, h.expiry_date as hold_cover_end, h.confirmed_on, (h.start_date - "
          + "p.expiry_date) as gap_days from sbm_renewal r join sbm_policy p on p.id = "
          + "r.policy_id join plc_hold_cover h on h.arn = r.arn where r.company_id = :company "
          + "and (h.confirmed_on is null or h.start_date > p.expiry_date)"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date")
          + " order by p.expiry_date, p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public HoldCoverGapReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Hold Cover with Gap",
            "Renewal accounts whose hold cover starts after the expiry of the submitted policy or is not confirmed",
            "Expiry",
            SQL,
            List.of(
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.date("expiry_date", "Policy Expiry"),
                ReportColumn.text("arn", "Renewal Account"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("status", "Hold Cover Status"),
                ReportColumn.date("start_date", "Hold Cover Start"),
                ReportColumn.date("hold_cover_end", "Hold Cover End"),
                ReportColumn.date("confirmed_on", "Confirmed"),
                ReportColumn.count("gap_days", "Gap (days)"))));
  }
}
