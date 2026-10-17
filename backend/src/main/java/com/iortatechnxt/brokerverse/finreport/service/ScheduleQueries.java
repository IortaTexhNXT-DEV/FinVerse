package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SQL read model of the account schedule engine (FRBS 3.2.0): the accounts a definition selects,
 * the figures of each row aggregated in the database from the posted ledger, and the increases per
 * row and day used for FIFO ageing. Every statement is a constant; the grouping, currency and side
 * are bind parameters.
 */
@Service
@Transactional(readOnly = true)
public class ScheduleQueries {

  private static final String ACCOUNTS_BY_PREFIX =
      "select id, code, name from coa_account"
          + " where company_id = ? and postable and code like any(?) order by code";

  private static final String ACCOUNTS_BY_GROUP =
      "select id, code, name from coa_account"
          + " where company_id = ? and postable and report_group = any(?) order by code";

  private static final String ROW_SOURCE =
      " from (select case cast(? as varchar)"
          + " when 'ACCOUNT' then cast(e.account_id as varchar)"
          + " when 'PARTY' then coalesce(e.party_code, '')"
          + " when 'DOCUMENT' then coalesce(e.party_code, '') || '|' || coalesce(e.reference, '')"
          + " when 'COST_CENTER' then coalesce(e.cost_center, '')"
          + " when 'BRANCH' then cast(e.branch_id as varchar)"
          + " else coalesce(e.business_line, '') end grp,"
          + " e.value_date,"
          + " case when cast(? as boolean) then e.debit_fc else e.debit_base end dr,"
          + " case when cast(? as boolean) then e.credit_fc else e.credit_base end cr"
          + " from gl_ledger_entry e"
          + " where e.company_id = ? and e.account_id = any(?) and e.value_date <= ?"
          + " and (cast(? as varchar) is null or e.currency = ?)"
          + " and (cast(? as bigint) is null or e.branch_id = ?)) x";

  private static final String FIGURES =
      "select grp,"
          + " coalesce(sum(dr - cr) filter (where value_date < ?), 0) opening,"
          + " coalesce(sum(dr) filter (where value_date >= ?), 0) debits,"
          + " coalesce(sum(cr) filter (where value_date >= ?), 0) credits,"
          + " coalesce(sum(dr - cr) filter (where value_date >= ?), 0) year_to_date,"
          + " coalesce(sum(dr - cr), 0) closing,"
          + " coalesce(sum(dr - cr) filter (where value_date between ? and ?), 0) comparative"
          + ROW_SOURCE
          + " group by grp";

  private static final String INCREASES =
      "select grp, value_date,"
          + " coalesce(sum(case when cast(? as boolean) then dr else cr end), 0) increase"
          + ROW_SOURCE
          + " group by grp, value_date"
          + " having coalesce(sum(case when cast(? as boolean) then dr else cr end), 0) > 0"
          + " order by grp, value_date desc";

  private static final String BRANCH_NAMES =
      "select cast(id as varchar) id, code, name from org_branch where company_id = ?";

  private static final String YEAR_START_MONTH =
      "select fiscal_year_start_month from org_company where id = ?";

  private final JdbcTemplate jdbc;

  /**
   * Creates the queries.
   *
   * @param jdbc JDBC template
   */
  public ScheduleQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The postable accounts a definition selects.
   *
   * @param companyId company
   * @param kind prefixes or report groups
   * @param entries selector entries
   * @return accounts in code order
   */
  public List<AccountRef> accounts(Long companyId, SelectorKind kind, List<String> entries) {
    boolean prefix = kind == SelectorKind.ACCOUNT_PREFIX;
    String[] values = entries.stream().map(e -> prefix ? e + "%" : e).toArray(String[]::new);
    return jdbc.query(
        prefix ? ACCOUNTS_BY_PREFIX : ACCOUNTS_BY_GROUP,
        (rs, i) -> new AccountRef(rs.getLong("id"), rs.getString("code"), rs.getString("name")),
        companyId,
        values);
  }

  /**
   * The figures of every row.
   *
   * @param selection rows, accounts and filters
   * @param window dates of the figures
   * @return figures per row key
   */
  public List<RowFigures> figures(Selection selection, Window window) {
    return jdbc.query(
        FIGURES,
        (rs, i) ->
            new RowFigures(
                rs.getString("grp"),
                rs.getBigDecimal("opening"),
                rs.getBigDecimal("debits"),
                rs.getBigDecimal("credits"),
                rs.getBigDecimal("year_to_date"),
                rs.getBigDecimal("closing"),
                rs.getBigDecimal("comparative")),
        Date.valueOf(window.from()),
        Date.valueOf(window.from()),
        Date.valueOf(window.from()),
        Date.valueOf(window.yearStart()),
        Date.valueOf(window.comparativeFrom()),
        Date.valueOf(window.comparativeTo()),
        selection.grouping().name(),
        selection.foreignCurrency(),
        selection.foreignCurrency(),
        selection.companyId(),
        selection.ids(),
        Date.valueOf(window.asOf()),
        selection.currency(),
        selection.currency(),
        selection.branchId(),
        selection.branchId());
  }

  /**
   * The increases (debits of a debit schedule, credits of a credit schedule) per row and day,
   * newest first, for FIFO ageing.
   *
   * @param selection rows, accounts and filters
   * @param asOf as-of date
   * @param debitSide whether debits increase the balance
   * @return increases
   */
  public List<Increase> increases(Selection selection, LocalDate asOf, boolean debitSide) {
    return jdbc.query(
        INCREASES,
        (rs, i) ->
            new Increase(
                rs.getString("grp"),
                rs.getDate("value_date").toLocalDate(),
                rs.getBigDecimal("increase")),
        debitSide,
        selection.grouping().name(),
        selection.foreignCurrency(),
        selection.foreignCurrency(),
        selection.companyId(),
        selection.ids(),
        Date.valueOf(asOf),
        selection.currency(),
        selection.currency(),
        selection.branchId(),
        selection.branchId(),
        debitSide);
  }

  /**
   * Branch codes and names by id.
   *
   * @param companyId company
   * @return {code, name} per branch id
   */
  public Map<String, String[]> branchNames(Long companyId) {
    Map<String, String[]> names = new HashMap<>();
    jdbc.query(
        BRANCH_NAMES,
        rs -> {
          names.put(rs.getString("id"), new String[] {rs.getString("code"), rs.getString("name")});
        },
        companyId);
    return names;
  }

  /**
   * First month of the company's fiscal year.
   *
   * @param companyId company
   * @return month 1-12 (1 when unknown)
   */
  public int yearStartMonth(Long companyId) {
    List<Integer> months = jdbc.queryForList(YEAR_START_MONTH, Integer.class, companyId);
    return months.isEmpty() || months.get(0) == null ? 1 : months.get(0);
  }

  /**
   * A selected account.
   *
   * @param id id
   * @param code code
   * @param name name
   */
  public record AccountRef(long id, String code, String name) {}

  /**
   * What a schedule reads.
   *
   * @param companyId company
   * @param grouping rows
   * @param accountIds accounts
   * @param currency one currency in that currency, null for every currency in base currency
   * @param branchId branch, null for all
   */
  public record Selection(
      Long companyId, Grouping grouping, List<Long> accountIds, String currency, Long branchId) {

    /** Copies the accounts. */
    public Selection {
      accountIds = List.copyOf(accountIds);
    }

    boolean foreignCurrency() {
      return currency != null;
    }

    Long[] ids() {
      return accountIds.toArray(Long[]::new);
    }
  }

  /**
   * The dates of a schedule run.
   *
   * @param from first day of the period
   * @param asOf as-of date (last day of the period)
   * @param yearStart first day of the fiscal year of the as-of date
   * @param comparativeFrom first day counted in the comparative figure
   * @param comparativeTo last day counted in the comparative figure
   */
  public record Window(
      LocalDate from,
      LocalDate asOf,
      LocalDate yearStart,
      LocalDate comparativeFrom,
      LocalDate comparativeTo) {}

  /**
   * Signed (debit minus credit) figures of one row, debits and credits as posted.
   *
   * @param key row key
   * @param opening balance before the period
   * @param debits debits of the period
   * @param credits credits of the period
   * @param yearToDate net movement of the fiscal year to date
   * @param closing balance at the as-of date
   * @param comparative net of the comparative window
   */
  public record RowFigures(
      String key,
      BigDecimal opening,
      BigDecimal debits,
      BigDecimal credits,
      BigDecimal yearToDate,
      BigDecimal closing,
      BigDecimal comparative) {}

  /**
   * Increase of a row's balance on one day.
   *
   * @param key row key
   * @param date value date
   * @param amount increase
   */
  public record Increase(String key, LocalDate date, BigDecimal amount) {}
}
