package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Document Processing Fallout ({@value #CODE}): documents that could not be read or whose proposal
 * was rejected, and intake files with refused rows.
 */
@Component
public class DocFalloutReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-DOC-FALLOUT";

  private static final String SQL =
      "select e.extraction_no as reference, 'Document' as kind, e.file_name, e.segment, "
          + "e.status, coalesce(e.reject_reason, e.note) as reason, e.created_at as uploaded "
          + "from sbm_extraction e where e.company_id = :company and (e.status in "
          + "('REJECTED','NOT_READABLE') or e.readable = false)"
          + String.format(SbmReportSupport.RANGE, "e.created_at")
          + " union all select r.run_no, 'Intake file', r.file_name, null, r.status, r.failed "
          + "|| ' rows refused', r.started_at from sbm_intake_run r where r.company_id = "
          + ":company and r.failed > 0"
          + String.format(SbmReportSupport.RANGE, "r.started_at")
          + " order by 7, 1";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public DocFalloutReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Document Processing Fallout",
            "Documents that could not be read or whose proposal was rejected, and intake files with refused rows",
            "Uploaded",
            SQL,
            List.of(
                ReportColumn.text("reference", "Reference"),
                ReportColumn.text("kind", "Kind"),
                ReportColumn.text("file_name", "File"),
                ReportColumn.text("segment", "Segment"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("reason", "Reason"),
                ReportColumn.date("uploaded", "Uploaded"))));
  }
}
