package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FinReportQueries;
import com.iortatechnxt.finverse.finreport.service.StatusFilter;
import com.iortatechnxt.finverse.finreport.service.VoucherLine;
import com.iortatechnxt.finverse.finreport.service.VoucherQuery;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * FIN-GL-ALLOCJV – Allocation JV Details (PREMIA FR2189).
 *
 * <p>Definition used in FinVerse: an <b>allocation JV</b> is a posted ADJUSTMENT or ACCRUAL journal
 * that credits a prepaid (asset) account and debits expense accounts – the periodic amortisation of
 * a prepayment (Dr expense, Cr prepaid). Each expense debit is one allocation line; the allocation
 * period is the calendar month of the JV date (allocated from / to) and the allocated date is the
 * JV date. The optional Prepaid A/c range restricts the credited asset accounts; when a JV credits
 * several prepaid accounts, its expense lines are listed under the first one.
 */
@Component
public class AllocationJournalReport implements ReportDefinition {

  private static final String PREPAID_FROM = "prepaidFrom";
  private static final String PREPAID_TO = "prepaidTo";
  private static final String PREPAID = "prepaid";
  private static final String JV = "jv";
  private static final String MAIN_NAME = "mainName";
  private static final String ALLOC_FROM = "allocatedFrom";
  private static final String ALLOC_TO = "allocatedTo";
  private static final String ALLOC_DATE = "allocatedDate";
  private static final String VALUE = "lcValue";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public AllocationJournalReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-GL-ALLOCJV",
        "Allocation JV Details",
        ReportCategory.GENERAL_LEDGER,
        "Expense allocation (prepaid amortisation) journals by prepaid account (FR2189)",
        List.of(
            FinParams.company(),
            ParameterSpec.optional(PREPAID_FROM, "Prepaid Main A/c From", ParameterType.ACCOUNT),
            ParameterSpec.optional(PREPAID_TO, "Prepaid Main A/c To", ParameterType.ACCOUNT),
            FinParams.from(),
            FinParams.to()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    VoucherQuery q =
        new VoucherQuery(
            companyId,
            null,
            p.date(FinParams.FROM),
            p.date(FinParams.TO),
            StatusFilter.POSTED,
            List.of(JournalType.ADJUSTMENT.name(), JournalType.ACCRUAL.name()),
            null,
            null,
            null,
            h.postableIds(a -> true, a -> true));
    Predicate<AccountNode> prepaidRange = FinReportSupport.range(p, PREPAID_FROM, PREPAID_TO);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (List<VoucherLine> jv :
        Vouchers.group(queries.voucherLines(q), VoucherLine::batchId).values()) {
      Optional<AccountNode> prepaid =
          jv.stream()
              .filter(l -> !l.debit())
              .map(l -> h.node(l.accountId()))
              .filter(a -> a.accountClass() == AccountClass.ASSET)
              .filter(a -> prepaidRange.test(h.mainOf(a.id())))
              .findFirst();
      prepaid.ifPresent(
          account ->
              jv.stream()
                  .filter(
                      l ->
                          l.debit() && h.node(l.accountId()).accountClass() == AccountClass.EXPENSE)
                  .forEach(l -> rows.add(cells(account, l, h, branches))));
    }
    rows.sort(
        Comparator.comparing((Map<String, Object> r) -> (String) r.get(PREPAID))
            .thenComparing(r -> (String) r.get(JV)));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(JV, "Allocated JV"),
            ReportColumn.text(Vouchers.MAIN_AC, "Expense Main A/c"),
            ReportColumn.text(MAIN_NAME, "Expense Main A/c Name"),
            ReportColumn.text(Vouchers.SUB_AC, "Expense Sub A/c"),
            ReportColumn.text(Vouchers.DIVISION, "Expense Division"),
            ReportColumn.text(Vouchers.DEPARTMENT, "Expense Department"),
            ReportColumn.date(ALLOC_FROM, "Allocated From Date"),
            ReportColumn.date(ALLOC_TO, "Allocated To Date"),
            ReportColumn.date(ALLOC_DATE, "Allocated Date"),
            ReportColumn.amount(VALUE, "LC Value"))
        .groupBy(PREPAID, "Prepaid A/c")
        .presorted()
        .rows(rows)
        .note(
            "Allocation JV = posted adjustment / accrual journal crediting a prepaid asset account"
                + " and debiting expense accounts; the allocation period is the JV month.")
        .build();
  }

  private static Map<String, Object> cells(
      AccountNode prepaid, VoucherLine l, AccountHierarchy h, Map<Long, String> branches) {
    Map<String, Object> cells = Vouchers.accountCells(h, l.accountId());
    YearMonth month = YearMonth.from(l.valueDate());
    cells.putAll(
        FinRows.cells(
            PREPAID,
            prepaid.caption(),
            JV,
            l.batchNo(),
            MAIN_NAME,
            h.mainOf(l.accountId()).name(),
            Vouchers.DIVISION,
            branches.get(l.branchId()),
            Vouchers.DEPARTMENT,
            l.costCenter(),
            ALLOC_FROM,
            month.atDay(1),
            ALLOC_TO,
            month.atEndOfMonth(),
            ALLOC_DATE,
            l.valueDate(),
            VALUE,
            l.baseAmount()));
    return cells;
  }
}
