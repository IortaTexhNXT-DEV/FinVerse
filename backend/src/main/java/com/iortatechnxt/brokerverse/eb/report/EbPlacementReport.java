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
 * Placement (EB-PLACEMENT; FR-EB-060, FRS columns BRID-022 AC6): the lines confirmed by the client
 * in the period with the confirmation date and channel, the insurer selected, the coverage, the
 * premium and commission, the proposal versions and revisions, the final terms, the last document
 * upload and the account created with its stage.
 */
@Component
public class EbPlacementReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-PLACEMENT";

  private static final String SQL =
      "select p.programme_no || ' - ' || p.client_name as programme,"
          + " cf.confirmed_on, cf.channel, cl.insurer_code as insurer,"
          + " cl.benefit_line || ' (' || coalesce((select string_agg(x.plan_code, ', ' order by x.sort_order)"
          + " from eb_proposal_line x where x.proposal_id = pr.id and x.benefit_line = cl.benefit_line), '')"
          + " || ')' as coverage, cl.annual_premium as premium,"
          + " (select sum(b.commission) from bkg_invoice b where b.arn = cl.account_arn"
          + " and b.status = 'BOOKED') as commission,"
          + " (select count(*) from eb_proposal y where y.cycle_id = cf.cycle_id)"
          + " + (select count(*) from eb_revision_request r where r.cycle_id = cf.cycle_id)"
          + " as versions_revisions,"
          + " pr.proposal_no || ' v' || pr.version_no as final_terms,"
          + " (select cast(max(d.created_at) at time zone '"
          + BusinessClock.zoneId()
          + "' as date) from eb_document d where d.cycle_id = cf.cycle_id) as last_upload,"
          + " coalesce(cl.account_arn, '-') || ' / ' || coalesce(a.status, 'NOT_PLACED') as arn_stage"
          + " from eb_client_confirmation cf"
          + " join eb_confirmation_line cl on cl.confirmation_id = cf.id"
          + " join eb_proposal pr on pr.id = cl.proposal_id"
          + " join eb_cycle c on c.id = cf.cycle_id"
          + " join eb_programme p on p.id = cf.programme_id"
          + " left join acc_account a on a.arn = cl.account_arn"
          + " where cf.company_id = :company and cf.status = 'ACTIVE'"
          + " and cf.confirmed_on between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:benefitLine as varchar) is null or cl.benefit_line = :benefitLine)"
          + " and (cast(:insurer as varchar) is null or cl.insurer_code = :insurer)"
          + " and (cast(:businessType as varchar) is null or c.business_type = :businessType)"
          + " order by cf.confirmed_on, p.programme_no, cl.line_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbPlacementReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Placement",
            "Lines confirmed by the client with the insurer selected, final terms and accounts")
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, EbReportSupport.args(p).map()).stream()
            .map(r -> EbReportSupport.relabel(r, "channel"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("programme", "Programme / Client"),
            ReportColumn.date("confirmed_on", "Client Confirmation"),
            ReportColumn.text("channel", "Channel"),
            ReportColumn.text("insurer", "Insurer Selected"),
            ReportColumn.text("coverage", "Coverage"),
            ReportColumn.amount("premium", "Annual Premium"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.count("versions_revisions", "Proposals and Revisions"),
            ReportColumn.text("final_terms", "Final Terms"),
            ReportColumn.date("last_upload", "Last Document Upload"),
            ReportColumn.text("arn_stage", "ARN / Stage"))
        .rows(rows)
        .presorted()
        .build();
  }
}
