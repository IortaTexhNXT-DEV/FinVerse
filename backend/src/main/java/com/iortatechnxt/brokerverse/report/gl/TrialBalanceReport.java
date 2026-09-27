package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
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
 * Trial balance as of a date: debit/credit balance of every account with activity, grouped by
 * account class, with a balance check note.
 */
@Component
public class TrialBalanceReport implements ReportDefinition {

  private static final String CODE = "GL-TB";
  private static final String CLASS = "accountClass";

  private final LedgerQueryService ledger;
  private final GlReportSupport support;

  /**
   * Creates the report.
   *
   * @param ledger ledger read model
   * @param support GL helpers
   */
  public TrialBalanceReport(LedgerQueryService ledger, GlReportSupport support) {
    this.ledger = ledger;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "Trial Balance",
        ReportCategory.GENERAL_LEDGER,
        "Debit and credit balances of all accounts as of a date",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.optional("includeZero", "Include zero balances", ParameterType.BOOLEAN)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    Long branchId = p.optionalLong(GlReportSupport.BRANCH).orElse(null);
    boolean includeZero = p.flag("includeZero");
    Map<Long, GlAccount> accounts = support.accountsById(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    for (AccountBalance b :
        ledger.balances(new BalanceQuery(companyId, branchId, null, asOf, false))) {
      BigDecimal net = b.netBase();
      if (net.signum() == 0 && !includeZero) {
        continue;
      }
      GlAccount a = accounts.get(b.accountId());
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(CLASS, a.getAccountClass().name());
      row.put("code", a.getCode());
      row.put("name", a.getName());
      row.put("debit", net.signum() > 0 ? net : BigDecimal.ZERO);
      row.put("credit", net.signum() < 0 ? net.negate() : BigDecimal.ZERO);
      totalDebit = totalDebit.add(net.max(BigDecimal.ZERO));
      totalCredit = totalCredit.add(net.min(BigDecimal.ZERO).negate());
      rows.add(row);
    }
    rows.sort(
        Comparator.comparing((Map<String, Object> r) -> AccountClass.valueOf((String) r.get(CLASS)))
            .thenComparing(r -> (String) r.get("code")));
    String check =
        totalDebit.compareTo(totalCredit) == 0
            ? "Trial balance is in balance."
            : "WARNING: Trial balance difference of " + totalDebit.subtract(totalCredit);
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Account Code"),
            ReportColumn.text("name", "Account Name"),
            ReportColumn.amount("debit", "Debit"),
            ReportColumn.amount("credit", "Credit"))
        .groupBy(CLASS, "Class")
        .presorted()
        .rows(rows)
        .note(check)
        .build();
  }
}
