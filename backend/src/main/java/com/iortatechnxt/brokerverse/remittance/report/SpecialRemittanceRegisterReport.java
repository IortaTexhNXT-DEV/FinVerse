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
 * Special Remittance Register (REM-SPECIAL-REGISTER, Annex III #2, RMTID.030/039, MKTID.009): the
 * special remittance requests of the period with requestor, processor, segment and the ages in days
 * of checking (request to approval), approval (approval to batch approval) and processing (request
 * to push to Disbursement).
 */
@Component
public class SpecialRemittanceRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "REM-SPECIAL-REGISTER";

  private static final String SQL =
      "select s.invoice_no, s.policy_no, s.insurer_code as insurer, s.assured_name,"
          + " s.condition_code as reason, b.processor, s.requested_by, s.segment,"
          + " s.created_at as posted, s.pushed_at as pushed, s.stage,"
          + " cast(extract(day from (coalesce(s.pushed_at, now()) - s.created_at)) as integer)"
          + " as processed_age,"
          + " cast(extract(day from (coalesce(s.approved_at, now()) - s.created_at)) as integer)"
          + " as checking_age,"
          + " cast(extract(day from (coalesce(b.approved_at, now()) - coalesce(s.approved_at, now())))"
          + " as integer) as approval_age"
          + " from rem_special_request s left join rem_batch b on b.batch_no = s.batch_no"
          + " where s.company_id = :companyId"
          + " and cast(s.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:insurer as varchar) is null or s.insurer_code = :insurer)"
          + " order by s.id";

  private final RemittanceReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public SpecialRemittanceRegisterReport(RemittanceReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    return RemittanceReportSql.metadata(
        CODE,
        "Special Remittance Register",
        "Special remittance requests with requestor, processor and ages (RMTID.030/039)",
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("invoice_no", "Invoice Number"),
            ReportColumn.text("policy_no", "Policy Number"),
            ReportColumn.text("insurer", "Insurance Company"),
            ReportColumn.text("assured_name", "Name of Assured"),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("processor", "Processor"),
            ReportColumn.text("requested_by", "Requestor"),
            ReportColumn.text("segment", "Marketing Segment"),
            ReportColumn.date("posted", "Date Request Posted"),
            ReportColumn.date("pushed", "Date Pushed to Disbursement"),
            ReportColumn.text("stage", "Status"),
            new ReportColumn("processed_age", "Processed Age", ColumnType.NUMBER, false),
            new ReportColumn("checking_age", "Checking Age", ColumnType.NUMBER, false),
            new ReportColumn("approval_age", "Approval Age", ColumnType.NUMBER, false))
        .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
        .presorted()
        .withoutGrandTotal()
        .build();
  }
}
