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
 * Renewal (EB-RENEWAL; FR-EB-060, FRS columns BRID-022 AC5): the programme lines expiring in the
 * period with the renewal advice sent date, the stage of the renewal cycle, the proposals received,
 * the comparative result, the client's revision requests, the BOR status, the TOR version and
 * whether the renewal is remarketed.
 */
@Component
public class EbRenewalReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-RENEWAL";

  private static final String SQL =
      "select p.programme_no || ' - ' || p.client_name as programme, l.benefit_line,"
          + " p.team_code || ' / ' || p.account_officer as team_ao, l.period_to as expiry,"
          + " cast(ra.sent_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as ra_sent,"
          + " coalesce(c.stage, 'NO_CYCLE') as status,"
          + " (select count(*) from eb_proposal pr where pr.cycle_id = c.id"
          + " and pr.status = 'VALIDATED') as proposals,"
          + " (select pr.insurer_code || ' ' || pl.premium from eb_comparative cp"
          + " join eb_comparative_line cl on cl.comparative_id = cp.id and cl.benefit_line = l.benefit_line"
          + " join eb_proposal pr on pr.id = cl.recommended_proposal_id"
          + " cross join lateral (select to_char(sum(x.annual_premium), 'FM999,999,999,990.00') as premium"
          + " from eb_proposal_line x where x.proposal_id = pr.id and x.benefit_line = l.benefit_line) pl"
          + " where cp.cycle_id = c.id and cp.status <> 'SUPERSEDED'"
          + " order by cp.version_no desc limit 1) as comparative_result,"
          + " (select count(*) from eb_revision_request r where r.cycle_id = c.id) as client_changes,"
          + " coalesce((select b.status from eb_bor b where b.programme_id = p.id"
          + " order by b.id desc limit 1), 'NONE') || ' / TOR '"
          + " || coalesce((select cast(max(t.version_no) as varchar) from eb_tor t where t.cycle_id = c.id"
          + " and t.status <> 'DRAFT'), '-') || ' / '"
          + " || case when c.remarketing then 'Remarketed' else 'Incumbent' end as bor_tor"
          + " from eb_programme_line l"
          + " join eb_programme p on p.id = l.programme_id"
          + " left join lateral (select * from eb_cycle x where x.programme_id = p.id"
          + " and x.business_type = 'RENEWAL' order by x.policy_year desc, x.id desc limit 1) c on true"
          + " left join eb_renewal_advice ra on ra.cycle_id = c.id"
          + " where p.company_id = :company and l.active and l.period_to between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:benefitLine as varchar) is null or l.benefit_line = :benefitLine)"
          + " and (cast(:insurer as varchar) is null or l.incumbent_insurer = :insurer)"
          + " and (cast(:businessType as varchar) is null or cast(:businessType as varchar) = 'RENEWAL')"
          + " order by l.period_to, p.programme_no, l.line_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbRenewalReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Renewal",
            "Programmes due for renewal in the period and the progress of their renewal")
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, EbReportSupport.args(p).map()).stream()
            .map(r -> EbReportSupport.relabel(r, "status"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("programme", "Programme / Client"),
            ReportColumn.text("benefit_line", "Benefit Line"),
            ReportColumn.text("team_ao", "Team / AO"),
            ReportColumn.date("expiry", "Expiry"),
            ReportColumn.date("ra_sent", "RA Sent"),
            ReportColumn.text("status", "Status"),
            ReportColumn.count("proposals", "Proposals Received"),
            ReportColumn.text("comparative_result", "Comparative Result"),
            ReportColumn.count("client_changes", "Client Changes"),
            ReportColumn.text("bor_tor", "BOR / TOR / Remarketing"))
        .rows(rows)
        .presorted()
        .build();
  }
}
