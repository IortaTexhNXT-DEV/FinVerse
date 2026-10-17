package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Penetration Report ({@value #CODE}): records received per source and segment against those placed
 * and booked through the broker.
 */
@Component
public class PenetrationReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-PENETRATION";

  private static final String SQL =
      "select p.source_code, p.segment, count(*) as received, count(*) filter (where "
          + "p.status in ('PLACED','BOOKED')) as placed, round(100.0 * count(*) filter (where "
          + "p.status in ('PLACED','BOOKED')) / nullif(count(*), 0), 2) as penetration from "
          + "sbm_policy p where p.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "p.date_received")
          + " group by p.source_code, p.segment order by p.source_code, p.segment";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public PenetrationReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Penetration Report",
            "Records received per source and segment against those placed and booked through the broker",
            "Date Received",
            SQL,
            List.of(
                ReportColumn.text("source_code", "Source"),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.count("received", "Received"),
                ReportColumn.count("placed", "Placed or Booked"),
                ReportColumn.percent("penetration", "Penetration %"))));
  }
}
