package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SQL read model of the new BIR outputs (FRBS 3.2.0, Appendix A VII; V703): the definitions of the
 * books of accounts and the form worksheets, the posted journal lines of a book, the general ledger
 * of an account class, ledger movements of account prefixes and the broker's production of a year.
 * Every statement is a constant with bind parameters.
 */
@Service
@Transactional(readOnly = true)
public class BirOutputQueries {

  private static final String BOOK =
      "select code, name, layout, journal_types, event_types, exclude_event_types, description"
          + " from tax_book_def where code = ?";

  private static final String JOURNAL =
      "select e.value_date, e.batch_no, e.journal_type, coalesce(e.reference, b.reference) ref,"
          + " coalesce(e.narration, b.narration) narration, a.code account, a.name account_name,"
          + " e.party_code, e.debit_base debit, e.credit_base credit"
          + " from gl_ledger_entry e join jnl_batch b on b.id = e.batch_id"
          + " join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and e.value_date between ? and ?"
          + " and (cardinality(cast(? as varchar[])) = 0 or e.journal_type = any(cast(? as varchar[])))"
          + " and (cardinality(cast(? as varchar[])) = 0 or exists (select 1 from acc_event_log g"
          + " where g.company_id = e.company_id and g.batch_no = e.batch_no"
          + " and g.event_type = any(cast(? as varchar[]))))"
          + " and (cardinality(cast(? as varchar[])) = 0 or not exists (select 1 from acc_event_log g"
          + " where g.company_id = e.company_id and g.batch_no = e.batch_no"
          + " and g.event_type = any(cast(? as varchar[]))))"
          + " order by e.value_date, e.batch_no, e.line_no";

  private static final String LEDGER =
      "select a.code account, a.name account_name, 0 seq, cast(? as date) value_date,"
          + " cast(null as varchar) batch_no, 'Balance forward' narration,"
          + " greatest(sum(e.debit_base - e.credit_base), 0) debit,"
          + " greatest(sum(e.credit_base - e.debit_base), 0) credit"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and a.account_class = ? and e.value_date < ?"
          + " group by a.code, a.name having sum(e.debit_base - e.credit_base) <> 0"
          + " union all"
          + " select a.code, a.name, 1, e.value_date, e.batch_no, e.narration, e.debit_base,"
          + " e.credit_base"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and a.account_class = ? and e.value_date between ? and ?"
          + " order by 1, 3, 4, 5";

  private static final String FORM_LINES =
      "select line_no, label, kind, selector, operand from tax_form_output_line"
          + " where form_code = ? order by line_no";

  private static final String MOVEMENT =
      "select coalesce(sum(e.debit_base), 0) debit, coalesce(sum(e.credit_base), 0) credit"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and e.value_date between ? and ? and a.code like any(?)";

  private static final String PRODUCTION =
      "select coalesce(i.product_line, '(none)') line, i.currency, count(*) policies,"
          + " count(distinct i.insurer_code) insurers, sum(i.gross_premium) premium,"
          + " sum(i.commission) commission, sum(i.vat_on_commission) commission_vat"
          + " from ops_invoice i where i.company_id = ? and not i.cancelled"
          + " and i.booking_date between ? and ?"
          + " group by 1, i.currency order by 1, i.currency";

  private static final String DEBIT = "debit";
  private static final String CREDIT = "credit";

  private final JdbcTemplate jdbc;

  /**
   * Creates the queries.
   *
   * @param jdbc JDBC template
   */
  public BirOutputQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * A book of accounts.
   *
   * @param code book code
   * @return definition
   */
  public BookDef book(String code) {
    List<BookDef> books =
        jdbc.query(
            BOOK,
            (rs, i) ->
                new BookDef(
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("layout"),
                    list(rs.getString("journal_types")),
                    list(rs.getString("event_types")),
                    list(rs.getString("exclude_event_types")),
                    rs.getString("description")),
            code);
    if (books.isEmpty()) {
      throw new ResourceNotFoundException("Book of accounts", code);
    }
    return books.get(0);
  }

  /**
   * The posted lines of a journal book.
   *
   * @param companyId company
   * @param book book
   * @param from first day
   * @param to last day
   * @return rows
   */
  public List<Map<String, Object>> journal(
      Long companyId, BookDef book, LocalDate from, LocalDate to) {
    String[] types = book.journalTypes().toArray(String[]::new);
    String[] events = book.eventTypes().toArray(String[]::new);
    String[] excluded = book.excludeEventTypes().toArray(String[]::new);
    return jdbc.query(
        JOURNAL,
        (rs, i) -> row(rs),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to),
        types,
        types,
        events,
        events,
        excluded,
        excluded);
  }

  /**
   * The general ledger of an account class: the balance brought forward and the lines of the
   * period, per account.
   *
   * @param companyId company
   * @param accountClass ASSET, LIABILITY, EQUITY, INCOME, EXPENSE or MEMORANDUM
   * @param from first day
   * @param to last day
   * @return rows
   */
  public List<Map<String, Object>> ledger(
      Long companyId, String accountClass, LocalDate from, LocalDate to) {
    Date start = Date.valueOf(from);
    return jdbc.query(
        LEDGER,
        (rs, i) -> row(rs),
        start,
        companyId,
        accountClass,
        start,
        companyId,
        accountClass,
        start,
        Date.valueOf(to));
  }

  /**
   * The lines of a form worksheet.
   *
   * @param formCode form
   * @return lines in order
   */
  public List<FormLineDef> formLines(String formCode) {
    return jdbc.query(
        FORM_LINES,
        (rs, i) ->
            new FormLineDef(
                rs.getInt("line_no"),
                rs.getString("label"),
                rs.getString("kind"),
                list(rs.getString("selector")),
                rs.getString("operand")),
        formCode);
  }

  /**
   * Debits and credits of the accounts with the given prefixes in a period.
   *
   * @param companyId company
   * @param prefixes account code prefixes
   * @param from first day
   * @param to last day
   * @return debits and credits in base currency
   */
  public Movement movement(Long companyId, List<String> prefixes, LocalDate from, LocalDate to) {
    String[] patterns = prefixes.stream().map(p -> p + "%").toArray(String[]::new);
    return jdbc.queryForObject(
        MOVEMENT,
        (rs, i) -> new Movement(rs.getBigDecimal(DEBIT), rs.getBigDecimal(CREDIT)),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to),
        patterns);
  }

  /**
   * Booked production per line of business and currency.
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @return rows
   */
  public List<Map<String, Object>> production(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        PRODUCTION, (rs, i) -> row(rs), companyId, Date.valueOf(from), Date.valueOf(to));
  }

  private static Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    int n = rs.getMetaData().getColumnCount();
    for (int c = 1; c <= n; c++) {
      Object v = rs.getObject(c);
      row.put(rs.getMetaData().getColumnLabel(c), v instanceof Date d ? d.toLocalDate() : v);
    }
    return row;
  }

  private static List<String> list(String csv) {
    List<String> out = new ArrayList<>();
    if (csv != null) {
      for (String s : csv.split(",")) {
        if (!s.isBlank()) {
          out.add(s.trim());
        }
      }
    }
    return out;
  }

  /**
   * A book of accounts (V703).
   *
   * @param code code
   * @param name name
   * @param layout JOURNAL or LEDGER
   * @param journalTypes journal types, empty for all
   * @param eventTypes accounting event types, empty for all
   * @param excludeEventTypes accounting event types left out
   * @param description description
   */
  public record BookDef(
      String code,
      String name,
      String layout,
      List<String> journalTypes,
      List<String> eventTypes,
      List<String> excludeEventTypes,
      String description) {}

  /**
   * A line of a form worksheet (V703).
   *
   * @param lineNo line number
   * @param label label
   * @param kind ACCOUNT_CREDITS, ACCOUNT_DEBITS, LINES, RATE or CERTIFICATES
   * @param selector account prefixes
   * @param operand lines ("10,-20") or rate ("50:TAX_RCIT_RATE")
   */
  public record FormLineDef(
      int lineNo, String label, String kind, List<String> selector, String operand) {}

  /**
   * Ledger movement.
   *
   * @param debit debits
   * @param credit credits
   */
  public record Movement(BigDecimal debit, BigDecimal credit) {}
}
