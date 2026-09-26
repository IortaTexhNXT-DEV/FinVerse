package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Service Invoice Register (NB-SI-REG, BRNB.100 / 100b): the service invoices and credits issued in
 * the period per invoice type, with the booked invoice and ARN, recipient, commission, VAT,
 * withholding tax, net amount and the dispatch outcome with the failure reason.
 */
@Component
public class ServiceInvoiceRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-SI-REG";

  private static final String DISPATCH = "dispatch";

  private static final String SQL =
      "select coalesce(t.name, s.type_code) as type, s.si_no, s.issue_date, s.kind,"
          + " s.invoice_no, s.arn, s.recipient_name as recipient, s.commission,"
          + " s.vat_on_commission as vat, s.wtax_amount as wtax, s.net_amount as net,"
          + " s.dispatch_status as dispatch, s.dispatch_error as error, s.credit_of"
          + " from bkg_service_invoice s left join bkg_si_type t on t.code = s.type_code"
          + " where s.company_id = :company and s.issue_date between :from and :to"
          + " and (cast(:dispatch as varchar) is null or s.dispatch_status = :dispatch)"
          + " order by 1, s.issue_date, s.si_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public ServiceInvoiceRegisterReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Service Invoice Register",
        "Service invoices and credits issued, with their dispatch outcome (BRNB.100/100b)",
        Permission.BOOKING_PROCESS,
        true,
        ParameterSpec.select(
            DISPATCH,
            "Dispatch",
            List.of(NbReportSupport.ALL, "NOT_SENT", "QUEUED", "SENT", "FAILED"),
            NbReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args = NbReportSupport.args(p).with(DISPATCH, NbReportSupport.selected(p, DISPATCH));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(r -> NbReportSupport.relabel(NbReportSupport.relabel(r, DISPATCH), "kind"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("si_no", "Service Invoice"),
            ReportColumn.date("issue_date", "Issue Date"),
            ReportColumn.text("kind", "Kind"),
            ReportColumn.text("invoice_no", "Booked Invoice"),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("recipient", "Recipient"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.amount("vat", "VAT"),
            ReportColumn.amount("wtax", "Withholding Tax"),
            ReportColumn.amount("net", "Net Amount"),
            ReportColumn.text(DISPATCH, "Dispatch"),
            ReportColumn.text("error", "Failure Reason"),
            ReportColumn.text("credit_of", "Credit Of"))
        .groupBy("type", "Invoice Type")
        .rows(rows)
        .presorted()
        .build();
  }
}
