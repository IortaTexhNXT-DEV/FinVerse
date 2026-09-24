package com.iortatechnxt.brokerverse.consolidation.report;

import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyReconciliationService;
import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyReconciliationService.ReconciliationLine;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-ICREC Inter-Company Reconciliation: due-from against due-to balances per company pair,
 * direction and currency, flagging mismatches.
 */
@Component
public class IntercompanyReconciliationReport implements ReportDefinition {

  private static final String ONLY_MISMATCHES = "mismatchesOnly";

  private final IntercompanyReconciliationService reconciliation;
  private final ConsolidationReportSupport support;

  /**
   * Creates the report.
   *
   * @param reconciliation reconciliation service
   * @param support helpers
   */
  public IntercompanyReconciliationReport(
      IntercompanyReconciliationService reconciliation, ConsolidationReportSupport support) {
    this.reconciliation = reconciliation;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-ICREC",
        "Inter-Company Reconciliation",
        ReportCategory.RECONCILIATION,
        "Due-to against due-from balances of related companies with mismatches",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.optional(ONLY_MISMATCHES, "Mismatches only", ParameterType.BOOLEAN)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    boolean onlyMismatches = p.flag(ONLY_MISMATCHES);
    List<ReconciliationLine> lines =
        reconciliation.reconcile(
            p.longValue(GlReportSupport.COMPANY), p.date(GlReportSupport.AS_OF));
    List<Map<String, Object>> rows =
        lines.stream().filter(l -> !onlyMismatches || !l.matched()).map(this::row).toList();
    long mismatches = lines.stream().filter(l -> !l.matched()).count();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("creditor", "Due-From Company"),
            ReportColumn.text("dueFromAccount", "Due-From A/c"),
            ReportColumn.text("debtor", "Due-To Company"),
            ReportColumn.text("dueToAccount", "Due-To A/c"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("dueFrom", "Due-From (FC)"),
            ReportColumn.amountNoTotal("dueTo", "Due-To (FC)"),
            ReportColumn.amountNoTotal("difference", "Difference (FC)"),
            ReportColumn.text("status", "Status"))
        .rows(rows)
        .note(
            mismatches == 0
                ? "All inter-company balances agree."
                : mismatches + " inter-company balance(s) do not agree.")
        .note("Balances are compared in transaction currency.")
        .build();
  }

  private Map<String, Object> row(ReconciliationLine l) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("creditor", support.companyCode(l.creditorCompanyId()));
    row.put("dueFromAccount", l.dueFromAccount());
    row.put("debtor", support.companyCode(l.debtorCompanyId()));
    row.put("dueToAccount", l.dueToAccount());
    row.put("currency", l.currency());
    row.put("dueFrom", l.dueFromFc());
    row.put("dueTo", l.dueToFc());
    row.put("difference", l.difference());
    row.put("status", l.matched() ? "MATCHED" : "MISMATCH");
    return row;
  }
}
