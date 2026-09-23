package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ChequeRow;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * FIN-AR-CHQ-RCPT (Src FR2390) Payment Received for Invoices through a Cheque Number: one numbered
 * entry per cheque (receipt) with one line per invoice it settled. PDC receipts are excluded.
 */
@Component
public class ChequePaymentsReceivedReport implements ReportDefinition {

  private static final String CODE = "FIN-AR-CHQ-RCPT";
  private static final String BANK = "bankAccount";
  private static final String PARTY = "partyCode";
  private static final String AMOUNT = "chequeAmount";
  private static final String ADJUSTED = "adjusted";
  private static final String SEP = " / ";

  private final ReceivablesQueries queries;

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   */
  public ChequePaymentsReceivedReport(ReceivablesQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "Payment Received for Invoices through a Cheque Number",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Cheques received from debtors and the invoices they settled",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.fromParam(),
            GlReportSupport.toParam(),
            ParameterSpec.optional(BANK, "Bank Account", ParameterType.ACCOUNT),
            ParameterSpec.optional(PARTY, "Customer Code", ParameterType.TEXT)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String bank = p.optionalText(BANK).orElse(null);
    String party = p.optionalText(PARTY).orElse(null);
    List<ReportRow> rows = new ArrayList<>();
    Long current = null;
    int serial = 0;
    BigDecimal cheques = BigDecimal.ZERO;
    BigDecimal adjusted = BigDecimal.ZERO;
    for (ChequeRow c :
        queries.chequeAllocations(
            p.longValue(GlReportSupport.COMPANY),
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO))) {
      if (excluded(bank, c.bankAccountCode()) || excluded(party, c.partyCode())) {
        continue;
      }
      boolean first = !Objects.equals(current, c.receiptId());
      if (first) {
        serial++;
        current = c.receiptId();
        cheques = cheques.add(c.amount());
      }
      adjusted = adjusted.add(c.adjusted() == null ? BigDecimal.ZERO : c.adjusted());
      rows.add(
          new ReportRow(RowKind.DETAIL, 0, first ? String.valueOf(serial) : "", cells(c, first)));
    }
    rows.add(
        new ReportRow(
            RowKind.TOTAL, 0, "Grand Total", Map.of(AMOUNT, cheques, ADJUSTED, adjusted)));
    List<ReportColumn> columns =
        List.of(
            ReportColumn.text("customer", "Customer Code / Name"),
            ReportColumn.text("cheque", "Cheque No / Date"),
            ReportColumn.amount(AMOUNT, "Cheque Amount"),
            ReportColumn.text("draweeBank", "Cust. Bank Name"),
            ReportColumn.text("bank", "Bank Name / Account No"),
            ReportColumn.text("receipt", "Document No / Date"),
            ReportColumn.text("invoice", "Invoice No / Date"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("invoiceAmount", "Invoice Amount"),
            ReportColumn.amount(ADJUSTED, "Adj. Amount"));
    return new ReportResult(
        CODE,
        metadata().title(),
        p.echo(),
        columns,
        rows,
        List.of("Adj. Amount = amount of the cheque knocked off against the invoice"));
  }

  private static Map<String, Object> cells(ChequeRow c, boolean first) {
    Map<String, Object> cells = new LinkedHashMap<>();
    if (first) {
      cells.put("customer", c.partyCode() + SEP + c.payerName());
      cells.put("cheque", c.chequeNo() + SEP + c.chequeDate());
      cells.put(AMOUNT, c.amount());
      cells.put("draweeBank", c.draweeBank());
      cells.put("bank", c.bankName() + SEP + c.bankAccountCode());
      cells.put("receipt", c.receiptNo() + SEP + c.receiptDate() + statusNote(c.status()));
      cells.put("currency", c.currency());
    }
    if (c.invoiceNo() != null) {
      cells.put("invoice", c.invoiceNo() + SEP + c.invoiceDate());
      cells.put("invoiceAmount", c.invoiceAmount());
      cells.put(ADJUSTED, c.adjusted());
    }
    return cells;
  }

  private static boolean excluded(String filter, String value) {
    return filter != null && !filter.equals(value);
  }

  private static String statusNote(String status) {
    return "APPROVED".equals(status) ? "" : " (" + status + ")";
  }
}
