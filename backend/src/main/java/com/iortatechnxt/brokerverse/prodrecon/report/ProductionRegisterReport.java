package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Production Register (PRC-REGISTER, Annex IV #5, PRCID.017/018/020): the booked invoices sent to
 * the insurers in the period as last extracted, with the variants plain, with the insurer's
 * feedback, with Marketing's feedback, or both. Estimated items are labelled (RMTID.037).
 */
@Component
public class ProductionRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PRC-REGISTER";

  private static final String VARIANT = "variant";
  private static final String BOTH = "BOTH";

  private static final String SQL =
      "select * from (select distinct on (c.id, l.invoice_no) c.insurer_code as insurer,"
          + " to_char(c.production_month, 'YYYY-MM') as month, l.invoice_no, l.booking_date,"
          + " l.inception_date, l.expiry_date, l.policy_no, l.endorsement_no, l.pn_nos,"
          + " l.assured_name, l.risk_code, l.basic_premium, l.gross_commission, l.client_code,"
          + " l.gross_premium, l.booked_vat, l.amount_paid, l.date_paid, l.ar_number, l.kind,"
          + " l.remittance_status, i.ins_remarks as remarks, i.insurer_feedback,"
          + " i.marketing_feedback, case when l.estimated then 'Estimated' else '' end as estimated"
          + " from prc_extract_line l join prc_extract e on e.id = l.extract_id"
          + " join prc_cycle c on c.id = e.cycle_id"
          + " left join prc_item i on i.cycle_id = c.id and i.invoice_no = l.invoice_no"
          + " where c.company_id = :companyId and c.production_month between :from and :to"
          + " and (cast(:insurer as varchar) is null or c.insurer_code = :insurer)"
          + " order by c.id, l.invoice_no, e.id desc) x order by insurer, month, invoice_no";

  private final ReconReportSupport support;

  /**
   * Creates the report.
   *
   * @param support report SQL
   */
  public ProductionRegisterReport(ReconReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return ReconReportSupport.metadata(
        CODE,
        "Production Register",
        "Booked accounts sent to the insurers, plain or with insurer / Marketing feedback"
            + " (PRCID.017/018/020)",
        ParameterSpec.select(
            VARIANT, "Variant", List.of("PLAIN", "INSURER", "MARKETING", BOTH), "PLAIN"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String variant = p.optionalText(VARIANT).orElse("PLAIN");
    List<ReportColumn> columns = new ArrayList<>(base());
    if ("INSURER".equals(variant) || BOTH.equals(variant)) {
      columns.add(ReportColumn.text("remarks", "Insurer Remarks"));
      columns.add(ReportColumn.text("insurer_feedback", "Insurer Feedback"));
    }
    if ("MARKETING".equals(variant) || BOTH.equals(variant)) {
      columns.add(ReportColumn.text("marketing_feedback", "Marketing Feedback"));
    }
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy("insurer", "Insurer")
        .rows(support.rows(SQL, ReconReportSupport.args(p)))
        .presorted()
        .build();
  }

  private static List<ReportColumn> base() {
    return List.of(
        ReportColumn.text("month", "Month of Production"),
        ReportColumn.text("invoice_no", "Invoice Number"),
        ReportColumn.date("booking_date", "Booking Date"),
        ReportColumn.date("inception_date", "Inception Date"),
        ReportColumn.date("expiry_date", "Expiry Date"),
        ReportColumn.text("policy_no", "Policy No."),
        ReportColumn.text("endorsement_no", "Endorsement No."),
        ReportColumn.text("pn_nos", "PN No."),
        ReportColumn.text("assured_name", "Assured Name"),
        ReportColumn.text("risk_code", "Risk Type"),
        ReportColumn.amount("basic_premium", "Basic Premium"),
        ReportColumn.amount("gross_commission", "Gross Commission"),
        ReportColumn.text("client_code", "A/R Client"),
        ReportColumn.amount("gross_premium", "Gross Premium"),
        ReportColumn.amount("booked_vat", "Booked VAT"),
        ReportColumn.amount("amount_paid", "Amount Paid"),
        ReportColumn.date("date_paid", "Date Paid"),
        ReportColumn.text("ar_number", "AR / OR Number"),
        ReportColumn.text("kind", "Adjustment Type"),
        ReportColumn.text("remittance_status", "Remittance Status"),
        ReportColumn.text("estimated", "Estimated"));
  }
}
