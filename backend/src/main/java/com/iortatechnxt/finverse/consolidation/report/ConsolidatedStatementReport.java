package com.iortatechnxt.finverse.consolidation.report;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.finverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base of the consolidated balance sheet and income statement: consolidated balances of selected
 * account classes in natural sign, one section per class.
 */
abstract class ConsolidatedStatementReport {

  private static final String SECTION = "section";

  private final ConsolidationReportSupport support;

  ConsolidatedStatementReport(ConsolidationReportSupport support) {
    this.support = support;
  }

  /**
   * Classes shown, in order.
   *
   * @return classes
   */
  abstract List<AccountClass> classes();

  /**
   * Extra rows and notes (e.g. net result in equity).
   *
   * @param balances all consolidated balances
   * @param rows rows built so far (may be extended)
   * @param notes notes (may be extended)
   */
  abstract void complete(
      Collection<ConsolidatedBalance> balances, List<Map<String, Object>> rows, List<String> notes);

  /**
   * Builds the statement.
   *
   * @param p parameters
   * @return result
   */
  ReportResult build(ReportParameters p) {
    ConsolidationRun run = support.run(p);
    Collection<ConsolidatedBalance> balances = ConsolidationReportSupport.balances(run).values();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (AccountClass c : classes()) {
      for (ConsolidatedBalance b : balances) {
        if (b.accountClass() == c && b.consolidated().signum() != 0) {
          rows.add(row(c, b.accountCode(), b.accountName(), natural(b)));
        }
      }
    }
    List<String> notes = new ArrayList<>();
    notes.add(ConsolidationReportSupport.runNote(run));
    complete(balances, rows, notes);
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text(ConsolidationReportSupport.CODE, "Account"),
                ReportColumn.text(ConsolidationReportSupport.NAME, "Particulars"),
                ReportColumn.amount("amount", "Amount (" + run.getCurrency() + ")"))
            .groupBy(SECTION, "Section")
            .presorted()
            .withoutGrandTotal()
            .rows(rows);
    notes.forEach(builder::note);
    return builder.build();
  }

  /**
   * Total of classes in natural sign.
   *
   * @param balances balances
   * @param classes classes
   * @return total
   */
  static BigDecimal total(Collection<ConsolidatedBalance> balances, Set<AccountClass> classes) {
    return balances.stream()
        .filter(b -> classes.contains(b.accountClass()))
        .map(ConsolidatedStatementReport::natural)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Net result: income less expenses.
   *
   * @param balances balances
   * @return profit (positive) or loss
   */
  static BigDecimal netResult(Collection<ConsolidatedBalance> balances) {
    return total(balances, Set.of(AccountClass.INCOME))
        .subtract(total(balances, Set.of(AccountClass.EXPENSE)));
  }

  static Map<String, Object> row(
      AccountClass section, String code, String name, BigDecimal amount) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(SECTION, section.name());
    row.put(ConsolidationReportSupport.CODE, code);
    row.put(ConsolidationReportSupport.NAME, name);
    row.put("amount", amount);
    return row;
  }

  private static BigDecimal natural(ConsolidatedBalance b) {
    return ConsolidationReportSupport.natural(b.accountClass(), b.consolidated());
  }
}
