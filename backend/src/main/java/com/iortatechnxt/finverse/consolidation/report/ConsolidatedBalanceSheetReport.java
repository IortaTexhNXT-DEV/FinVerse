package com.iortatechnxt.finverse.consolidation.report;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * GL-CON-BS Consolidated Balance Sheet. Equity includes the unappropriated net result (income less
 * expenses not yet closed to retained earnings), so assets equal liabilities plus equity.
 */
@Component
public class ConsolidatedBalanceSheetReport extends ConsolidatedStatementReport
    implements ReportDefinition {

  /**
   * Creates the report.
   *
   * @param support consolidation report helpers
   */
  public ConsolidatedBalanceSheetReport(ConsolidationReportSupport support) {
    super(support);
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-CON-BS",
        "Consolidated Balance Sheet",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Consolidated assets, liabilities and equity of a group after eliminations",
        ConsolidationReportSupport.parameters(),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return build(p);
  }

  @Override
  List<AccountClass> classes() {
    return List.of(AccountClass.ASSET, AccountClass.LIABILITY, AccountClass.EQUITY);
  }

  @Override
  void complete(
      Collection<ConsolidatedBalance> balances,
      List<Map<String, Object>> rows,
      List<String> notes) {
    BigDecimal result = netResult(balances);
    rows.add(
        row(AccountClass.EQUITY, "", "Net result not yet closed to retained earnings", result));
    BigDecimal assets = total(balances, Set.of(AccountClass.ASSET));
    BigDecimal liabilitiesAndEquity =
        total(balances, Set.of(AccountClass.LIABILITY, AccountClass.EQUITY)).add(result);
    notes.add("Total assets: " + assets.toPlainString());
    notes.add("Total liabilities and equity: " + liabilitiesAndEquity.toPlainString());
  }
}
