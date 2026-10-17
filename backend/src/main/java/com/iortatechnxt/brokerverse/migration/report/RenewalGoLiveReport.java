package com.iortatechnxt.brokerverse.migration.report;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
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
 * Go-live renewal extraction check ({@code MIG-RENEWAL-GOLIVE}; DATA_MIGRATION_DESIGN section
 * 15.1): per expiry month of the go-live window, the migrated headers expiring, the candidates the
 * go-live extraction creates (not renewed in legacy), the headers renewed in legacy, the renewal
 * advices already sent (P03) and the urgent candidates. Headers = candidates + renewed.
 */
@Component
public class RenewalGoLiveReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "MIG-RENEWAL-GOLIVE";

  private static final String SQL =
      "select date_trunc('month', a.period_to)::date as expiry_month, count(*) as headers,"
          + " count(*) filter (where not x.renewed) as candidates,"
          + " count(*) filter (where x.renewed) as renewed_in_legacy,"
          + " count(r.id) filter (where not x.renewed) as ra_sent,"
          + " count(*) filter (where not x.renewed and a.period_to <= :urgentTo) as urgent,"
          + " sum(a.gross_premium) as premium"
          + " from acc_account_legacy l join acc_account a on a.id = l.account_id"
          + " cross join lateral (select exists (select 1 from acc_account_legacy n"
          + " join acc_account b on b.id = n.account_id where n.company_id = l.company_id"
          + " and n.cover_no = l.cover_no and n.rolled_back_at is null"
          + " and b.period_from = a.period_to) as renewed) x"
          + " left join mig_ra_sent r on r.company_id = l.company_id"
          + " and r.legacy_policy_ref = l.legacy_ref and not r.rolled_back"
          + " where l.company_id = :company and l.rolled_back_at is null"
          + " and a.status <> 'CANCELLED' and a.period_to between :goLive and :windowTo"
          + " group by 1 order by 1";

  private final NbReportJdbc jdbc;
  private final MigrationParameters parameters;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param parameters migration parameters (window and urgent dates)
   */
  public RenewalGoLiveReport(NbReportJdbc jdbc, MigrationParameters parameters) {
    this.jdbc = jdbc;
    this.parameters = parameters;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "Go-live Renewal Extraction Check",
        ReportCategory.DATA_MIGRATION,
        "Migrated headers expiring in the go-live window: candidates, renewed in legacy, advices"
            + " already sent and urgent candidates per expiry month",
        List.of(ParameterSpec.required(MigReport.COMPANY, "Company", ParameterType.COMPANY)),
        Permission.MIG_VIEW,
        Permission.MIG_VIEW,
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    SqlArgs args =
        SqlArgs.company(p.longValue(MigReport.COMPANY))
            .with("goLive", parameters.cutoverDate())
            .with("windowTo", parameters.goLiveRenewalTo())
            .with("urgentTo", parameters.renewalUrgentTo());
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("expiry_month", "Expiry month"),
            ReportColumn.count("headers", "Headers expiring"),
            ReportColumn.count("candidates", "Candidates"),
            ReportColumn.count("renewed_in_legacy", "Renewed in legacy"),
            ReportColumn.count("ra_sent", "Advice already sent"),
            ReportColumn.count("urgent", "Urgent"),
            ReportColumn.amount("premium", "Gross premium"))
        .rows(jdbc.rows(SQL, args.map()))
        .presorted()
        .note(
            "Window "
                + parameters.cutoverDate()
                + " to "
                + parameters.goLiveRenewalTo()
                + "; urgent up to "
                + parameters.renewalUrgentTo())
        .build();
  }
}
