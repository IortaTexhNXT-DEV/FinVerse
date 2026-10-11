package com.iortatechnxt.brokerverse.receivables.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Book side of the bank reconciliation: posted ledger entries of a bank GL account and whether they
 * are reconciled. Amounts are in the transaction currency of the bank account (bank accounts are
 * single-currency), debit = money in.
 */
@Service
@Transactional(readOnly = true)
public class BankBookQueries {

  private static final String COLUMNS =
      "select e.id, e.value_date, e.batch_no, e.journal_type, e.reference, e.narration,"
          + " e.currency, e.debit_fc, e.credit_fc from gl_ledger_entry e";

  private static final String ORDER = " order by e.value_date, e.id";

  private static final String UNRECONCILED_SQL =
      COLUMNS
          + " where e.company_id = ? and e.account_id = ? and e.value_date <= ?"
          + " and not exists (select 1 from brs_match_book b join brs_match m on m.id = b.match_id"
          + " where b.ledger_entry_id = e.id and m.match_date <= ?)"
          + ORDER;

  private static final String UNMATCHED_SQL =
      COLUMNS
          + " where e.company_id = ? and e.account_id = ? and e.value_date <= ?"
          + " and not exists (select 1 from brs_match_book b where b.ledger_entry_id = e.id)"
          + ORDER;

  private static final String BY_IDS_SQL =
      COLUMNS
          + " where e.company_id = ? and e.account_id = ? and e.id = any (?)"
          + " and not exists (select 1 from brs_match_book b where b.ledger_entry_id = e.id)"
          + ORDER;

  private static final String BY_MATCH_SQL =
      COLUMNS + " join brs_match_book b on b.ledger_entry_id = e.id where b.match_id = ?" + ORDER;

  private static final String BALANCE_SQL =
      "select coalesce(sum(debit_fc - credit_fc), 0) from gl_ledger_entry"
          + " where company_id = ? and account_id = ? and value_date <= ?";

  private final JdbcTemplate jdbc;

  /**
   * Creates the query service.
   *
   * @param jdbc JDBC template
   */
  public BankBookQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Book entries dated on or before a date and not reconciled as of that date.
   *
   * @param companyId company
   * @param accountId bank GL account id
   * @param asOf date
   * @return entries by value date
   */
  public List<BookEntry> unreconciled(Long companyId, Long accountId, LocalDate asOf) {
    Date date = Date.valueOf(asOf);
    return jdbc.query(UNRECONCILED_SQL, (rs, i) -> map(rs), companyId, accountId, date, date);
  }

  /**
   * Book entries dated on or before a date that were never matched (matching workbench).
   *
   * @param companyId company
   * @param accountId bank GL account id
   * @param asOf date
   * @return entries by value date
   */
  public List<BookEntry> unmatched(Long companyId, Long accountId, LocalDate asOf) {
    return jdbc.query(UNMATCHED_SQL, (rs, i) -> map(rs), companyId, accountId, Date.valueOf(asOf));
  }

  /**
   * Unreconciled entries of an account among the given ids.
   *
   * @param companyId company
   * @param accountId bank GL account id
   * @param ids ledger entry ids
   * @return entries found (unknown, foreign or reconciled ids are omitted)
   */
  public List<BookEntry> unreconciledByIds(Long companyId, Long accountId, List<Long> ids) {
    return jdbc.query(
        BY_IDS_SQL, (rs, i) -> map(rs), companyId, accountId, ids.toArray(new Long[0]));
  }

  /**
   * Book entries of a reconciliation match.
   *
   * @param matchId match
   * @return entries
   */
  public List<BookEntry> ofMatch(Long matchId) {
    return jdbc.query(BY_MATCH_SQL, (rs, i) -> map(rs), matchId);
  }

  /**
   * Book balance (debit positive, account currency) as of a date.
   *
   * @param companyId company
   * @param accountId bank GL account id
   * @param asOf date
   * @return balance
   */
  public BigDecimal balance(Long companyId, Long accountId, LocalDate asOf) {
    return jdbc.queryForObject(
        BALANCE_SQL, BigDecimal.class, companyId, accountId, Date.valueOf(asOf));
  }

  private static BookEntry map(ResultSet rs) throws SQLException {
    return new BookEntry(
        rs.getLong("id"),
        rs.getDate("value_date").toLocalDate(),
        rs.getString("batch_no"),
        rs.getString("journal_type"),
        rs.getString("reference"),
        rs.getString("narration"),
        rs.getString("currency"),
        rs.getBigDecimal("debit_fc"),
        rs.getBigDecimal("credit_fc"));
  }

  /**
   * Posted ledger entry of a bank account.
   *
   * @param id ledger entry id
   * @param valueDate value date
   * @param batchNo journal batch
   * @param journalType journal type
   * @param reference business reference (receipt / payment number)
   * @param narration narration
   * @param currency currency
   * @param debit money in
   * @param credit money out
   */
  public record BookEntry(
      Long id,
      LocalDate valueDate,
      String batchNo,
      String journalType,
      String reference,
      String narration,
      String currency,
      BigDecimal debit,
      BigDecimal credit) {

    /**
     * Signed amount (money in positive).
     *
     * @return debit minus credit
     */
    public BigDecimal signedAmount() {
      return debit.subtract(credit);
    }
  }
}
