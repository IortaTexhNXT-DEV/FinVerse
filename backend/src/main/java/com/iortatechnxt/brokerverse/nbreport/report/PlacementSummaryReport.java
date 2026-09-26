package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import org.springframework.stereotype.Component;

/**
 * Placement Summary (NB-PLC-SUMMARY, BRNB.075, replacing the QPS placement report of OOS-2): per
 * insurer and branch, the placement slips generated in the period, the accounts on them, the slips
 * sent, resent (sent again or regenerated after a return) and the slip e-mails that failed or are
 * still queued (from the messaging send log).
 */
@Component
public class PlacementSummaryReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-PLC-SUMMARY";

  private static final String SQL =
      "select coalesce(i.name, s.insurer_code) as insurer, s.branch_code as branch,"
          + " count(*) as slips, coalesce(sum(sa.accounts), 0) as accounts,"
          + " count(*) filter (where s.send_count > 0) as sent,"
          + " count(*) filter (where s.send_count > 1 or s.version_no > 1) as resent,"
          + " coalesce(sum(m.failed), 0) as failed, coalesce(sum(m.queued), 0) as queued,"
          + " count(*) filter (where s.status = 'SUPERSEDED') as superseded"
          + " from plc_slip s"
          + " left join cat_insurer i on i.company_id = s.company_id"
          + " and i.party_code = s.insurer_code"
          + " left join lateral (select count(*) as accounts from plc_slip_account x"
          + " where x.slip_id = s.id) sa on true"
          + " left join lateral (select count(*) filter (where o.status = 'FAILED') as failed,"
          + " count(*) filter (where o.status = 'QUEUED') as queued from msg_outbound o"
          + " where o.entity_type = 'PlacementSlip' and o.entity_id = cast(s.id as varchar)"
          + " and o.purpose = 'PLACEMENT_SLIP') m on true"
          + " where s.company_id = :company"
          + " and cast(s.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " group by coalesce(i.name, s.insurer_code), s.branch_code order by 1, 2";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public PlacementSummaryReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Placement Summary",
        "Placement slips sent, failed and resent by insurer (BRNB.075)",
        Permission.PLACEMENT_MANAGE,
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("branch", "Branch"),
            ReportColumn.count("slips", "Slips"),
            ReportColumn.count("accounts", "Accounts"),
            ReportColumn.count("sent", "Sent"),
            ReportColumn.count("resent", "Resent"),
            ReportColumn.count("failed", "Failed E-mails"),
            ReportColumn.count("queued", "Queued E-mails"),
            ReportColumn.count("superseded", "Superseded"))
        .rows(jdbc.rows(SQL, NbReportSupport.args(p).map()))
        .presorted()
        .build();
  }
}
