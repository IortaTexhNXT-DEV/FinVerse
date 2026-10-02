package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Production (EB-PRODUCTION; FR-EB-060, 061): premium and commission of the booked invoices of the
 * EB programmes in the period, by team, account officer, benefit line, insurer and business type.
 */
@Component
public class EbProductionReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-PRODUCTION";

  private static final String SQL =
      "select p.team_code as team, coalesce(b.account_officer, p.account_officer) as ao,"
          + " b.line_code as benefit_line, b.insurer_code as insurer, b.business_type,"
          + " count(*) as invoices,"
          + " sum(b.basic_premium + b.dst + b.premium_tax_vat + b.lgt + b.fst + b.other_charges)"
          + " as gross_premium, sum(b.commission) as commission"
          + " from bkg_invoice b"
          + " join (select distinct programme_id, arn from eb_programme_arn_v) a on a.arn = b.arn"
          + " join eb_programme p on p.id = a.programme_id"
          + " where b.company_id = :company and b.status = 'BOOKED'"
          + " and b.booking_date between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:benefitLine as varchar) is null or b.line_code = :benefitLine)"
          + " and (cast(:insurer as varchar) is null or b.insurer_code = :insurer)"
          + " and (cast(:businessType as varchar) is null or b.business_type = :businessType)"
          + " group by p.team_code, coalesce(b.account_officer, p.account_officer), b.line_code,"
          + " b.insurer_code, b.business_type"
          + " order by p.team_code, 2, b.line_code, b.insurer_code, b.business_type";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbProductionReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Production",
            "Premium and commission booked by team, account officer, benefit line, insurer and"
                + " business type")
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, EbReportSupport.args(p).map()).stream()
            .map(r -> EbReportSupport.relabel(r, "business_type"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("ao", "Account Officer"),
            ReportColumn.text("benefit_line", "Benefit Line"),
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("business_type", "Business Type"),
            ReportColumn.count("invoices", "Invoices"),
            ReportColumn.amount("gross_premium", "Gross Premium"),
            ReportColumn.amount("commission", "Commission"))
        .groupBy("team", "Team")
        .rows(rows)
        .presorted()
        .build();
  }
}
