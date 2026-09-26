package com.iortatechnxt.brokerverse.consolidation.report;

import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRunLine;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-CON-ELIM Elimination Details: every elimination and translation adjustment line of a run,
 * grouped by rule.
 */
@Component
public class EliminationDetailsReport implements ReportDefinition {

  private static final String RULE = "rule";

  private final ConsolidationReportSupport support;

  /**
   * Creates the report.
   *
   * @param support consolidation report helpers
   */
  public EliminationDetailsReport(ConsolidationReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-CON-ELIM",
        "Consolidation Elimination Details",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Inter-company, investment/equity eliminations and translation adjustments of a run",
        ConsolidationReportSupport.parameters(),
        Permission.CONSOLIDATION_RUN);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    ConsolidationRun run = support.run(p);
    List<Map<String, Object>> rows =
        run.getLines().stream()
            .filter(l -> l.getType() != ConsolidationLineType.TRANSLATED)
            .map(this::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("company", "Company"),
            ReportColumn.text(ConsolidationReportSupport.CODE, "Account"),
            ReportColumn.text(ConsolidationReportSupport.NAME, "Account Name"),
            ReportColumn.text("description", "Description"),
            ReportColumn.amount("debit", "Debit"),
            ReportColumn.amount("credit", "Credit"))
        .groupBy(RULE, "Rule")
        .rows(rows)
        .note(ConsolidationReportSupport.runNote(run))
        .note("Each rule is balanced: debit equals credit within every group.")
        .build();
  }

  private Map<String, Object> row(ConsolidationRunLine l) {
    BigDecimal amount = l.getAmount();
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(RULE, l.getRuleCode() == null ? "TRANSLATION_ADJUSTMENT" : l.getRuleCode());
    row.put("company", support.companyCode(l.getCompanyId()));
    row.put(ConsolidationReportSupport.CODE, l.getAccountCode());
    row.put(ConsolidationReportSupport.NAME, l.getAccountName());
    row.put("description", l.getDescription());
    row.put("debit", amount.max(BigDecimal.ZERO));
    row.put("credit", amount.min(BigDecimal.ZERO).negate());
    return row;
  }
}
