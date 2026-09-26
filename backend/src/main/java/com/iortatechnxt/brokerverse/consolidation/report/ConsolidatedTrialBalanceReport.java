package com.iortatechnxt.brokerverse.consolidation.report;

import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-CON-TB Consolidated Trial Balance: aggregated (translated) member balances, eliminations and
 * consolidated debit/credit per group account.
 */
@Component
public class ConsolidatedTrialBalanceReport implements ReportDefinition {

  private final ConsolidationReportSupport support;

  /**
   * Creates the report.
   *
   * @param support consolidation report helpers
   */
  public ConsolidatedTrialBalanceReport(ConsolidationReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-CON-TB",
        "Consolidated Trial Balance",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Translated member balances, eliminations and consolidated balances of a group",
        ConsolidationReportSupport.parameters(),
        Permission.CONSOLIDATION_RUN);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    ConsolidationRun run = support.run(p);
    List<Map<String, Object>> rows =
        ConsolidationReportSupport.balances(run).values().stream()
            .sorted(
                Comparator.comparing(ConsolidatedBalance::accountClass)
                    .thenComparing(ConsolidatedBalance::accountCode))
            .map(ConsolidatedTrialBalanceReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(ConsolidationReportSupport.CODE, "Account"),
            ReportColumn.text(ConsolidationReportSupport.NAME, "Account Name"),
            ReportColumn.amount("aggregated", "Aggregated"),
            ReportColumn.amount("eliminations", "Eliminations"),
            ReportColumn.amount("debit", "Consolidated Debit"),
            ReportColumn.amount("credit", "Consolidated Credit"))
        .groupBy(ConsolidationReportSupport.CLASS, "Class")
        .presorted()
        .rows(rows)
        .note(ConsolidationReportSupport.runNote(run))
        .note(
            run.isBalanced()
                ? "Consolidated trial balance is in balance."
                : "WARNING: consolidated trial balance does not balance.")
        .build();
  }

  private static Map<String, Object> row(ConsolidatedBalance b) {
    BigDecimal net = b.consolidated();
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(ConsolidationReportSupport.CLASS, b.accountClass().name());
    row.put(ConsolidationReportSupport.CODE, b.accountCode());
    row.put(ConsolidationReportSupport.NAME, b.accountName());
    row.put("aggregated", b.aggregated());
    row.put("eliminations", b.eliminations());
    row.put("debit", net.max(BigDecimal.ZERO));
    row.put("credit", net.min(BigDecimal.ZERO).negate());
    return row;
  }
}
