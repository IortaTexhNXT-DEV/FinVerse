package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ChequeRow;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-AR-CHQ-UNDEP (Src FR2392) Cheques Received but Not Deposited as of a date (non-PDC cheques
 * with no deposit recorded on or before the date), with totals per currency.
 */
@Component
public class ChequesNotDepositedReport implements ReportDefinition {

  private final ReceivablesQueries queries;

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   */
  public ChequesNotDepositedReport(ReceivablesQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-AR-CHQ-UNDEP",
        "Cheques Received but Not Deposited",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Cheques received and not yet banked as of a date",
        List.of(GlReportSupport.companyParam(), GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        queries
            .undeposited(p.longValue(GlReportSupport.COMPANY), p.date(GlReportSupport.AS_OF))
            .stream()
            .map(ChequesNotDepositedReport::cells)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("chequeNo", "Cheque Number"),
            ReportColumn.date("chequeDate", "Cheque Date"),
            ReportColumn.text("receiptNo", "Document Number"),
            ReportColumn.date("receiptDate", "Document Date"),
            ReportColumn.text("bank", "Bank Code / Bank Name"),
            ReportColumn.text("customer", "Customer Code / Customer Name"),
            ReportColumn.amount("amountFc", "Cheque Amount FC"),
            ReportColumn.amount("amountLc", "Cheque Amount LC"))
        .groupBy("currency", "Currency")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> cells(ChequeRow c) {
    Map<String, Object> cells = new LinkedHashMap<>();
    cells.put("currency", c.currency());
    cells.put("chequeNo", c.chequeNo());
    cells.put("chequeDate", c.chequeDate());
    cells.put("receiptNo", c.receiptNo());
    cells.put("receiptDate", c.receiptDate());
    cells.put("bank", c.bankAccountCode() + " / " + c.bankName());
    cells.put("customer", c.partyCode() + " / " + c.payerName());
    cells.put("amountFc", c.amount());
    cells.put("amountLc", c.baseAmount());
    return cells;
  }
}
