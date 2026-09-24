package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService.GlLine;
import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Validation List (ADJ-VALIDATION-LIST, ADJID.017): the posted endorsement transactions of the
 * period with their GL entries, one row per journal line, with the fields of the BRD list (type,
 * invoice, endorsement reference, request number, segment, AO, policy, assured, insurer, GL code,
 * debit / credit, reason, validation date, validation batch).
 */
@Component
public class ValidationListReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ADJ-VALIDATION-LIST";

  private final AdjustmentReportSupport support;
  private final AdjustmentQueryService queries;
  private final LovService lovs;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   * @param queries GL lines of a request
   * @param lovs labels (reasons)
   */
  public ValidationListReport(
      AdjustmentReportSupport support, AdjustmentQueryService queries, LovService lovs) {
    this.support = support;
    this.queries = queries;
    this.lovs = lovs;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.operations(
        CODE,
        "Validation List",
        "Posted endorsement transactions with their GL entries and remarks (ADJID.017)",
        AdjustmentReportSupport.period("MONTH_START"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (EndorsementRequest r : support.posted(p)) {
      for (GlLine line : queries.journalLines(r)) {
        rows.add(row(r, line));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("kind", "Type of Cancellation / Adjustment"),
            ReportColumn.text("invoice", "Invoice No."),
            ReportColumn.text("reference", "Endorsement Ref. No."),
            ReportColumn.text("requestNo", "Endorsement Request No."),
            ReportColumn.text("segment", "Market Segment"),
            ReportColumn.text("ao", "Requesting AO"),
            ReportColumn.text("policy", "Policy No."),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("gl", "GL Code"),
            ReportColumn.amount("debit", "Debit"),
            ReportColumn.amount("credit", "Credit"),
            ReportColumn.text("reason", "Reason / Remarks"),
            ReportColumn.date("validated", "Validation Date"),
            ReportColumn.text("batch", "Validation Batch No."))
        .rows(rows)
        .presorted()
        .build();
  }

  private Map<String, Object> row(EndorsementRequest r, GlLine line) {
    boolean debit = line.side() == BalanceSide.DEBIT;
    String reason = lovs.label("CANCELLATION_REASON", r.getTerms().reasonCode());
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("kind", r.getRequestClass() + " / " + r.getTerms().endorsementType());
    m.put("invoice", r.getSubject().invoiceNo());
    m.put("reference", DocText.text(r.getTerms().endorsementRef()));
    m.put("requestNo", r.getRequestNo());
    m.put("segment", DocText.text(r.getSubject().segment()));
    m.put("ao", DocText.text(r.getSubject().aoUsername()));
    m.put("policy", DocText.text(r.getSubject().policyNo()));
    m.put("assured", r.getSubject().assuredName());
    m.put("insurer", r.getSubject().insurerCode());
    m.put("gl", line.accountCode() + " " + line.accountName());
    m.put("debit", debit ? line.amount() : BigDecimal.ZERO);
    m.put("credit", debit ? BigDecimal.ZERO : line.amount());
    m.put("reason", (reason == null ? "" : reason + " - ") + r.getTerms().description());
    m.put("validated", AdjustmentReportSupport.day(r.trail().validatedAt()));
    m.put("batch", DocText.text(r.outcome().batchNo()));
    return m;
  }
}
