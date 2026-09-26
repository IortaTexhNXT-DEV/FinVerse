package com.iortatechnxt.brokerverse.collections.unapplied.report;

import com.iortatechnxt.brokerverse.collections.report.ClxReportSql;
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
 * The Collections reports of the unapplied payments (BDOI_CLXN_BRD_SPEC section 7): the requests
 * "For Application To Invoice" of a period (the report of the daily text file, BRCLXN.041, p.60)
 * and the collector dispositions of a period with their Cashiering request (BRCLXN.031/033, 040).
 * Both are Collections reports (view CLX_REPORT_VIEW, export CLX_EXPORT, archived).
 */
public final class UnappliedReports {

  /** For Application To Invoice code. */
  public static final String APPLICATION_TO_INVOICE = "CLX-APPLICATION-TO-INVOICE";

  /** Unapplied dispositions code. */
  public static final String UNAPPLIED_DISPOSITIONS = "CLX-UNAPPLIED-DISPOSITIONS";

  private static final String PERIOD = " at time zone 'Asia/Manila' as date) between :from and :to";

  private UnappliedReports() {}

  private static List<ParameterSpec> parameters() {
    return List.of(
        ParameterSpec.required(ClxReportSql.COMPANY, "Company", ParameterType.COMPANY),
        ParameterSpec.required(ClxReportSql.FROM, "From", ParameterType.DATE)
            .withDefault("MONTH_START"),
        ParameterSpec.required(ClxReportSql.TO, "To", ParameterType.DATE).withDefault("TODAY"));
  }

  private static Map<String, Object> args(ReportParameters p) {
    return Map.of(
        ClxReportSql.COMPANY, p.longValue(ClxReportSql.COMPANY),
        ClxReportSql.FROM, p.date(ClxReportSql.FROM),
        ClxReportSql.TO, p.date(ClxReportSql.TO));
  }

  /** The application requests of the period with the p.60 fields (BRCLXN.041). */
  @Component
  public static class ApplicationToInvoice implements ReportDefinition {

    private static final String SQL =
        "select r.requested_at, r.payment_date, r.payment_file_name, r.transaction_no,"
            + " r.paid_amount, r.currency, r.payment_type, r.payor_name, r.reference_no,"
            + " r.assured_name, r.invoice_no, r.requested_by, r.unapplied_ref, r.status,"
            + " r.cashiering_ref, r.file_run_no from clx_application_request r"
            + " where r.company_id = :companyId and r.action = 'APPLY_TO_INVOICE'"
            + " and cast(r.requested_at"
            + PERIOD
            + " order by r.id";

    private final ClxReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public ApplicationToInvoice(ClxReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.collections(
          APPLICATION_TO_INVOICE,
          "For Application To Invoice",
          "Collector requests to apply unapplied payments to invoices, with the fields of the daily"
              + " text file (BRCLXN.041)",
          parameters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.date("requested_at", "Requested On"),
              ReportColumn.date("payment_date", "Payment Date"),
              ReportColumn.text("payment_file_name", "Payment File Name"),
              ReportColumn.text("transaction_no", "Transaction No."),
              ReportColumn.amount("paid_amount", "Paid Amount"),
              ReportColumn.text("currency", "Currency"),
              ReportColumn.text("payment_type", "Payment Type"),
              ReportColumn.text("payor_name", "Payor"),
              ReportColumn.text("reference_no", "Reference No."),
              ReportColumn.text("assured_name", "Name of Assured"),
              ReportColumn.text("invoice_no", "Invoice No."),
              ReportColumn.text("requested_by", "User ID"),
              ReportColumn.text("unapplied_ref", "Unapplied Reference"),
              ReportColumn.text("status", "Status"),
              ReportColumn.text("cashiering_ref", "Cashiering Reference"),
              ReportColumn.text("file_run_no", "File"))
          .rows(sql.rows(SQL, args(p)))
          .presorted()
          .build();
    }
  }

  /** The collector dispositions of the period with their request (BRCLXN.031/033, 040). */
  @Component
  public static class UnappliedDispositions implements ReportDefinition {

    private static final String SQL =
        "select d.created_at, d.unapplied_ref, d.disposition_code, d.cashiering_action,"
            + " d.invoice_no, d.amount, d.remarks, d.created_by, r.status as request_status,"
            + " r.cashiering_ref from clx_unapplied_disposition d"
            + " left join clx_application_request r on r.id = d.request_id"
            + " where d.company_id = :companyId and cast(d.created_at"
            + PERIOD
            + " order by d.id";

    private final ClxReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public UnappliedDispositions(ClxReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.collections(
          UNAPPLIED_DISPOSITIONS,
          "Unapplied Payment Dispositions",
          "Collector dispositions of unapplied payments in the period with their Cashiering"
              + " request (BRCLXN.031/033, 040)",
          parameters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.date("created_at", "Date"),
              ReportColumn.text("unapplied_ref", "Unapplied Reference"),
              ReportColumn.text("disposition_code", "Disposition"),
              ReportColumn.text("cashiering_action", "Cashiering Action"),
              ReportColumn.text("invoice_no", "Invoice No."),
              ReportColumn.amountNoTotal("amount", "Amount"),
              ReportColumn.text("remarks", "Remarks"),
              ReportColumn.text("created_by", "User ID"),
              ReportColumn.text("request_status", "Request Status"),
              ReportColumn.text("cashiering_ref", "Cashiering Reference"))
          .rows(sql.rows(SQL, args(p)))
          .presorted()
          .withoutGrandTotal()
          .build();
    }
  }
}
