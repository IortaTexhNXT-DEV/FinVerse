package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
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
 * Booked Accounts Register (NB-BOOKED-REG, BRNB.027 / 036 / 075 / 108): the invoices booked in the
 * period (bookings, endorsements and cancellations) with ARN, client, insurer, product, policy,
 * premium and charges, commission, cost center, officer, incentive flag and payment arrangement,
 * grouped by product line. Filter Business Type (BRID-022.01, shared work item BT0).
 */
@Component
public class BookedAccountsRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-BOOKED-REG";

  private static final String KIND = "kind";
  private static final String INSURER = "insurer";

  private static final String SQL =
      "select i.line_code as line, i.booking_date, i.invoice_no, i.arn, i.client_name as client,"
          + " coalesce(n.name, i.insurer_code) as insurer, i.risk_code as product, i.policy_no,"
          + " i.kind, i.basic_premium as premium,"
          + " i.dst + i.premium_tax_vat + i.lgt + i.fst + i.other_charges as charges,"
          + " i.basic_premium + i.dst + i.premium_tax_vat + i.lgt + i.fst + i.other_charges"
          + " as gross, i.commission, i.vat_on_commission as vat, i.cost_center,"
          + " i.account_officer as officer, i.incentive_eligible as incentive,"
          + " i.direct_payment as direct, i.business_type"
          + " from bkg_invoice i left join cat_insurer n on n.company_id = i.company_id"
          + " and n.party_code = i.insurer_code"
          + " where i.company_id = :company and i.status = 'BOOKED'"
          + " and i.booking_date between :from and :to"
          + " and (cast(:kind as varchar) is null or i.kind = :kind)"
          + " and (cast(:insurer as varchar) is null or i.insurer_code = :insurer)"
          + " and (cast(:businessType as varchar) is null or i.business_type = :businessType)"
          + " order by i.line_code, i.booking_date, i.invoice_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public BookedAccountsRegisterReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Booked Accounts Register",
        "Invoices booked in the period with premium, commission and cost center"
            + " (BRNB.027/075/108)",
        Permission.ACCOUNT_VIEW,
        true,
        ParameterSpec.select(
            KIND,
            "Transaction",
            List.of(
                NbReportSupport.ALL,
                "BOOKING",
                "ENDORSEMENT_PLUS",
                "ENDORSEMENT_MINUS",
                "CANCELLATION"),
            NbReportSupport.ALL),
        ParameterSpec.optional(INSURER, "Insurer Code", ParameterType.TEXT),
        NbReportSupport.businessTypeFilter());
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args =
        NbReportSupport.args(p)
            .with(KIND, NbReportSupport.selected(p, KIND))
            .with(INSURER, NbReportSupport.upper(p, INSURER))
            .with(
                NbReportSupport.BUSINESS_TYPE,
                NbReportSupport.selected(p, NbReportSupport.BUSINESS_TYPE));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(
                r ->
                    NbReportSupport.relabel(
                        NbReportSupport.relabel(
                            NbReportSupport.flag(NbReportSupport.flag(r, "incentive"), "direct"),
                            KIND),
                        "business_type"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("booking_date", "Booking Date"),
            ReportColumn.text("invoice_no", "Invoice No."),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("client", "Client"),
            ReportColumn.text(INSURER, "Insurer"),
            ReportColumn.text("product", "Product"),
            ReportColumn.text("policy_no", "Policy No."),
            ReportColumn.text(KIND, "Transaction"),
            ReportColumn.text("business_type", "NB / Renewal"),
            ReportColumn.amount("premium", "Basic Premium"),
            ReportColumn.amount("charges", "Taxes & Charges"),
            ReportColumn.amount("gross", "Gross Premium"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.amount("vat", "VAT on Commission"),
            ReportColumn.text("cost_center", "Cost Center"),
            ReportColumn.text("officer", "Officer"),
            ReportColumn.text("incentive", "Incentive"),
            ReportColumn.text("direct", "Direct Payment"))
        .groupBy("line", "Product Line")
        .rows(rows)
        .presorted()
        .build();
  }
}
