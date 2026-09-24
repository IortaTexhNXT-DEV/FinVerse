package com.iortatechnxt.brokerverse.remittance.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Remittance control reports named in the requirement rows (RMTID.015/016/020/039): remitted
 * accounts with their batch (Annex III #8, draft layout), paid AR higher than the DTIP, the insurer
 * OR vs paid PR exception report, accounts excluded from remittance and the hold register.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the reports
public final class RemittanceControlReports {

  private static final String PERIOD_FILTER =
      " and cast(b.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:insurer as varchar) is null or b.insurer_code = :insurer)";
  private static final String INVOICE_NO = "invoice_no";
  private static final String INVOICE = "Invoice Number";
  private static final String PAID_AR = "paid_ar";
  private static final String BATCH = "batch";
  private static final String BATCH_LABEL = "Batch No.";
  private static final String INSURER = "insurer";
  private static final String INSURER_LABEL = "Insurer";
  private static final String ASSURED = "assured_name";
  private static final String ASSURED_LABEL = "Name of Assured";

  private RemittanceControlReports() {}

  /** REM-REMITTED-BATCH (Annex III #8; layout not specified, OQ42). */
  @Component
  public static class RemittedAccounts implements ReportDefinition {

    private static final String SQL =
        "select b.batch_no as batch, b.insurer_code as insurer, b.remittance_type as type,"
            + " b.approved_at as approved, b.dv_no, l.invoice_no, l.assured_name, l.paid_ar,"
            + " l.net_due, l.remitted_status, l.insurer_or_no"
            + " from rem_batch_line l join rem_batch b on b.id = l.batch_id"
            + " where b.company_id = :companyId and not l.excluded"
            + " and b.stage in ('APPROVED', 'PARTIALLY_REMITTED', 'FULLY_REMITTED', 'OR_RECEIVED')"
            + PERIOD_FILTER
            + " order by b.id, l.id";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public RemittedAccounts(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-REMITTED-BATCH",
          "List of Remitted Accounts with Batch Number",
          "Accounts of the approved remittance batches with DV and insurer OR (RMTID.039)",
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text(INSURER, INSURER_LABEL),
              ReportColumn.text("type", "Remit Type"),
              ReportColumn.date("approved", "Approved On"),
              ReportColumn.text("dv_no", "DV No."),
              ReportColumn.text(INVOICE_NO, INVOICE),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.amount(PAID_AR, "Paid AR"),
              ReportColumn.amount("net_due", "Net Due"),
              ReportColumn.text("remitted_status", "Remittance Status"),
              ReportColumn.text("insurer_or_no", "Insurer OR"))
          .groupBy(BATCH, BATCH_LABEL)
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .note(RemittanceReportSql.DRAFT_NOTE)
          .build();
    }
  }

  /** REM-PAIDAR-OVER-DTIP (RMTID.015). */
  @Component
  public static class PaidArOverDtip implements ReportDefinition {

    private static final String SQL =
        "select r.run_no, t.created_at as examined, t.invoice_no, t.insurer_code as insurer,"
            + " t.paid_ar, t.dtip_balance, t.paid_ar - t.dtip_balance as excess,"
            + " case when t.reasons like '%PAID_AR_OVER_DTIP%' then 'Excluded' else 'Capped' end"
            + " as treatment, t.batch_no"
            + " from rem_extraction_tag t left join rem_extraction_run r on r.id = t.run_id"
            + " where t.company_id = :companyId"
            + " and (t.reasons like '%PAID_AR_OVER_DTIP%' or t.remarks like 'Paid AR capped%')"
            + " and cast(t.created_at at time zone 'Asia/Manila' as date) between :from and :to"
            + " and (cast(:insurer as varchar) is null or t.insurer_code = :insurer)"
            + " order by t.id";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public PaidArOverDtip(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-PAIDAR-OVER-DTIP",
          "Paid AR Higher than DTIP Balance",
          "Invoices whose paid AR exceeded the DTIP balance at extraction (RMTID.014/015, OQ19)",
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("run_no", "Extraction Run"),
              ReportColumn.date("examined", "Examined On"),
              ReportColumn.text(INVOICE_NO, INVOICE),
              ReportColumn.text(INSURER, INSURER_LABEL),
              ReportColumn.amount(PAID_AR, "Paid AR"),
              ReportColumn.amount("dtip_balance", "DTIP Balance"),
              ReportColumn.amount("excess", "Excess"),
              ReportColumn.text("treatment", "Treatment"),
              ReportColumn.text(BATCH + "_no", BATCH_LABEL))
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .build();
    }
  }

  /** REM-OR-EXCEPTION (RMTID.016). */
  @Component
  public static class OrExceptionReport implements ReportDefinition {

    private static final String STATUS = "status";
    private static final String RUN = "runNo";
    private static final String SQL =
        "select b.batch_no as batch, l.invoice_no, l.client_code, l.assured_name, l.insurer_or_no,"
            + " l.insurer_or_date, l.insurer_or_amount, l.paid_ar,"
            + " l.insurer_or_amount - l.paid_ar as difference, l.or_status as status, l.or_run_no"
            + " from rem_batch_line l join rem_batch b on b.id = l.batch_id"
            + " where b.company_id = :companyId and l.insurer_or_no is not null"
            + PERIOD_FILTER
            + " and (cast(:status as varchar) is null or l.or_status = :status)"
            + " and (cast(:runNo as varchar) is null or l.or_run_no = :runNo)"
            + " order by b.id, l.id";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public OrExceptionReport(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-OR-EXCEPTION",
          "OR vs Paid PR Exception Report",
          "Insurer OR amounts compared with the paid AR per account, per upload (RMTID.016)",
          true,
          ParameterSpec.select(
              STATUS,
              "Status",
              List.of(RemittanceReportSql.ALL, "MATCHED", "AMOUNT_MISMATCH"),
              RemittanceReportSql.ALL),
          ParameterSpec.optional(RUN, "Upload run", ParameterType.TEXT));
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = RemittanceReportSql.args(p);
      args.put(STATUS, RemittanceReportSql.selected(p, STATUS));
      args.put(RUN, p.optionalText(RUN).map(String::strip).filter(s -> !s.isEmpty()).orElse(null));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text(INVOICE_NO, INVOICE),
              ReportColumn.text("client_code", "Client"),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.text("insurer_or_no", "OR Number"),
              ReportColumn.date("insurer_or_date", "OR Date"),
              ReportColumn.amount("insurer_or_amount", "OR Amount"),
              ReportColumn.amount(PAID_AR, "Paid PR"),
              ReportColumn.amount("difference", "Difference"),
              ReportColumn.text(STATUS, "Status"),
              ReportColumn.text("or_run_no", "Upload Run"))
          .groupBy(BATCH, BATCH_LABEL)
          .rows(sql.rows(SQL, args))
          .presorted()
          .build();
    }
  }

  /** REM-EXCLUDED (RMTID.020/022/031). */
  @Component
  public static class Excluded implements ReportDefinition {

    private static final String SQL =
        "select 'Batch exclusion' as source, b.batch_no as reference, l.invoice_no,"
            + " b.insurer_code as insurer, l.exclusion_reason as reasons, l.exclusion_comment as remarks,"
            + " l.excluded_by as by_user, l.excluded_at as on_date, l.paid_ar"
            + " from rem_batch_line l join rem_batch b on b.id = l.batch_id"
            + " where b.company_id = :companyId and l.excluded"
            + " and cast(l.excluded_at at time zone 'Asia/Manila' as date) between :from and :to"
            + " and (cast(:insurer as varchar) is null or b.insurer_code = :insurer)"
            + " union all"
            + " select 'Not extracted', r.run_no, t.invoice_no, t.insurer_code, t.reasons, t.remarks,"
            + " t.created_by, t.created_at, t.paid_ar"
            + " from rem_extraction_tag t left join rem_extraction_run r on r.id = t.run_id"
            + " where t.company_id = :companyId and t.tag = 'UNEXTRACTED_DUE'"
            + " and cast(t.created_at at time zone 'Asia/Manila' as date) between :from and :to"
            + " and (cast(:insurer as varchar) is null or t.insurer_code = :insurer)"
            + " order by 1, 8";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Excluded(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-EXCLUDED",
          "Accounts Excluded from Remittance",
          "Invoices excluded from batches or not extracted: hold, pending negative adjustment,"
              + " write-off, check holding, over DTIP (RMTID.002/020/022/031)",
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("reference", "Batch / Run"),
              ReportColumn.text(INVOICE_NO, INVOICE),
              ReportColumn.text(INSURER, INSURER_LABEL),
              ReportColumn.text("reasons", "Reasons"),
              ReportColumn.text("remarks", "Remarks"),
              ReportColumn.text("by_user", "By"),
              ReportColumn.date("on_date", "On"),
              ReportColumn.amount(PAID_AR, "Paid AR"))
          .groupBy("source", "Source")
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .build();
    }
  }

  /** REM-HOLD (RMTID.021/032, MKTID.002-007). */
  @Component
  public static class Holds implements ReportDefinition {

    private static final String SQL =
        "select h.request_no, h.invoice_no, h.assured_name, h.insurer_code as insurer,"
            + " h.reason_code, h.hold_until, h.extension_count, h.stage, h.requested_by,"
            + " h.approved_by, h.assigned_processor, h.source, h.released_at"
            + " from rem_hold_request h where h.company_id = :companyId"
            + " and cast(h.created_at at time zone 'Asia/Manila' as date) between :from and :to"
            + " and (cast(:insurer as varchar) is null or h.insurer_code = :insurer)"
            + " order by h.stage, h.hold_until, h.id";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Holds(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-HOLD",
          "Remittance Hold Register",
          "Hold requests with status, hold-until date, extensions and assignment (RMTID.021/032)",
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("request_no", "Hold No."),
              ReportColumn.text(INVOICE_NO, INVOICE),
              ReportColumn.text(ASSURED, ASSURED_LABEL),
              ReportColumn.text(INSURER, INSURER_LABEL),
              ReportColumn.text("reason_code", "Reason"),
              ReportColumn.date("hold_until", "Hold Until"),
              new ReportColumn("extension_count", "Extensions", ColumnType.NUMBER, false),
              ReportColumn.text("requested_by", "Requested By"),
              ReportColumn.text("approved_by", "Approved By"),
              ReportColumn.text("assigned_processor", "Processor"),
              ReportColumn.text("source", "Source"),
              ReportColumn.date("released_at", "Released On"))
          .groupBy("stage", "Status")
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .build();
    }
  }
}
