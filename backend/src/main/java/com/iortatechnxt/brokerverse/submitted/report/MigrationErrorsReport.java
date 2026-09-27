package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Migration Error Log ({@value #CODE}): rows of the migration uploads of the Excel masterlists that
 * were refused.
 */
@Component
public class MigrationErrorsReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-MIGRATION-ERRORS";

  private static final String SQL =
      "select j.job_no, j.file_name, j.created_at, r.row_no, r.status, r.messages from "
          + "bulk_job j join bulk_row r on r.job_id = j.id where j.company_id = :company and "
          + "j.handler_code = 'SBM_MIGRATION' and r.status in ('INVALID','FAILED')"
          + String.format(SbmReportSupport.RANGE, "j.created_at")
          + " order by j.job_no, r.row_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public MigrationErrorsReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Migration Error Log",
            "Rows of the migration uploads of the Excel masterlists that were refused",
            "Upload",
            SQL,
            List.of(
                ReportColumn.text("job_no", "Upload"),
                ReportColumn.text("file_name", "File"),
                ReportColumn.date("created_at", "Uploaded"),
                ReportColumn.count("row_no", "Row"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("messages", "Errors"))));
  }
}
