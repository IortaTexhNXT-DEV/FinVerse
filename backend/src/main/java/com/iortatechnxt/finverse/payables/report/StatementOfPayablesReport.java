package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-AP-SOP – Statement of Payables (Base Currency) (Src FAP007A "Statement of Accounts"): one
 * statement per supplier of the open documents in one currency, with post-dated cheques issued and
 * not yet presented, ageing of outstandings and the balance net of PDC.
 *
 * <p>An item paid by a PDC that is not yet presented is still owed until the cheque is presented:
 * its Balance includes the PDC amount, the PDC columns show the cheque, and Balance Net of PDC is
 * the sub-ledger balance. The group subtotal of each supplier gives Net Balance, PDC Cheques,
 * ageing and Balance Net of PDC.
 */
@Component
public class StatementOfPayablesReport implements ReportDefinition {

  private static final String STATEMENT_CURRENCY = "statementCurrency";
  private static final String SUPPLIER = "supplier";
  private static final String BALANCE = "balance";
  private static final String PDC_AMOUNT = "pdcAmount";

  /**
   * Default slots of this statement's fixed layout (0-30, 31-60, 61-90, 91-180, 181-365, over 365),
   * which the Reports Book prescribes independently of the company-wide ageing default.
   */
  private static final AgeingSlots STATEMENT_SLOTS = AgeingSlots.of(List.of(30, 60, 90, 180, 365));

  private final CreditorReportSupport support;
  private final CreditorLedger ledger;

  /**
   * Creates the report.
   *
   * @param support creditor engine
   * @param ledger creditor read model
   */
  public StatementOfPayablesReport(CreditorReportSupport support, CreditorLedger ledger) {
    this.support = support;
    this.ledger = ledger;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>(CreditorReportSupport.parameters(false));
    params.add(
        ParameterSpec.optional(
            STATEMENT_CURRENCY, "Statement Currency (blank = base)", ParameterType.CURRENCY));
    return new ReportMetadata(
        "FIN-AP-SOP",
        "Statement of Payables (Base Currency)",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Supplier statement of open documents with PDCs issued and ageing (FAP007A)",
        params,
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AgeingSlots slots = CreditorReportSupport.slots(p, STATEMENT_SLOTS);
    String currency = p.optionalText(STATEMENT_CURRENCY).orElse(support.baseCurrency(p));
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    boolean byDue = CreditorReportSupport.byDueDate(p);
    Map<Long, CreditorLedger.PendingPdc> pdcs =
        ledger.pendingPdcs(p.longValue(GlReportSupport.COMPANY), asOf);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (CreditorItem item : support.items(p, pdcs.keySet())) {
      if (!currency.equals(item.currency())) {
        continue;
      }
      rows.add(row(item, pdcs.get(item.id()), slots, asOf, byDue));
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.date("documentDate", "Document Date"));
    columns.add(ReportColumn.date("dueDate", "Due Date"));
    columns.add(ReportColumn.text("reference", "Document Reference"));
    columns.add(ReportColumn.text("documentNo", "Document Number"));
    columns.add(ReportColumn.amount("debit", "Debit"));
    columns.add(ReportColumn.amount("credit", "Credit"));
    columns.add(ReportColumn.amount(BALANCE, "Balance Amount"));
    columns.add(ReportColumn.date("pdcDate", "PDC Date"));
    columns.add(ReportColumn.text("pdcNo", "PDC Number"));
    columns.add(ReportColumn.amount(PDC_AMOUNT, "PDC Amount"));
    columns.addAll(CreditorReportSupport.bucketColumns(slots));
    columns.add(ReportColumn.amount(CreditorReportSupport.ON_ACCOUNT, "On Account"));
    columns.add(ReportColumn.amount("netOfPdc", "Balance Net of PDC"));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy(SUPPLIER, "Statement of Accounts")
        .presorted()
        .rows(rows)
        .note("Statement currency " + currency + " as of " + asOf + ".")
        .note(CreditorReportSupport.note(p, slots))
        .note(
            "Subtotal per supplier: Net Balance, PDC Cheques, ageing of outstandings and Balance Net of PDC.")
        .build();
  }

  private static Map<String, Object> row(
      CreditorItem item,
      CreditorLedger.PendingPdc pdc,
      AgeingSlots slots,
      LocalDate asOf,
      boolean byDue) {
    BigDecimal pdcAmount = pdc == null ? BigDecimal.ZERO : pdc.amount();
    BigDecimal netOfPdc = item.signed(false);
    BigDecimal gross = netOfPdc.add(item.credit() ? pdcAmount : BigDecimal.ZERO);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(
        SUPPLIER,
        item.partyCode()
            + " "
            + item.partyName()
            + (item.address() == null ? "" : ", " + item.address())
            + " - Internal Ref "
            + item.mainAccount()
            + "/"
            + item.partyCode());
    row.put("documentDate", item.documentDate());
    row.put("dueDate", item.dueDate());
    row.put("reference", item.narration());
    row.put("documentNo", item.documentNo());
    row.put(item.credit() ? "credit" : "debit", item.amount());
    row.put(BALANCE, gross);
    if (pdc != null) {
      row.put("pdcDate", pdc.chequeDate());
      row.put("pdcNo", pdc.chequeNo());
      row.put(PDC_AMOUNT, pdcAmount);
    }
    String bucket =
        item.credit()
            ? CreditorReportSupport.bucketKey(slots.index(item.age(asOf, byDue)))
            : CreditorReportSupport.ON_ACCOUNT;
    row.put(bucket, gross);
    row.put("netOfPdc", netOfPdc);
    return row;
  }
}
