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
import org.springframework.stereotype.Component;

/**
 * Placement Update Report (NB-PLC-UPDATE, BRNB.011), individual and collective: where each account
 * stands with its insurer - stage and since when, placement slip, placement date, hold cover,
 * policy issue and the last insurer return. Collective: every account whose stage changed in the
 * period, grouped by insurer; individual: one ARN regardless of the period.
 */
@Component
public class PlacementUpdateReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-PLC-UPDATE";

  private static final String ARN = "arn";
  private static final String INSURER = "insurer";
  private static final String OFFICER = "officer";

  private static final String SQL =
      "select a.arn, a.client_name as client, a.product_code as product,"
          + " coalesce(i.name, a.insurer_code, '(not chosen)') as insurer, s.name as stage,"
          + " c.stage_entered_at as since, a.placement_slip_ref as slip, a.placed_at as placed,"
          + " a.hold_cover_status as hold_cover, a.hold_cover_ref as hold_ref,"
          + " a.policy_issue_date as issued, r.reason_code as returned, r.remarks,"
          + " a.account_officer as officer"
          + " from acc_account a"
          + " join wf_case c on c.entity_type = 'Account' and c.entity_id = cast(a.id as varchar)"
          + " join wf_stage s on s.workflow_code = c.workflow_code and s.stage_code = c.stage_code"
          + " left join cat_insurer i on i.company_id = a.company_id"
          + " and i.party_code = a.insurer_code"
          + " left join lateral (select x.reason_code, x.remarks from plc_insurer_return x"
          + " where x.account_id = a.id order by x.id desc limit 1) r on true"
          + " where a.company_id = :company"
          + " and a.status in ('READY_FOR_PLACEMENT', 'PLACED', 'RETURNED_BY_INSURER',"
          + " 'PLACEMENT_CANCELLED', 'POLICY_ISSUED', 'BOOKED', 'CANCELLED')"
          + " and (cast(:arn as varchar) is null or a.arn = :arn)"
          + " and (cast(:insurer as varchar) is null or a.insurer_code = :insurer)"
          + " and (cast(:officer as varchar) is null"
          + " or lower(a.account_officer) = lower(cast(:officer as varchar)))"
          + " and (cast(:arn as varchar) is not null"
          + " or cast(c.stage_entered_at at time zone 'Asia/Manila' as date) between :from and :to)"
          + " order by 4, a.arn";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public PlacementUpdateReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Placement Update Report",
        "Placement status of accounts with the insurers, individual (ARN) or collective"
            + " (BRNB.011)",
        Permission.ACCOUNT_VIEW,
        true,
        ParameterSpec.optional(ARN, "ARN (individual report)", ParameterType.TEXT),
        ParameterSpec.optional(INSURER, "Insurer Code", ParameterType.TEXT),
        ParameterSpec.optional(OFFICER, "Account Officer", ParameterType.TEXT));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args =
        NbReportSupport.args(p)
            .with(ARN, NbReportSupport.upper(p, ARN))
            .with(INSURER, NbReportSupport.upper(p, INSURER))
            .with(OFFICER, p.optionalText(OFFICER).map(String::strip).orElse(null));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(r -> NbReportSupport.relabel(r, "hold_cover"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(ARN, "ARN"),
            ReportColumn.text("client", "Client"),
            ReportColumn.text("product", "Product"),
            ReportColumn.text("stage", "Stage"),
            ReportColumn.date("since", "Since"),
            ReportColumn.text("slip", "Placement Slip"),
            ReportColumn.date("placed", "Placed On"),
            ReportColumn.text("hold_cover", "Hold Cover"),
            ReportColumn.text("hold_ref", "Hold Cover Ref."),
            ReportColumn.date("issued", "Policy Issued"),
            ReportColumn.text("returned", "Last Return"),
            ReportColumn.text("remarks", "Remarks"),
            ReportColumn.text(OFFICER, "Officer"))
        .groupBy(INSURER, "Insurer")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }
}
