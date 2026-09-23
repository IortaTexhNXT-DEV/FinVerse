package com.iortatechnxt.finverse.ledger.service;

import com.iortatechnxt.finverse.coa.service.AccountUsageChecker;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntry;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntryRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model of the general ledger: balances, trial balance inputs and account statements.
 *
 * <p>Also implements {@link AccountUsageChecker} for the chart of accounts module.
 */
@Service
@Transactional(readOnly = true)
public class LedgerQueryService implements AccountUsageChecker {

  private static final String FILTERS =
      " where company_id = ? and balance_date <= ?"
          + " and (cast(? as date) is null or balance_date >= ?)"
          + " and (cast(? as bigint) is null or branch_id = ?)";

  private static final String BALANCES_SQL =
      "select account_id, null as currency, sum(debit_fc) dfc, sum(credit_fc) cfc,"
          + " sum(debit_base) db, sum(credit_base) cb from gl_daily_balance"
          + FILTERS
          + " group by account_id";

  private static final String BALANCES_BY_CURRENCY_SQL =
      "select account_id, currency, sum(debit_fc) dfc, sum(credit_fc) cfc,"
          + " sum(debit_base) db, sum(credit_base) cb from gl_daily_balance"
          + FILTERS
          + " group by account_id, currency";

  private static final String NET_BALANCE_SQL =
      "select coalesce(sum(debit_base - credit_base), 0) from gl_daily_balance"
          + " where company_id = ? and account_id = ? and balance_date <= ?"
          + " and (cast(? as bigint) is null or branch_id = ?)";

  private final JdbcTemplate jdbc;
  private final LedgerEntryRepository entries;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC template
   * @param entries ledger repository
   */
  public LedgerQueryService(JdbcTemplate jdbc, LedgerEntryRepository entries) {
    this.jdbc = jdbc;
    this.entries = entries;
  }

  @Override
  public boolean hasPostings(Long accountId) {
    return entries.existsByAccountId(accountId);
  }

  /**
   * Aggregates balances per account (and optionally per currency).
   *
   * @param q query
   * @return balances of accounts with activity
   */
  public List<AccountBalance> balances(BalanceQuery q) {
    String sql = q.byCurrency() ? BALANCES_BY_CURRENCY_SQL : BALANCES_SQL;
    Date from = q.fromDate() == null ? null : Date.valueOf(q.fromDate());
    return jdbc.query(
        sql,
        (rs, i) ->
            new AccountBalance(
                rs.getLong("account_id"),
                rs.getString("currency"),
                rs.getBigDecimal("dfc"),
                rs.getBigDecimal("cfc"),
                rs.getBigDecimal("db"),
                rs.getBigDecimal("cb")),
        q.companyId(),
        Date.valueOf(q.toDate()),
        from,
        from,
        q.branchId(),
        q.branchId());
  }

  /**
   * Net base balance (debit positive) of an account as of a date.
   *
   * @param companyId company
   * @param accountId account
   * @param branchId branch or null
   * @param asOf date
   * @return net balance
   */
  public BigDecimal netBalance(Long companyId, Long accountId, Long branchId, LocalDate asOf) {
    BigDecimal result =
        jdbc.queryForObject(
            NET_BALANCE_SQL,
            BigDecimal.class,
            companyId,
            accountId,
            Date.valueOf(asOf),
            branchId,
            branchId);
    return Money.round(result);
  }

  /**
   * Account statement: opening balance plus entries in a date range.
   *
   * @param companyId company
   * @param accountId account
   * @param branchId branch or null
   * @param from start date
   * @param to end date
   * @return statement
   */
  public AccountStatement statement(
      Long companyId, Long accountId, Long branchId, LocalDate from, LocalDate to) {
    BigDecimal opening = netBalance(companyId, accountId, branchId, from.minusDays(1));
    List<LedgerEntry> rows = entries.statement(companyId, accountId, branchId, from, to);
    return new AccountStatement(opening, rows);
  }

  /**
   * Account statement result.
   *
   * @param openingBalance net base balance before the start date (debit positive)
   * @param entries entries within the range in posting order
   */
  public record AccountStatement(BigDecimal openingBalance, List<LedgerEntry> entries) {}
}
