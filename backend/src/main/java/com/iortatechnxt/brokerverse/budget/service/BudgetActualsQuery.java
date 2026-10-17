package com.iortatechnxt.brokerverse.budget.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Monthly actuals per account and cost centre, aggregated in SQL from the posted ledger.
 *
 * <p>Year-end CLOSING journals are excluded: they zero income and expense accounts and would
 * otherwise wipe out the actuals of a closed year.
 */
@Component
public class BudgetActualsQuery {

  private static final String SQL =
      """
      select account_id, cost_center, cast(date_trunc('month', value_date) as date) as month_start,
             sum(debit_base - credit_base) as net_debit
      from gl_ledger_entry
      where company_id = ? and value_date between ? and ? and journal_type <> 'CLOSING'
      group by account_id, cost_center, cast(date_trunc('month', value_date) as date)
      """;

  private final JdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc JDBC template
   */
  public BudgetActualsQuery(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Net debit movements per account, cost centre and calendar month in a date range.
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @return movements
   */
  public List<MonthlyActual> monthly(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        SQL,
        (rs, i) ->
            new MonthlyActual(
                rs.getLong("account_id"),
                rs.getString("cost_center"),
                rs.getDate("month_start").toLocalDate(),
                rs.getBigDecimal("net_debit")),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to));
  }

  /**
   * Net debit movement of an account and cost centre in one month.
   *
   * @param accountId account
   * @param costCenter cost centre or null
   * @param monthStart first day of the month
   * @param netDebit debit minus credit, base currency
   */
  public record MonthlyActual(
      Long accountId, String costCenter, LocalDate monthStart, BigDecimal netDebit) {}
}
