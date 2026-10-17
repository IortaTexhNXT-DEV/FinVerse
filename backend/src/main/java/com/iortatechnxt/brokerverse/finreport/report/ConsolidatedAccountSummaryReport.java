package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries.DailyAmount;
import com.iortatechnxt.brokerverse.finreport.service.StatusFilter;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-GL-CONSSUM – Consolidated Account-wise Summary (PREMIA FGL009): debits, credits and net per
 * main / sub account, optionally per day, for posted and / or unposted vouchers, counting only
 * lines above an amount limit. Aggregated in SQL.
 */
@Component
public class ConsolidatedAccountSummaryReport implements ReportDefinition {

  private static final String DAILY = "dailySummary";
  private static final String AMOUNT_OVER = "amountOver";
  private static final String MAIN = "mainAccount";
  private static final String DATE = "date";
  private static final String NET = "net";
  private static final String SIDE = "side";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public ConsolidatedAccountSummaryReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.addAll(FinParams.txnRange());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.mainRange());
    params.addAll(FinParams.subRange());
    params.add(
        ParameterSpec.optional(AMOUNT_OVER, "Amount Over Limit", ParameterType.NUMBER)
            .withDefault("0"));
    params.add(FinParams.flag(DAILY, "Daily Summary"));
    params.add(FinParams.status(StatusFilter.POSTED));
    return new ReportMetadata(
        "FIN-GL-CONSSUM",
        "Consolidated Account-wise Summary",
        ReportCategory.GENERAL_LEDGER,
        "Main and sub account summary by day and transaction status (FGL009)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    boolean daily = p.flag(DAILY);
    BigDecimal limit = p.optionalDecimal(AMOUNT_OVER).orElse(BigDecimal.ZERO);
    List<DailyAmount> amounts =
        queries.dailyAmounts(Vouchers.query(p, h, FinParams.status(p)), limit);
    Map<List<Object>, BigDecimal[]> sums = new LinkedHashMap<>();
    for (DailyAmount a : amounts) {
      List<Object> key = List.of(a.accountId(), daily ? a.date() : LocalDate.MIN);
      BigDecimal[] s =
          sums.computeIfAbsent(key, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      s[0] = s[0].add(a.debit());
      s[1] = s[1].add(a.credit());
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    sums.forEach(
        (key, s) -> {
          Map<String, Object> cells = Vouchers.accountCells(h, (Long) key.get(0));
          AccountNode main = h.mainOf((Long) key.get(0));
          BigDecimal net = s[0].subtract(s[1]);
          cells.putAll(
              FinRows.cells(
                  MAIN,
                  main.caption(),
                  DATE,
                  daily ? key.get(1) : null,
                  Vouchers.DEBIT,
                  s[0],
                  Vouchers.CREDIT,
                  s[1],
                  NET,
                  net.abs(),
                  SIDE,
                  FinRows.drCr(net)));
          rows.add(cells);
        });
    rows.sort(
        Comparator.comparing((Map<String, Object> r) -> (String) r.get(Vouchers.MAIN_AC))
            .thenComparing(r -> (String) r.get(Vouchers.SUB_AC))
            .thenComparing(r -> String.valueOf(r.get(DATE))));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(Vouchers.MAIN_AC, "Main A/c"),
            ReportColumn.text(Vouchers.SUB_AC, "Sub A/c"),
            ReportColumn.text(Vouchers.ACCOUNT_NAME, "Account Description"),
            ReportColumn.date(DATE, "Date"),
            ReportColumn.amount(Vouchers.DEBIT, "Debit Amount"),
            ReportColumn.amount(Vouchers.CREDIT, "Credit Amount"),
            ReportColumn.amountNoTotal(NET, "Net Amount"),
            ReportColumn.text(SIDE, "Dr/Cr"))
        .groupBy(MAIN, "Main Account")
        .presorted()
        .rows(rows)
        .note("Only lines with an amount above " + limit + " are included.")
        .build();
  }
}
