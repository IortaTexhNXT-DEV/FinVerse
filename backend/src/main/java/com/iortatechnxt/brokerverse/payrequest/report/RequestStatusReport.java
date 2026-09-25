package com.iortatechnxt.brokerverse.payrequest.report;

import com.iortatechnxt.brokerverse.payrequest.domain.DisbursementTrack;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Request status report (PRQ-STATUS, MKT 1.18.0-1.18.1): every request of the period with its
 * current status, where it is in Disbursement (gateway status, DV, instrument) and when it was
 * paid; .xlsx / .ods export and print through the report framework.
 */
@Component
public class RequestStatusReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PRQ-STATUS";

  private final PayRequestReports support;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   */
  public RequestStatusReport(PayRequestReports support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return PayRequestReports.metadata(
        CODE,
        "Request Status Report",
        "Refund, cash-advance and check-cancellation requests of a period with their status and"
            + " Disbursement tracking (MKT 1.18.1)");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        support.requests(p).stream().map(RequestStatusReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.date("date", "Request Date"),
            ReportColumn.text(PayRequestReports.KIND, "Kind"),
            ReportColumn.text("payee", "Payee"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amount("amount", "Amount"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.text("gateway", "Disbursement Status"),
            ReportColumn.text("dvNo", "DV No."),
            ReportColumn.text("instrument", "Instrument Status"),
            ReportColumn.text("disbursedAt", "Disbursed"))
        .rows(rows)
        .presorted()
        .build();
  }

  private static Map<String, Object> row(PaymentRequest r) {
    Map<String, Object> m = PayRequestReports.row(r);
    DisbursementTrack t = r.trackOrNone();
    m.put("gateway", t.status());
    m.put("dvNo", t.dvNo());
    m.put("instrument", t.instrumentStatus());
    m.put("disbursedAt", t.disbursedAt() == null ? null : t.disbursedAt().toString());
    return m;
  }
}
