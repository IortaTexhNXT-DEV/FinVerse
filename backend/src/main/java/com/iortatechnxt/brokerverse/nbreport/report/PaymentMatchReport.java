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
 * Matched and Unmatched Payments (NB-PAY-MATCH, BRNB.067 / 068 / 075): every line of the payment
 * reports uploaded in the period (CLPC by PN or loan application number, other segments by ARN)
 * with its match result, the account found and whether the payment opened the payment gate.
 */
@Component
public class PaymentMatchReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-PAY-MATCH";

  private static final String MATCH = "match";

  private static final String SQL =
      "select initcap(replace(l.match_status, '_', ' ')) as match_result, r.report_no,"
          + " r.kind, r.status as report_status, l.row_no, l.reference, l.paid, l.amount,"
          + " l.paid_on, l.arn, l.manually_matched as manual, l.applied,"
          + " coalesce(l.apply_message, l.message) as message"
          + " from plc_payment_report r join plc_payment_report_line l on l.report_id = r.id"
          + " where r.company_id = :company"
          + " and cast(r.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:match as varchar) is null or l.match_status = :match)"
          + " order by l.match_status, r.report_no, l.row_no";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public PaymentMatchReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return NbReportSupport.metadata(
        CODE,
        "Matched and Unmatched Payments",
        "Payment report lines matched to accounts or left unmatched (BRNB.067/068/075)",
        Permission.BILLING_MANAGE,
        true,
        ParameterSpec.select(
            MATCH,
            "Match Result",
            List.of(NbReportSupport.ALL, "MATCHED", "UNMATCHED", "AMBIGUOUS", "UNPAID"),
            NbReportSupport.ALL));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args = NbReportSupport.args(p).with(MATCH, NbReportSupport.selected(p, MATCH));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(
                r ->
                    NbReportSupport.flag(
                        NbReportSupport.flag(NbReportSupport.flag(r, "paid"), "manual"), "applied"))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("report_no", "Payment Report"),
            ReportColumn.text("kind", "Kind"),
            ReportColumn.text("report_status", "Report Status"),
            new ReportColumn("row_no", "Row", ColumnType.NUMBER, false),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.text("paid", "Paid"),
            ReportColumn.amount("amount", "Amount"),
            ReportColumn.date("paid_on", "Paid On"),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("manual", "Manual Match"),
            ReportColumn.text("applied", "Applied"),
            ReportColumn.text("message", "Message"))
        .groupBy("match_result", "Match Result")
        .rows(rows)
        .presorted()
        .build();
  }
}
