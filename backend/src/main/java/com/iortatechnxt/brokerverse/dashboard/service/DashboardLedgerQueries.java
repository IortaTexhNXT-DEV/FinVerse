package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SQL read model of the executive dashboard over platform tables only (general ledger daily
 * balances, chart of accounts, open-item sub-ledger and parties), so the dashboard needs no
 * dependency on business modules for ledger figures. Every figure is aggregated in the database.
 *
 * <p>All statements are constants with bind parameters. Accounts are selected by statement line
 * ({@code report_group = any(?)}) or by code prefix ({@code code like any(?)}); an optional branch
 * uses the {@code (cast(? as bigint) is null or branch_id = ?)} idiom.
 */
@Service
@Transactional(readOnly = true)
public class DashboardLedgerQueries {

  /** Lower bound of "balance up to" ranges. */
  public static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

  private static final String ACCOUNT_FILTER =
      " and (a.report_group = any(?) or a.code like any(?))"
          + " and (cast(? as bigint) is null or d.branch_id = ?)";

  private static final String MONTHLY_NET =
      "select to_char(d.balance_date, 'YYYY-MM') as month,"
          + " coalesce(sum(d.debit_base - d.credit_base), 0) as net"
          + " from gl_daily_balance d join coa_account a on a.id = d.account_id"
          + " where d.company_id = ? and d.balance_date between ? and ?"
          + ACCOUNT_FILTER
          + " group by to_char(d.balance_date, 'YYYY-MM')";

  private static final String NET =
      "select coalesce(sum(d.debit_base - d.credit_base), 0)"
          + " from gl_daily_balance d join coa_account a on a.id = d.account_id"
          + " where d.company_id = ? and d.balance_date between ? and ?"
          + ACCOUNT_FILTER;

  private static final String ACCOUNT_BALANCES =
      "select a.code, a.name, coalesce(sum(d.debit_base - d.credit_base), 0) as balance"
          + " from gl_daily_balance d join coa_account a on a.id = d.account_id"
          + " where d.company_id = ? and d.balance_date <= ?"
          + ACCOUNT_FILTER
          + " group by a.code, a.name order by a.code";

  private static final String OUTSTANDING =
      "o.base_amount * (o.amount - o.settled_amount) / o.amount";

  private static final String PAYABLES_DUE =
      "select coalesce(sum("
          + OUTSTANDING
          + ") filter (where o.due_date < ?), 0) as overdue,"
          + " coalesce(sum("
          + OUTSTANDING
          + ") filter (where o.due_date between ? and ?), 0) as due_week,"
          + " coalesce(sum("
          + OUTSTANDING
          + ") filter (where o.due_date between ? and ?), 0) as due_month,"
          + " coalesce(sum("
          + OUTSTANDING
          + "), 0) as total, count(*) as items"
          + " from sl_open_item o join pty_party p on p.id = o.party_id"
          + " where o.company_id = ? and o.direction = 'CREDIT'"
          + " and o.status in ('OPEN', 'PARTIALLY_SETTLED') and p.party_type = any(?)"
          + " and (cast(? as bigint) is null or o.branch_id = ?)";

  private static final int WEEK_DAYS = 7;
  private static final int MONTH_DAYS = 30;

  private final JdbcTemplate jdbc;

  /**
   * Creates the read model.
   *
   * @param jdbc JDBC template
   */
  public DashboardLedgerQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Monthly net movement (debit minus credit, base currency) of the selected accounts.
   *
   * @param scope company, branch and date range
   * @param accounts account selection
   * @return net per month (months without movement are absent)
   */
  public Map<YearMonth, BigDecimal> monthlyNet(Scope scope, Accounts accounts) {
    Map<YearMonth, BigDecimal> result = new TreeMap<>();
    jdbc.query(
        MONTHLY_NET,
        rs -> {
          result.put(YearMonth.parse(rs.getString("month")), rs.getBigDecimal("net"));
        },
        scope.companyId(),
        Date.valueOf(scope.from()),
        Date.valueOf(scope.to()),
        accounts.groups(),
        accounts.patterns(),
        scope.branchId(),
        scope.branchId());
    return result;
  }

  /**
   * Net movement (debit minus credit, base currency) of the selected accounts in a date range; from
   * {@link #BEGINNING} it is the balance at the end of the last day.
   *
   * @param scope company, branch and date range
   * @param accounts account selection
   * @return net movement
   */
  public BigDecimal net(Scope scope, Accounts accounts) {
    return jdbc.queryForObject(
        NET,
        BigDecimal.class,
        scope.companyId(),
        Date.valueOf(scope.from()),
        Date.valueOf(scope.to()),
        accounts.groups(),
        accounts.patterns(),
        scope.branchId(),
        scope.branchId());
  }

  /**
   * Balance per account (debit minus credit, base currency) of the selected accounts.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param asOf balance date
   * @param accounts account selection
   * @return accounts with a posting up to the date, in code order
   */
  public List<AccountBalance> accountBalances(
      Long companyId, Long branchId, LocalDate asOf, Accounts accounts) {
    return jdbc.query(
        ACCOUNT_BALANCES,
        (rs, i) ->
            new AccountBalance(
                rs.getString("code"), rs.getString("name"), rs.getBigDecimal("balance")),
        companyId,
        Date.valueOf(asOf),
        accounts.groups(),
        accounts.patterns(),
        branchId,
        branchId);
  }

  /**
   * Outstanding payables (CREDIT open items of the given party types) by due date window.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param asOf reference date
   * @param partyTypes party types whose credit items are payables
   * @return overdue, due within 7 and 30 days (both counted from the reference date), total
   */
  public PayablesDue payablesDue(
      Long companyId, Long branchId, LocalDate asOf, Collection<String> partyTypes) {
    Date today = Date.valueOf(asOf);
    return jdbc.queryForObject(
        PAYABLES_DUE,
        (rs, i) ->
            new PayablesDue(
                rs.getBigDecimal("overdue"),
                rs.getBigDecimal("due_week"),
                rs.getBigDecimal("due_month"),
                rs.getBigDecimal("total"),
                rs.getLong("items")),
        today,
        today,
        Date.valueOf(asOf.plusDays(WEEK_DAYS)),
        today,
        Date.valueOf(asOf.plusDays(MONTH_DAYS)),
        companyId,
        partyTypes.toArray(String[]::new),
        branchId,
        branchId);
  }

  /**
   * Company, optional branch and inclusive date range of a query.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param from first day
   * @param to last day
   */
  public record Scope(Long companyId, Long branchId, LocalDate from, LocalDate to) {}

  /**
   * Account selection: statement lines and/or code prefixes (either list may be empty).
   *
   * @param reportGroups statement lines ({@code report_group})
   * @param codePrefixes account code prefixes
   */
  public record Accounts(List<String> reportGroups, List<String> codePrefixes) {

    /** Canonical constructor copying the lists. */
    public Accounts {
      reportGroups = List.copyOf(reportGroups);
      codePrefixes = List.copyOf(codePrefixes);
    }

    /**
     * Accounts of statement lines.
     *
     * @param groups statement lines
     * @return selection
     */
    public static Accounts ofGroups(List<String> groups) {
      return new Accounts(groups, List.of());
    }

    /**
     * Accounts by code prefix.
     *
     * @param prefixes code prefixes
     * @return selection
     */
    public static Accounts ofPrefixes(List<String> prefixes) {
      return new Accounts(List.of(), prefixes);
    }

    String[] groups() {
      return reportGroups.toArray(String[]::new);
    }

    String[] patterns() {
      return codePrefixes.stream().map(p -> p + "%").toArray(String[]::new);
    }
  }

  /**
   * Balance of one account.
   *
   * @param code account code
   * @param name account name
   * @param balance debit minus credit, base currency
   */
  public record AccountBalance(String code, String name, BigDecimal balance) {}

  /**
   * Payables by due date window (base currency, positive amounts owed).
   *
   * @param overdue due before the reference date
   * @param dueIn7Days due from the reference date to 7 days later
   * @param dueIn30Days due from the reference date to 30 days later (includes the 7 days)
   * @param total all outstanding payables
   * @param openItems number of outstanding items
   */
  public record PayablesDue(
      BigDecimal overdue,
      BigDecimal dueIn7Days,
      BigDecimal dueIn30Days,
      BigDecimal total,
      long openItems) {}
}
