package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
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
 * Insurance Advice and E-policy Dispatch Report (NB-DISPATCH, BRNB.078 / 035): the e-policies and
 * Insurance Advices e-mailed in the period from the messaging send log, successful and unsuccessful
 * with the reason, recipients, attempts and whether the send was simulated (no mail server
 * configured). The password e-mails that follow each protected send are not listed.
 */
@Component
public class DispatchReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-DISPATCH";

  private static final String DOCUMENT = "document";
  private static final String OUTCOME = "outcome";

  private static final String SQL =
      "select case o.status when 'SENT' then 'Sent' when 'FAILED' then 'Failed'"
          + " when 'QUEUED' then 'Queued' else 'Cancelled' end as outcome,"
          + " case o.purpose when 'EPOLICY' then 'E-policy' else 'Insurance Advice' end"
          + " as document, o.reference, o.created_at as queued, o.sent_at as sent,"
          + " o.recipients, o.subject, o.attempts, o.last_error as reason, o.simulated,"
          + " o.created_by as sender"
          + " from msg_outbound o"
          + " where o.company_id = :company and o.purpose in ('EPOLICY', 'INSURANCE_ADVICE')"
          + " and cast(o.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:document as varchar) is null or o.purpose = :document)"
          + " and (cast(:outcome as varchar) is null or o.status = :outcome)"
          + " order by o.status desc, o.created_at, o.id";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public DispatchReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Insurance Advice and E-policy Dispatch",
        "E-policies and Insurance Advices sent or failed with the reason, by date range"
            + " (BRNB.078)",
        Permission.ACCOUNT_VIEW,
        true,
        ParameterSpec.select(
            DOCUMENT,
            "Document",
            List.of(NbReportSupport.ALL, "EPOLICY", "INSURANCE_ADVICE"),
            NbReportSupport.ALL),
        ParameterSpec.select(
            OUTCOME,
            "Outcome",
            List.of(NbReportSupport.ALL, "SENT", "FAILED", "QUEUED"),
            NbReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args =
        NbReportSupport.args(p)
            .with(DOCUMENT, NbReportSupport.selected(p, DOCUMENT))
            .with(OUTCOME, NbReportSupport.selected(p, OUTCOME));
    var rows =
        jdbc.rows(SQL, args.map()).stream().map(r -> NbReportSupport.flag(r, "simulated")).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(DOCUMENT, "Document"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.date("queued", "Queued On"),
            ReportColumn.date("sent", "Sent On"),
            ReportColumn.text("recipients", "Recipients"),
            ReportColumn.text("subject", "Subject"),
            new ReportColumn("attempts", "Attempts", ColumnType.NUMBER, false),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("simulated", "Simulated"),
            ReportColumn.text("sender", "Sent By"))
        .groupBy(OUTCOME, "Outcome")
        .rows(rows)
        .presorted()
        .build();
  }
}
