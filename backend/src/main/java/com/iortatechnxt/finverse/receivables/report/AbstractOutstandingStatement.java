package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.finverse.receivables.report.ReceivablesReportSupport.AgedItem;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import com.iortatechnxt.finverse.receivables.service.PdcQueries.PdcAsOf;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ArItem;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statement of Outstanding engine (Src FAP007A / FAP007): per customer, the open items with
 * original and balance amounts and linked post-dated cheques, then Net Balance, the ageing of
 * outstandings, On Account, PDC Cheques and Balance Net of PDC.
 */
public abstract class AbstractOutstandingStatement implements ReportDefinition {

  private static final String BALANCE = "balance";
  private static final String DEBIT = "debit";
  private static final String CREDIT = "credit";
  private static final String PDC_AMOUNT = "pdcAmount";
  private static final String CURRENCY = "currency";

  private final ReceivablesQueries queries;
  private final PdcQueries pdcs;
  private final String code;
  private final String title;
  private final boolean foreignCurrency;

  /**
   * Creates the report.
   *
   * @param queries receivables read model
   * @param pdcs PDC register
   * @param code report code
   * @param title title
   * @param foreignCurrency one foreign currency, amounts not converted
   */
  protected AbstractOutstandingStatement(
      ReceivablesQueries queries,
      PdcQueries pdcs,
      String code,
      String title,
      boolean foreignCurrency) {
    this.queries = queries;
    this.pdcs = pdcs;
    this.code = code;
    this.title = title;
    this.foreignCurrency = foreignCurrency;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    params.add(GlReportSupport.asOfParam());
    if (foreignCurrency) {
      params.add(
          ParameterSpec.required(CURRENCY, "Currency Code", ParameterType.CURRENCY)
              .withDefault("USD"));
    }
    params.addAll(ReceivablesReportSupport.partyParams());
    params.add(ReceivablesReportSupport.ageingParams().get(0));
    params.add(ReceivablesReportSupport.ageingParams().get(2));
    return new ReportMetadata(
        code,
        title,
        ReportCategory.RECEIVABLES_PAYABLES,
        "Customer statement of open items with ageing and post-dated cheques",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    String currency = foreignCurrency ? p.text(CURRENCY) : null;
    List<ArItem> items =
        ReceivablesReportSupport.selectedItems(queries, p).stream()
            .filter(i -> currency == null || i.currency().equals(currency))
            .toList();
    Map<String, List<AgedItem>> byParty =
        ReceivablesReportSupport.age(items, asOf, p, foreignCurrency).stream()
            .collect(
                Collectors.groupingBy(
                    a -> ReceivablesReportSupport.partyLabel(a.item()),
                    LinkedHashMap::new,
                    Collectors.toList()));
    Map<String, List<PostDatedCheque>> heldByParty =
        pdcs.heldAsOf(p.longValue(GlReportSupport.COMPANY), asOf).stream()
            .map(PdcAsOf::pdc)
            .filter(c -> currency == null || c.getCurrency().equals(currency))
            .collect(Collectors.groupingBy(PostDatedCheque::getPartyCode));
    AgeingSlots slots = ReceivablesReportSupport.slots(p);
    List<ReportRow> rows = new ArrayList<>();
    BigDecimal grand = BigDecimal.ZERO;
    for (Map.Entry<String, List<AgedItem>> e : byParty.entrySet()) {
      String partyCode = e.getValue().get(0).item().partyCode();
      grand =
          grand.add(appendParty(rows, e.getKey(), e.getValue(), heldByParty.get(partyCode), slots));
    }
    rows.add(new ReportRow(RowKind.TOTAL, 0, "Group Net Balance", Map.of(BALANCE, grand)));
    return new ReportResult(code, title, p.echo(), columns(), rows, notes(currency));
  }

  private BigDecimal appendParty(
      List<ReportRow> rows,
      String party,
      List<AgedItem> aged,
      List<PostDatedCheque> held,
      AgeingSlots slots) {
    List<PostDatedCheque> cheques = held == null ? List.of() : held;
    Map<Long, PostDatedCheque> linked = new LinkedHashMap<>();
    cheques.stream()
        .filter(c -> c.getDebitItemId() != null)
        .forEach(c -> linked.putIfAbsent(c.getDebitItemId(), c));
    rows.add(new ReportRow(RowKind.GROUP_HEADER, 0, party, Map.of()));
    BigDecimal[] buckets = new BigDecimal[slots.size()];
    Arrays.fill(buckets, BigDecimal.ZERO);
    BigDecimal onAccount = BigDecimal.ZERO;
    BigDecimal net = BigDecimal.ZERO;
    for (AgedItem a : aged) {
      rows.add(ReportRow.detail(itemCells(a, linked.get(a.item().id()))));
      net = net.add(a.amount());
      if (a.onAccount()) {
        onAccount = onAccount.add(a.amount());
      } else {
        buckets[a.bucket()] = buckets[a.bucket()].add(a.amount());
      }
    }
    BigDecimal pdcTotal =
        cheques.stream().map(this::pdcValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    rows.add(new ReportRow(RowKind.SUBTOTAL, 0, "Net Balance", Map.of(BALANCE, net)));
    List<String> labels = slots.labels();
    for (int i = 0; i < buckets.length; i++) {
      rows.add(line("Ageing " + labels.get(i), BALANCE, buckets[i]));
    }
    rows.add(line("On Account", BALANCE, onAccount));
    rows.add(line("PDC Cheques", PDC_AMOUNT, pdcTotal));
    rows.add(line("Balance Net of PDC", BALANCE, net.subtract(pdcTotal)));
    return net;
  }

  private Map<String, Object> itemCells(AgedItem a, PostDatedCheque pdc) {
    ArItem i = a.item();
    BigDecimal original = i.signedOriginal(foreignCurrency);
    Map<String, Object> cells = new LinkedHashMap<>();
    cells.put("documentDate", i.documentDate());
    cells.put("dueDate", i.dueDate());
    cells.put("reference", i.narration());
    cells.put("documentNo", i.documentType() + "-" + i.documentNo());
    cells.put(original.signum() > 0 ? DEBIT : CREDIT, original.abs());
    cells.put("original", original);
    cells.put(BALANCE, a.amount());
    if (pdc != null) {
      cells.put("pdcDate", pdc.getChequeDate());
      cells.put("pdcNo", pdc.getChequeNo());
      cells.put(PDC_AMOUNT, pdcValue(pdc));
    }
    return cells;
  }

  private BigDecimal pdcValue(PostDatedCheque c) {
    return foreignCurrency ? c.getAmount() : c.getBaseAmount();
  }

  private static ReportRow line(String label, String key, BigDecimal value) {
    return new ReportRow(RowKind.DETAIL, 1, label, Map.of(key, value));
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.date("documentDate", "Document Date"),
        ReportColumn.date("dueDate", "Due Date"),
        ReportColumn.text("reference", "Document Reference"),
        ReportColumn.text("documentNo", "Document Number"),
        ReportColumn.amount(DEBIT, "Debit"),
        ReportColumn.amount(CREDIT, "Credit"),
        ReportColumn.amountNoTotal("original", "Original Amount Dr/Cr"),
        ReportColumn.amountNoTotal(BALANCE, "Balance Amount"),
        ReportColumn.date("pdcDate", "PDC Date"),
        ReportColumn.text("pdcNo", "PDC Number"),
        ReportColumn.amountNoTotal(PDC_AMOUNT, "PDC Amount"));
  }

  private static List<String> notes(String currency) {
    return List.of(
        currency == null
            ? "Amounts in base currency at the historical rate of each document (R-FX)"
            : "Amounts in " + currency + ", not converted",
        "PDC Cheques = post-dated cheques received and still on hand as of the date");
  }
}
