package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Name Matching Fall-out ({@value #CODE}; SNSRP-301, Report List p.23): the potential matches of
 * the screening runs of the period that scored at or above the matching threshold but below the
 * case threshold, so that Compliance can review the names that did not open a case, with the
 * account officer and the marketing business unit of the client. Sorted by score, highest first.
 */
@Component
public class NameMatchFalloutReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "SCR-NAME-MATCH-FALLOUT";

  private static final String SOURCE = "source";
  private static final String OFFICER = "accountOfficer";

  private static final String SQL =
      "select coalesce(k.case_no, 'M-' || m.id) as reference, m.score,"
          + " ing.ingest_date, m.entry_name || ' (' || m.source_code || ')' as listed_name,"
          + " m.client_name || ' (' || m.client_code || ')' as client,"
          + " sa.account_officer, sa.unit"
          + " from scr_match m join scr_screening_run r on r.id = m.run_id"
          + " left join scr_case k on k.id = m.case_id"
          + " left join lateral (select cast(coalesce(i.created_at, e.created_at) at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as ingest_date from scr_watchlist_entry e"
          + " left join scr_watchlist_change w on w.entry_id = e.id"
          + " left join scr_ingestion_run i on i.id = w.run_id"
          + " where e.id = m.entry_id order by w.id desc nulls last limit 1) ing on true"
          + " left join lateral (select a.account_officer,"
          + " coalesce(a.sales_department, a.sales_team) as unit from acc_account a"
          + " where a.client_id = m.client_id and a.account_officer is not null"
          + " order by a.id desc limit 1) sa on true"
          + " where m.company_id = :companyId and not m.case_threshold"
          + " and cast(r.started_at"
          + ScrReportSql.DAY
          + " between :fromDate and :toDate"
          + " and (cast(:source as varchar) is null or m.source_code = :source)"
          + " and (cast(:marketingUnit as varchar) is null or sa.unit = :marketingUnit)"
          + " and (cast(:accountOfficer as varchar) is null"
          + " or lower(sa.account_officer) = lower(:accountOfficer))"
          + " order by m.score desc, m.id";

  private final ScrReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public NameMatchFalloutReport(ScrReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ScrReportSql.company());
    params.addAll(ScrReportSql.period());
    params.add(ParameterSpec.optional(SOURCE, "List Source", ParameterType.TEXT));
    params.add(ParameterSpec.optional(ScrReportSql.UNIT, "Marketing Unit", ParameterType.TEXT));
    params.add(ParameterSpec.optional(OFFICER, "Account Officer", ParameterType.TEXT));
    return ReportMetadata.compliance(
        CODE,
        "Name Matching Fall-out",
        "Names that match but do not meet the criteria for case creation",
        params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = ScrReportSql.args(p);
    args.put(SOURCE, ScrReportSql.text(p, SOURCE));
    args.put(OFFICER, ScrReportSql.text(p, OFFICER));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("reference", "Reference"),
            ScrReportSql.number("score", "Matching Score"),
            ReportColumn.date("ingest_date", "Ingest Date of Sanction List"),
            ReportColumn.text("listed_name", "Name of Sanctioned Individual / Entity"),
            ReportColumn.text("client", "Name of Client / Client ID"),
            ReportColumn.text("account_officer", "Account Officer (in charge)"),
            ReportColumn.text("unit", "Marketing Business Unit"))
        .rows(sql.rows(SQL, args))
        .presorted()
        .withoutGrandTotal()
        .build();
  }
}
