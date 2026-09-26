package com.iortatechnxt.brokerverse.remittance.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Remittance Tracker (REM-TRACKER, Annex III #1, RMTID.039/036): per insurer, the batches extracted
 * in the period with type, accounts, net due, incentive, stage, approval and DV.
 */
@Component
public class RemittanceTrackerReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "REM-TRACKER";

  private static final String SQL =
      "select b.insurer_code as insurer, b.remittance_type as type, b.batch_no as batch,"
          + " b.line_count as accounts, b.net_due, b.incentive + b.incentive_vat as incentive,"
          + " b.net_due - b.incentive - b.incentive_vat as payable, b.stage,"
          + " b.created_at as extracted, b.approved_at as due_date, b.dv_no"
          + " from rem_batch b where b.company_id = :companyId"
          + " and cast(b.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:insurer as varchar) is null or b.insurer_code = :insurer)"
          + " order by b.insurer_code, b.id";

  private final RemittanceReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public RemittanceTrackerReport(RemittanceReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    return RemittanceReportSql.metadata(
        CODE,
        "Remittance Tracker",
        "Remittance batches per insurer with accounts, net due, incentive and status (RMTID.039)",
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("type", "Remit Type"),
            ReportColumn.text("batch", "Batch No."),
            new ReportColumn("accounts", "No. of Accounts Extracted", ColumnType.NUMBER, true),
            ReportColumn.amount("net_due", "Net Due"),
            ReportColumn.amount("incentive", "Incentive Amount"),
            ReportColumn.amount("payable", "Amount Payable"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.date("extracted", "Extracted On"),
            ReportColumn.date("due_date", "Due Date (Approved)"),
            ReportColumn.text("dv_no", "DV No."))
        .groupBy("insurer", "Insurance Company")
        .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
        .presorted()
        .note("Due date = approval date until BDOI defines the remittance due date (OQ42).")
        .build();
  }
}
