package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collection figures for dashboards (query service of the receivables module): approved receipts
 * per month and the debtors' outstanding debit items by due date. Constant SQL with bind
 * parameters, aggregated in the database; amounts in base currency.
 */
@Service
@Transactional(readOnly = true)
public class CollectionQueries {

  private static final String MONTHLY_RECEIPTS =
      "select to_char(r.receipt_date, 'YYYY-MM') as month, coalesce(sum(r.base_amount), 0) as total"
          + " from rcv_receipt r"
          + " where r.company_id = ? and r.receipt_date between ? and ? and r.status = 'APPROVED'"
          + " and (cast(? as bigint) is null or r.branch_id = ?)"
          + " group by to_char(r.receipt_date, 'YYYY-MM') order by 1";

  private static final String DEBTOR_OUTSTANDING_BY_DUE_DATE =
      "select o.due_date,"
          + " coalesce(sum(o.base_amount * (o.amount - o.settled_amount) / o.amount), 0) as amount"
          + " from sl_open_item o join pty_party p on p.id = o.party_id"
          + " where o.company_id = ? and o.direction = 'DEBIT'"
          + " and o.status in ('OPEN', 'PARTIALLY_SETTLED') and p.party_type = any(?)"
          + " and (cast(? as bigint) is null or o.branch_id = ?)"
          + " group by o.due_date order by o.due_date";

  private final JdbcTemplate jdbc;

  /**
   * Creates the query service.
   *
   * @param jdbc JDBC template
   */
  public CollectionQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Approved receipts per month (receipts cancelled or bounced later are excluded).
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param from first receipt date
   * @param to last receipt date
   * @return totals per month with receipts, in month order
   */
  public List<MonthlyAmount> monthlyCollections(
      Long companyId, Long branchId, LocalDate from, LocalDate to) {
    return jdbc.query(
        MONTHLY_RECEIPTS,
        (rs, i) ->
            new MonthlyAmount(YearMonth.parse(rs.getString("month")), rs.getBigDecimal("total")),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to),
        branchId,
        branchId);
  }

  /**
   * Current outstanding debit items of debtors (policyholders, intermediaries, reinsurers) summed
   * per due date, for ageing.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @return outstanding amount per due date, earliest first
   */
  public List<DueAmount> debtorOutstandingByDueDate(Long companyId, Long branchId) {
    return jdbc.query(
        DEBTOR_OUTSTANDING_BY_DUE_DATE,
        (rs, i) -> new DueAmount(rs.getDate("due_date").toLocalDate(), rs.getBigDecimal("amount")),
        companyId,
        PayerType.debtorPartyTypes().stream().map(PartyType::name).toArray(String[]::new),
        branchId,
        branchId);
  }

  /**
   * Amount of one month.
   *
   * @param month month
   * @param amount base currency amount
   */
  public record MonthlyAmount(YearMonth month, BigDecimal amount) {}

  /**
   * Outstanding amount falling due on one date.
   *
   * @param dueDate due date
   * @param amount base currency outstanding
   */
  public record DueAmount(LocalDate dueDate, BigDecimal amount) {}
}
