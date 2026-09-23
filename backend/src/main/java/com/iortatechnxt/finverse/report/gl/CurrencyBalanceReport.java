package com.iortatechnxt.finverse.report.gl;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.ledger.service.AccountBalance;
import com.iortatechnxt.finverse.ledger.service.BalanceQuery;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Currency-wise GL balances listed alongside their base currency equivalents. */
@Component
public class CurrencyBalanceReport implements ReportDefinition {

  private final LedgerQueryService ledger;
  private final GlReportSupport support;

  /**
   * Creates the report.
   *
   * @param ledger ledger read model
   * @param support GL helpers
   */
  public CurrencyBalanceReport(LedgerQueryService ledger, GlReportSupport support) {
    this.ledger = ledger;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-CCY",
        "Currency-wise GL Balances",
        ReportCategory.GENERAL_LEDGER,
        "Foreign currency balances per account with base currency equivalent",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Map<Long, GlAccount> accounts = support.accountsById(companyId);
    var query =
        new BalanceQuery(
            companyId,
            p.optionalLong(GlReportSupport.BRANCH).orElse(null),
            null,
            p.date(GlReportSupport.AS_OF),
            true);
    List<Map<String, Object>> rows =
        ledger.balances(query).stream()
            .filter(b -> b.netBase().signum() != 0 || b.netFc().signum() != 0)
            .map(b -> row(accounts.get(b.accountId()), b))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Account"),
            ReportColumn.text("name", "Name"),
            ReportColumn.amountNoTotal("fc", "Balance (FC)"),
            ReportColumn.amount("base", "Balance (Base)"))
        .groupBy("currency", "Currency")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> row(GlAccount a, AccountBalance b) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("currency", b.currency());
    m.put("code", a.getCode());
    m.put("name", a.getName());
    m.put("fc", b.netFc());
    m.put("base", b.netBase());
    return m;
  }
}
