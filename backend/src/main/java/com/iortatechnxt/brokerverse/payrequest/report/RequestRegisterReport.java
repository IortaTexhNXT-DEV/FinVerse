package com.iortatechnxt.brokerverse.payrequest.report;

import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
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
 * Request register (PRQ-REGISTER, MKT 1.10.0, 2.23.0): one row per refund account (AR number,
 * invoice, reason) and per cash advance or check cancellation, with who prepared, reviewed and
 * approved it, grouped by kind.
 */
@Component
public class RequestRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PRQ-REGISTER";

  private final PayRequestReports support;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   */
  public RequestRegisterReport(PayRequestReports support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return PayRequestReports.metadata(
        CODE,
        "Request Register",
        "Refund accounts (AR, invoice, reason), cash advances and check cancellations of a period"
            + " with their preparer, reviewer and approvers (MKT 1.10.0)");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (PaymentRequest r : support.requests(p)) {
      if (r.getLines().isEmpty()) {
        rows.add(row(r, null));
      } else {
        r.getLines().forEach(l -> rows.add(row(r, l)));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.date("date", "Request Date"),
            ReportColumn.text("payee", "Payee"),
            ReportColumn.text("arNo", "AR No."),
            ReportColumn.text("invoiceNo", "Invoice No."),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amount("amount", "Amount"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.text("preparedBy", "Prepared By"),
            ReportColumn.text("reviewedBy", "Reviewed By"),
            ReportColumn.text("approvedBy", "Approved By"))
        .groupBy(PayRequestReports.KIND, "Kind")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> row(PaymentRequest r, RefundLine line) {
    Map<String, Object> m = PayRequestReports.row(r);
    RequestTrail t = r.trailOrNone();
    if (line != null) {
      m.put("arNo", line.getArNo());
      m.put("invoiceNo", line.getInvoiceNo());
      m.put("reason", line.getReasonCode());
      m.put("amount", line.getAmount());
    }
    m.put("preparedBy", r.getCreatedBy());
    m.put("reviewedBy", t.reviewedBy());
    m.put(
        "approvedBy",
        t.hrApprovedBy() == null ? t.approvedBy() : t.approvedBy() + " / HR " + t.hrApprovedBy());
    return m;
  }
}
