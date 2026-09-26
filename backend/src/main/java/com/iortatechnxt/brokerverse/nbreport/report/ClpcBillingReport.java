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
 * CLPC Billing Report (NB-CLPC-BILLING, BRNB.067 / 075): the CBG Fire accounts billed to CLPC in
 * the period, per billing batch, with the promissory note and loan application numbers, borrower,
 * originating unit, premium, amortisation and the payment status returned by CLPC.
 */
@Component
public class ClpcBillingReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-CLPC-BILLING";

  private static final String PAYMENT = "payment";

  private static final String SQL =
      "select b.batch_no || ' (' || to_char(b.billing_date, 'DD-MM-YYYY') || ')' as batch,"
          + " t.line_no, t.arn, t.pn_numbers as pn, t.loan_application_no as loan,"
          + " t.borrower, t.originating_unit as unit, t.bdoi_location as location,"
          + " t.booking_date, t.premium, t.amortised, t.payment_status as payment"
          + " from plc_billing_batch b join plc_billing_item t on t.batch_id = b.id"
          + " where b.company_id = :company and b.billing_date between :from and :to"
          + " and (cast(:payment as varchar) is null or t.payment_status = :payment)"
          + " order by b.billing_date, b.batch_no, t.line_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public ClpcBillingReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "CLPC Billing Report",
        "CBG Fire accounts billed to CLPC with their payment status (BRNB.067/075)",
        Permission.BILLING_MANAGE,
        true,
        ParameterSpec.select(
            PAYMENT,
            "Payment Status",
            List.of(NbReportSupport.ALL, "BILLED", "PAID", "UNPAID"),
            NbReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args = NbReportSupport.args(p).with(PAYMENT, NbReportSupport.selected(p, PAYMENT));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(r -> NbReportSupport.relabel(NbReportSupport.flag(r, "amortised"), PAYMENT))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("pn", "PN No."),
            ReportColumn.text("loan", "Loan Application No."),
            ReportColumn.text("borrower", "Borrower"),
            ReportColumn.text("unit", "Originating Unit"),
            ReportColumn.text("location", "BDOI Location"),
            ReportColumn.date("booking_date", "Booking Date"),
            ReportColumn.amount("premium", "Premium"),
            ReportColumn.text("amortised", "Amortised"),
            ReportColumn.text(PAYMENT, "Payment"))
        .groupBy("batch", "Billing Batch")
        .rows(rows)
        .presorted()
        .build();
  }
}
