package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * New Business (EB-NEW-BUSINESS; FR-EB-061, BRID-022.01): the accounts of the EB programmes created
 * in the period with client, line and product, effective date, insurer selected, premium,
 * commission, account officer, business type (the Business Type filter; New Business by default
 * reading), date and status. Exported to Word like every EB report.
 */
@Component
public class EbNewBusinessReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-NEW-BUSINESS";

  private static final String SQL =
      "select a.client_name as client, l.benefit_line || ' / ' || a.product_code as line_product,"
          + " a.period_from as effective_date, a.insurer_code as insurer, a.gross_premium as premium,"
          + " a.commission, a.account_officer as ao, a.business_type,"
          + " cast(a.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as created_on, a.status, a.arn"
          + " from acc_account a"
          + " join (select distinct programme_id, arn from eb_programme_arn_v) pa on pa.arn = a.arn"
          + " join eb_programme p on p.id = pa.programme_id"
          + " left join lateral (select x.benefit_line from eb_programme_line x"
          + " where x.programme_id = p.id and (x.current_arn = a.arn or x.product_code = a.product_code)"
          + " order by x.line_no limit 1) l on true"
          + " where a.company_id = :company"
          + " and cast(a.created_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:benefitLine as varchar) is null or l.benefit_line = :benefitLine)"
          + " and (cast(:insurer as varchar) is null or a.insurer_code = :insurer)"
          + " and (cast(:businessType as varchar) is null or a.business_type = :businessType)"
          + " order by a.created_at, a.arn";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbNewBusinessReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits New Business",
            "Accounts of the Employee Benefits programmes created in the period")
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, EbReportSupport.args(p).map()).stream()
            .map(r -> EbReportSupport.relabel(r, "business_type", "status"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("client", "Client"),
            ReportColumn.text("line_product", "Line / Product"),
            ReportColumn.date("effective_date", "Effective Date"),
            ReportColumn.text("insurer", "Insurer Selected"),
            ReportColumn.amount("premium", "Premium"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.text("ao", "Account Officer"),
            ReportColumn.text("business_type", "Business Type"),
            ReportColumn.date("created_on", "Date"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("arn", "ARN"))
        .rows(rows)
        .presorted()
        .build();
  }
}
