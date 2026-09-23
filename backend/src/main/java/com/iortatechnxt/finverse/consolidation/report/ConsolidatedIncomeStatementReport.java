package com.iortatechnxt.finverse.consolidation.report;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-CON-PL Consolidated Income Statement: income and expenses translated at average rates, net of
 * inter-company eliminations.
 */
@Component
public class ConsolidatedIncomeStatementReport extends ConsolidatedStatementReport
    implements ReportDefinition {

  /**
   * Creates the report.
   *
   * @param support consolidation report helpers
   */
  public ConsolidatedIncomeStatementReport(ConsolidationReportSupport support) {
    super(support);
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-CON-PL",
        "Consolidated Income Statement",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Consolidated income, expenses and net result of a group",
        ConsolidationReportSupport.parameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return build(p);
  }

  @Override
  List<AccountClass> classes() {
    return List.of(AccountClass.INCOME, AccountClass.EXPENSE);
  }

  @Override
  void complete(
      Collection<ConsolidatedBalance> balances,
      List<Map<String, Object>> rows,
      List<String> notes) {
    notes.add("Net result (income less expenses): " + netResult(balances).toPlainString());
  }
}
