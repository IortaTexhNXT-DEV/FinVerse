package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Submitted Policies Conversion ({@value #CODE}): records handed to Renewal with their outcome,
 * account and booking.
 */
@Component
public class ConversionReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-CONVERSION";

  private static final String SQL =
      "select p.sbm_no, p.segment, p.assured_name, p.insurer_code, r.insurer_assigned, "
          + "p.expiry_date, r.handoff_status, r.arn, r.outcome, r.decline_reason, "
          + "p.conversion_status, p.booked_invoice_no, p.booked_on from sbm_renewal r join "
          + "sbm_policy p on p.id = r.policy_id where r.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date")
          + " order by p.expiry_date, p.sbm_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public ConversionReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Submitted Policies Conversion",
            "Records handed to Renewal with their outcome, account and booking",
            "Expiry",
            SQL,
            List.of(
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("insurer_code", "Expiring Insurer"),
                ReportColumn.text("insurer_assigned", "Insurer Assigned"),
                ReportColumn.date("expiry_date", "Expiry"),
                ReportColumn.text("handoff_status", "Hand-off"),
                ReportColumn.text("arn", "Renewal Account"),
                ReportColumn.text("outcome", "Outcome"),
                ReportColumn.text("decline_reason", "Decline Reason"),
                ReportColumn.text("conversion_status", "Conversion Status"),
                ReportColumn.text("booked_invoice_no", "Invoice"),
                ReportColumn.date("booked_on", "Booked"))));
  }
}
