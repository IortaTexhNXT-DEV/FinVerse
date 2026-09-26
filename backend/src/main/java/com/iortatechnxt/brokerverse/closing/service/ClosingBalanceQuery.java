package com.iortatechnxt.brokerverse.closing.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Income and expense balances per branch, account, cost centre and line of business (base
 * currency), aggregated in SQL from the posted ledger — the input of the year-end closing journal.
 * Dimensions are kept so that accounts requiring a cost centre or line of business can be closed.
 */
@Component
public class ClosingBalanceQuery {

  private static final String SQL =
      """
      select e.branch_id, e.account_id, a.code, e.cost_center, e.business_line,
             sum(e.debit_base - e.credit_base) as net_debit
      from gl_ledger_entry e join coa_account a on a.id = e.account_id
      where e.company_id = ? and e.value_date <= ? and a.account_class in ('INCOME', 'EXPENSE')
      group by e.branch_id, e.account_id, a.code, e.cost_center, e.business_line
      having sum(e.debit_base - e.credit_base) <> 0
      order by e.branch_id, a.code, e.cost_center, e.business_line
      """;

  private static final String NOMINAL_SQL =
      """
      select coalesce(sum(e.debit_base - e.credit_base), 0)
      from gl_ledger_entry e join coa_account a on a.id = e.account_id
      where e.company_id = ? and e.value_date <= ? and a.account_class in ('INCOME', 'EXPENSE')
      """;

  private static final String TB_SQL =
      """
      select coalesce(sum(e.debit_base - e.credit_base), 0)
      from gl_ledger_entry e
      where e.company_id = ? and e.value_date <= ?
      """;

  private final JdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   */
  public ClosingBalanceQuery(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Non-zero income and expense balances as of a date.
   *
   * @param companyId company
   * @param asOf date (inclusive)
   * @return balances
   */
  public List<PnlBalance> balances(Long companyId, LocalDate asOf) {
    return jdbc.query(
        SQL,
        (rs, i) ->
            new PnlBalance(
                rs.getLong("branch_id"),
                rs.getString("code"),
                rs.getString("cost_center"),
                rs.getString("business_line"),
                rs.getBigDecimal("net_debit")),
        companyId,
        Date.valueOf(asOf));
  }

  /**
   * Net income and expense balance as of a date (zero after a year-end close, FRBS 2.7.1).
   *
   * @param companyId company
   * @param asOf date (inclusive)
   * @return net debit
   */
  public BigDecimal nominalBalance(Long companyId, LocalDate asOf) {
    return jdbc.queryForObject(NOMINAL_SQL, BigDecimal.class, companyId, Date.valueOf(asOf));
  }

  /**
   * Trial balance difference (total debit minus total credit) as of a date (FRBS 2.7.1).
   *
   * @param companyId company
   * @param asOf date (inclusive)
   * @return difference, zero when the books balance
   */
  public BigDecimal trialBalanceDifference(Long companyId, LocalDate asOf) {
    return jdbc.queryForObject(TB_SQL, BigDecimal.class, companyId, Date.valueOf(asOf));
  }

  /**
   * Income or expense balance of one account and dimension combination.
   *
   * @param branchId branch
   * @param accountCode account
   * @param costCenter cost centre or null
   * @param businessLine line of business or null
   * @param netDebit debit minus credit, base currency
   */
  public record PnlBalance(
      Long branchId,
      String accountCode,
      String costCenter,
      String businessLine,
      BigDecimal netDebit) {}
}
