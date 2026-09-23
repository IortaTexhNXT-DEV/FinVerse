package com.iortatechnxt.finverse.ledger.service;

import com.iortatechnxt.finverse.ledger.domain.LedgerEntryValues;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains {@code gl_daily_balance}: debit/credit totals per company, branch, account, currency
 * and value date.
 *
 * <p>Balances are updated with an atomic {@code INSERT ... ON CONFLICT DO UPDATE}, so concurrent
 * postings to the same account never lose updates. Reports aggregate this table (far fewer rows
 * than the ledger) for trial balances and statements.
 */
@Repository
public class LedgerBalanceStore {

  private static final String UPSERT =
      """
      insert into gl_daily_balance
        (company_id, branch_id, account_id, currency, balance_date,
         debit_fc, credit_fc, debit_base, credit_base)
      values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      on conflict (company_id, branch_id, account_id, currency, balance_date) do update set
        debit_fc = gl_daily_balance.debit_fc + excluded.debit_fc,
        credit_fc = gl_daily_balance.credit_fc + excluded.credit_fc,
        debit_base = gl_daily_balance.debit_base + excluded.debit_base,
        credit_base = gl_daily_balance.credit_base + excluded.credit_base
      """;

  private final JdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template
   */
  public LedgerBalanceStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Adds a posted entry to the daily balance. Must run inside the posting transaction.
   *
   * @param e posted entry values
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(LedgerEntryValues e) {
    jdbc.update(
        UPSERT,
        e.companyId(),
        e.branchId(),
        e.accountId(),
        e.currency(),
        e.valueDate(),
        e.debitFc(),
        e.creditFc(),
        e.debitBase(),
        e.creditBase());
  }
}
