package com.iortatechnxt.finverse.report.gl;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntry;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
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
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * General ledger detail: for each postable account in a code range, the opening balance, every
 * posted entry with running balance, and the closing balance.
 */
@Component
public class GeneralLedgerDetailReport implements ReportDefinition {

  private static final String DEBIT = "debit";
  private static final String CREDIT = "credit";
  private static final String BALANCE = "balance";
  private static final String FROM_ACCOUNT = "fromAccount";
  private static final String TO_ACCOUNT = "toAccount";

  private final LedgerQueryService ledger;
  private final GlReportSupport support;

  /**
   * Creates the report.
   *
   * @param ledger ledger read model
   * @param support GL helpers
   */
  public GeneralLedgerDetailReport(LedgerQueryService ledger, GlReportSupport support) {
    this.ledger = ledger;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-DETAIL",
        "General Ledger Detail",
        ReportCategory.GENERAL_LEDGER,
        "Account-wise transactions with opening, running and closing balances",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.fromParam(),
            GlReportSupport.toParam(),
            ParameterSpec.optional(FROM_ACCOUNT, "Account From", ParameterType.ACCOUNT),
            ParameterSpec.optional(TO_ACCOUNT, "Account To", ParameterType.ACCOUNT)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Long branchId = p.optionalLong(GlReportSupport.BRANCH).orElse(null);
    LocalDate from = p.date(GlReportSupport.FROM);
    LocalDate to = p.date(GlReportSupport.TO);
    String low = p.optionalText(FROM_ACCOUNT).orElse("");
    String high = p.optionalText(TO_ACCOUNT).orElse("￿");
    List<GlAccount> accounts =
        support.accountsById(companyId).values().stream()
            .filter(GlAccount::isPostable)
            .filter(a -> a.getCode().compareTo(low) >= 0 && a.getCode().compareTo(high) <= 0)
            .sorted(Comparator.comparing(GlAccount::getCode))
            .toList();
    List<ReportRow> rows = new ArrayList<>();
    for (GlAccount a : accounts) {
      var st = ledger.statement(companyId, a.getId(), branchId, from, to);
      if (st.entries().isEmpty() && st.openingBalance().signum() == 0) {
        continue;
      }
      appendAccount(rows, a, st.openingBalance(), st.entries());
    }
    List<ReportColumn> cols =
        List.of(
            ReportColumn.date("date", "Value Date"),
            ReportColumn.text("batchNo", "Batch No"),
            ReportColumn.text("narration", "Narration"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.amount(DEBIT, "Debit"),
            ReportColumn.amount(CREDIT, "Credit"),
            ReportColumn.amountNoTotal(BALANCE, "Balance (Dr+/Cr-)"));
    return new ReportResult("GL-DETAIL", metadata().title(), p.echo(), cols, rows, List.of());
  }

  private static void appendAccount(
      List<ReportRow> rows, GlAccount a, BigDecimal opening, List<LedgerEntry> entries) {
    rows.add(new ReportRow(RowKind.GROUP_HEADER, 0, a.getCode() + " - " + a.getName(), Map.of()));
    rows.add(new ReportRow(RowKind.DETAIL, 1, "Opening balance", Map.of(BALANCE, opening)));
    BigDecimal running = opening;
    BigDecimal dr = BigDecimal.ZERO;
    BigDecimal cr = BigDecimal.ZERO;
    for (LedgerEntry e : entries) {
      running = running.add(e.getDebitBase()).subtract(e.getCreditBase());
      dr = dr.add(e.getDebitBase());
      cr = cr.add(e.getCreditBase());
      Map<String, Object> cells = new LinkedHashMap<>();
      cells.put("date", e.getValueDate());
      cells.put("batchNo", e.getBatchNo());
      cells.put("narration", e.getNarration());
      cells.put("reference", e.getReference());
      cells.put(DEBIT, e.getDebitBase());
      cells.put(CREDIT, e.getCreditBase());
      cells.put(BALANCE, running);
      rows.add(ReportRow.detail(cells));
    }
    Map<String, Object> closing = new LinkedHashMap<>();
    closing.put(DEBIT, dr);
    closing.put(CREDIT, cr);
    closing.put(BALANCE, running);
    rows.add(new ReportRow(RowKind.SUBTOTAL, 0, "Closing balance " + a.getCode(), closing));
  }
}
