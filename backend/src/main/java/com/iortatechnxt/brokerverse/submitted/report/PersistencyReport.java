package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Renewal Persistency of Submitted Accounts ({@value #CODE}): records due for renewal per expiry
 * month against those renewed and booked.
 */
@Component
public class PersistencyReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-PERSISTENCY";

  private static final String SQL =
      "select to_char(p.expiry_date, 'YYYY-MM') as expiry_month, p.segment, count(*) as "
          + "due, count(r.id) as handed_off, count(*) filter (where p.status = 'BOOKED') as "
          + "booked, round(100.0 * count(*) filter (where p.status = 'BOOKED') / "
          + "nullif(count(*), 0), 2) as persistency from sbm_policy p left join sbm_renewal r "
          + "on r.policy_id = p.id where p.company_id = :company and p.renewal_tag = "
          + "'RENEWABLE'"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.expiry_date")
          + " group by 1, p.segment order by 1, p.segment";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public PersistencyReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Renewal Persistency of Submitted Accounts",
            "Records due for renewal per expiry month against those renewed and booked",
            "Expiry",
            SQL,
            List.of(
                ReportColumn.text("expiry_month", "Expiry Month"),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.count("due", "Due"),
                ReportColumn.count("handed_off", "Handed to Renewal"),
                ReportColumn.count("booked", "Booked"),
                ReportColumn.percent("persistency", "Persistency %"))));
  }
}
