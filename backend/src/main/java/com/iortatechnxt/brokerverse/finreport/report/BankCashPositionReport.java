package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.LedgerQuery;
import com.iortatechnxt.brokerverse.finreport.service.MovementRow;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * FIN-CB-POSITION – Bank / Cash Position (PREMIA FGL007A): per bank or cash account and currency,
 * the opening balance, deposits (debits), withdrawals (credits) and closing balance in local
 * currency, with the closing balance in the account currency. Only accounts whose category is
 * flagged as bank / cash are included. With From = To = today it is the cashier's daily position.
 */
@Component
public class BankCashPositionReport implements ReportDefinition {

  private static final String MAIN = "mainAccount";
  private static final String OPENING = "opening";
  private static final String DEPOSITS = "deposits";
  private static final String WITHDRAWALS = "withdrawals";
  private static final String CLOSING = "closing";
  private static final String CLOSING_SIDE = "closingSide";
  private static final String FC_CLOSING = "fcClosing";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public BankCashPositionReport(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.add(FinParams.from().withDefault("TODAY"));
    params.add(FinParams.to());
    params.add(FinParams.division());
    params.add(FinParams.department());
    params.addAll(FinParams.mainRange());
    params.addAll(FinParams.subRange());
    return new ReportMetadata(
        "FIN-CB-POSITION",
        "Bank / Cash Position",
        ReportCategory.GENERAL_LEDGER,
        "Opening, deposits, withdrawals and closing of bank and cash accounts (FGL007A)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Set<Long> ids = FinReportSupport.selectedAccounts(h, p);
    ids.removeIf(id -> !h.node(id).bankOrCash());
    LedgerQuery q =
        new LedgerQuery(
            companyId,
            FinParams.branch(p),
            FinParams.costCenter(p),
            p.date(FinParams.FROM),
            p.date(FinParams.TO),
            ids,
            null,
            null);
    List<MovementRow> movements =
        q.costCenter() == null ? queries.movements(q) : queries.dimensionMovements(q);
    Map<List<Object>, MovementRow> byAccountCurrency =
        TrialBalances.sumByKey(movements, r -> List.of(r.accountId(), r.currency()));
    List<Map<String, Object>> rows = new ArrayList<>();
    byAccountCurrency.values().stream()
        .filter(r -> !r.isEmpty())
        .sorted(
            Comparator.comparing((MovementRow r) -> h.node(r.accountId()).code())
                .thenComparing(MovementRow::currency))
        .forEach(r -> rows.add(cells(h, r)));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(Vouchers.SUB_AC, "Sub A/c"),
            ReportColumn.text(Vouchers.ACCOUNT_NAME, "Bank / Cash Account"),
            ReportColumn.amount(OPENING, "Opening Balance Dr/(Cr)"),
            ReportColumn.amount(DEPOSITS, "Deposits"),
            ReportColumn.amount(WITHDRAWALS, "Withdrawals"),
            ReportColumn.amount(CLOSING, "Closing Balance Dr/(Cr)"),
            ReportColumn.text(CLOSING_SIDE, "Dr/Cr"),
            ReportColumn.text(Vouchers.CURRENCY, "Currency"),
            ReportColumn.amountNoTotal(FC_CLOSING, "FC Value Dr/(Cr)"))
        .groupBy(MAIN, "Bank Code")
        .presorted()
        .rows(rows)
        .note("Local currency amounts; FC value = closing balance in the account currency.")
        .build();
  }

  private static Map<String, Object> cells(AccountHierarchy h, MovementRow r) {
    AccountNode account = h.node(r.accountId());
    return FinRows.cells(
        MAIN,
        h.mainOf(account.id()).caption(),
        Vouchers.SUB_AC,
        account.code(),
        Vouchers.ACCOUNT_NAME,
        account.name(),
        OPENING,
        r.openBase(),
        DEPOSITS,
        r.debitBase(),
        WITHDRAWALS,
        r.creditBase(),
        CLOSING,
        r.closeBase(),
        CLOSING_SIDE,
        FinRows.drCr(r.closeBase()),
        Vouchers.CURRENCY,
        r.currency(),
        FC_CLOSING,
        r.closeFc());
  }
}
