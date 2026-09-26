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
 * DTIP Status reports (Annex III #5 and #6, RMTID.028/032/039): the due to insurer of the booked
 * invoices of the Operations ledger (direct payment excluded) as of today, per insurer and
 * remittance status, and in detail per invoice with the collection, remittance and ageing facts.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the reports
public final class DtipStatusReports {

  private static final String INSURER = "insurer";
  private static final String FILTER =
      " from ops_invoice i join ops_invoice_component d on d.invoice_id = i.id"
          + " and d.component = 'DTIP'"
          + " where i.company_id = :companyId and i.dp_flag = false"
          + " and (cast(:insurer as varchar) is null or i.insurer_code = :insurer)";

  private DtipStatusReports() {}

  /** REM-DTIP-SUMMARY (Annex III #5). */
  @Component
  public static class Summary implements ReportDefinition {

    private static final String SQL =
        "select i.insurer_code as insurer, i.remittance_status as status, count(*) as accounts,"
            + " sum(d.balance) as amount"
            + FILTER
            + " group by i.insurer_code, i.remittance_status order by i.insurer_code, i.remittance_status";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Summary(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-DTIP-SUMMARY",
          "DTIP Status Report - Summary",
          "Outstanding due to insurers per insurer and remittance status (RMTID.028/039)",
          false);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("status", "Remittance Status"),
              new ReportColumn("accounts", "No. of Accounts", ColumnType.NUMBER, true),
              ReportColumn.amount("amount", "Amount"))
          .groupBy(INSURER, "Insurance Company")
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .build();
    }
  }

  /** REM-DTIP-DETAIL (Annex III #6). */
  @Component
  public static class Detail implements ReportDefinition {

    private static final String SQL =
        "select i.insurer_code as insurer, i.invoice_no, i.assured_name, i.inception_date,"
            + " i.expiry_date, i.risk_code, i.product_line, i.remittance_status,"
            + " (select c.booked + c.adjusted from ops_invoice_component c where c.invoice_id = i.id"
            + " and c.component = 'BASIC') as basic,"
            + " (select sum(c.applied - c.reversed) from ops_invoice_component c"
            + " where c.invoice_id = i.id and c.component in"
            + " ('BASIC','DST','PREMIUM_TAX_VAT','LGT','FST','OTHER')) as paid_ar,"
            + " (select sum(c.applied - c.reversed) from ops_invoice_component c"
            + " where c.invoice_id = i.id and c.component in"
            + " ('BASIC','DST','PREMIUM_TAX_VAT','LGT','FST','OTHER')) - d.remitted as for_remittance,"
            + " (select c.remitted from ops_invoice_component c where c.invoice_id = i.id"
            + " and c.component = 'COMMISSION') as commission_collected,"
            + " (select max(m.value_date) from ops_invoice_movement m where m.invoice_id = i.id"
            + " and m.movement_type = 'REMITTED') as last_remitted,"
            + " (select max(m.value_date) from ops_invoice_movement m where m.invoice_id = i.id"
            + " and m.movement_type = 'APPLIED') as last_paid,"
            + " current_date - i.inception_date as age_inception,"
            + " d.balance as dtip,"
            + " (select sum(c.balance) from ops_invoice_component c where c.invoice_id = i.id"
            + " and c.component in ('BASIC','DST','PREMIUM_TAX_VAT','LGT','FST','OTHER')) as ra,"
            + " (select c.balance from ops_invoice_component c where c.invoice_id = i.id"
            + " and c.component = 'COMMISSION') as commission_due,"
            + " (select c.balance from ops_invoice_component c where c.invoice_id = i.id"
            + " and c.component = 'COMMISSION_VAT') as vat_due"
            + FILTER
            + " and d.balance <> 0 order by i.insurer_code, i.invoice_no";

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Detail(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceReportSql.metadata(
          "REM-DTIP-DETAIL",
          "DTIP Status Report - Detailed",
          "Invoices with outstanding due to insurer, collections and remittances (RMTID.028/039)",
          false);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("invoice_no", "Invoice Number"),
              ReportColumn.text("assured_name", "Name of Assured"),
              ReportColumn.date("inception_date", "Inception Date"),
              ReportColumn.date("expiry_date", "Date of Expiry"),
              ReportColumn.text("risk_code", "Risk Code"),
              ReportColumn.text("product_line", "Product Type"),
              ReportColumn.text("remittance_status", "Remittance Status"),
              ReportColumn.amount("basic", "Basic Premium"),
              ReportColumn.amount("paid_ar", "AR Paid"),
              ReportColumn.amount("for_remittance", "AP / For Remittance"),
              ReportColumn.amount("commission_collected", "Commission Collected"),
              ReportColumn.date("last_remitted", "Date of Last Remittance"),
              ReportColumn.date("last_paid", "Date of Last Payment"),
              new ReportColumn("age_inception", "Aging from Inception", ColumnType.NUMBER, false),
              ReportColumn.amount("dtip", "Outstanding DTIP"),
              ReportColumn.amount("ra", "Outstanding RA"),
              ReportColumn.amount("commission_due", "Uncollected Commission"),
              ReportColumn.amount("vat_due", "Uncollected VAT"))
          .groupBy(INSURER, "Insurance Company")
          .rows(sql.rows(SQL, RemittanceReportSql.args(p)))
          .presorted()
          .note("CAT collected and the ageing from last payment wait for their definitions (OQ42).")
          .build();
    }
  }
}
