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
 * Franchise (EB-FRANCHISE; FR-EB-032, 060): the franchise requests sent in the period by insurer,
 * with the programme, dates sent and due, the insurer's decision and reason, the days to decide
 * and the advice to the client.
 */
@Component
public class EbFranchiseReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-FRANCHISE";

  private static final String SENT =
      "cast(f.submitted_at at time zone '" + BusinessClock.zoneId() + "' as date)";

  private static final String SQL =
      "select f.insurer_code as insurer, f.franchise_no,"
          + " p.programme_no || ' - ' || p.client_name as programme, " + SENT + " as sent,"
          + " f.due_date, coalesce(f.decision, f.status) as decision, f.decided_on,"
          + " case when f.decided_on is null then null else f.decided_on - " + SENT + " end as days,"
          + " f.reason_code, cast(f.advised_at at time zone '" + BusinessClock.zoneId()
          + "' as date) as advised, f.status"
          + " from eb_franchise_request f join eb_programme p on p.id = f.programme_id"
          + " join eb_cycle c on c.id = f.cycle_id"
          + " where f.company_id = :company and f.submitted_at is not null"
          + " and " + SENT + " between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:insurer as varchar) is null or f.insurer_code = :insurer)"
          + " and (cast(:businessType as varchar) is null or c.business_type = :businessType)"
          + " and (cast(:benefitLine as varchar) is null or exists (select 1 from eb_programme_line l"
          + " where l.programme_id = p.id and l.benefit_line = :benefitLine))"
          + " order by f.insurer_code, f.submitted_at, f.id";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbFranchiseReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Franchise",
            "Franchise requests, outcomes and response times per insurer")
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var rows =
        jdbc.rows(SQL, EbReportSupport.args(p).map()).stream()
            .map(r -> EbReportSupport.relabel(r, "decision", "reason_code", "status"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("franchise_no", "Franchise No."),
            ReportColumn.text("programme", "Programme / Client"),
            ReportColumn.date("sent", "Sent"),
            ReportColumn.date("due_date", "Due"),
            ReportColumn.text("decision", "Decision"),
            ReportColumn.date("decided_on", "Decided On"),
            ReportColumn.count("days", "Days to Decide"),
            ReportColumn.text("reason_code", "Reason"),
            ReportColumn.date("advised", "Client Advised"),
            ReportColumn.text("status", "Status"))
        .groupBy("insurer", "Insurer")
        .rows(rows)
        .presorted()
        .build();
  }
}
