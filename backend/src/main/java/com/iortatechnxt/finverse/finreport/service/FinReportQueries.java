package com.iortatechnxt.finverse.finreport.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SQL read model of the finance reports. Every balance is aggregated in the database (from {@code
 * gl_daily_balance} when no line dimension is needed, else from {@code gl_ledger_entry}); only
 * detail listings return individual lines, always restricted to a date range.
 *
 * <p>All statements are constants with bind parameters; optional filters use the {@code (cast(? as
 * type) is null or column = ?)} idiom.
 */
@Service
@Transactional(readOnly = true)
public class FinReportQueries {

  private static final String DAILY_MOVEMENTS =
      "select account_id, branch_id, cast(null as varchar) cost_center,"
          + " cast(null as varchar) business_line, cast(null as varchar) party_code, currency,"
          + " coalesce(sum(debit_base - credit_base) filter (where balance_date < ?), 0) open_base,"
          + " coalesce(sum(debit_fc - credit_fc) filter (where balance_date < ?), 0) open_fc,"
          + " coalesce(sum(debit_base) filter (where balance_date >= ?), 0) dr_base,"
          + " coalesce(sum(credit_base) filter (where balance_date >= ?), 0) cr_base,"
          + " coalesce(sum(debit_fc) filter (where balance_date >= ?), 0) dr_fc,"
          + " coalesce(sum(credit_fc) filter (where balance_date >= ?), 0) cr_fc"
          + " from gl_daily_balance"
          + " where company_id = ? and balance_date <= ? and account_id = any(?)"
          + " and (cast(? as bigint) is null or branch_id = ?)"
          + " group by account_id, branch_id, currency";

  private static final String LEDGER_SUMS =
      " coalesce(sum(debit_base - credit_base) filter (where value_date < ?), 0) open_base,"
          + " coalesce(sum(debit_fc - credit_fc) filter (where value_date < ?), 0) open_fc,"
          + " coalesce(sum(debit_base) filter (where value_date >= ?), 0) dr_base,"
          + " coalesce(sum(credit_base) filter (where value_date >= ?), 0) cr_base,"
          + " coalesce(sum(debit_fc) filter (where value_date >= ?), 0) dr_fc,"
          + " coalesce(sum(credit_fc) filter (where value_date >= ?), 0) cr_fc"
          + " from gl_ledger_entry"
          + " where company_id = ? and value_date <= ? and account_id = any(?)"
          + " and (cast(? as bigint) is null or branch_id = ?)";

  private static final String DIMENSION_MOVEMENTS =
      "select account_id, branch_id, cost_center, business_line,"
          + " cast(null as varchar) party_code, currency,"
          + LEDGER_SUMS
          + " and (cast(? as varchar) is null or cost_center = ?)"
          + " group by account_id, branch_id, cost_center, business_line, currency";

  private static final String PARTY_MOVEMENTS =
      "select account_id, cast(null as bigint) branch_id, cast(null as varchar) cost_center,"
          + " cast(null as varchar) business_line, party_code, currency,"
          + LEDGER_SUMS
          + " and party_code is not null"
          + " and (cast(? as varchar) is null or party_code >= ?)"
          + " and (cast(? as varchar) is null or party_code <= ?)"
          + " group by account_id, party_code, currency";

  private static final String UNPOSTED_MOVEMENTS =
      "select l.account_id, l.branch_id, cast(null as varchar) cost_center,"
          + " cast(null as varchar) business_line, cast(null as varchar) party_code, l.currency,"
          + " coalesce(sum(case when l.side = 'DEBIT' then l.base_amount else -l.base_amount end)"
          + " filter (where b.value_date < ?), 0) open_base,"
          + " coalesce(sum(case when l.side = 'DEBIT' then l.amount else -l.amount end)"
          + " filter (where b.value_date < ?), 0) open_fc,"
          + " coalesce(sum(l.base_amount) filter (where l.side = 'DEBIT' and b.value_date >= ?), 0)"
          + " dr_base,"
          + " coalesce(sum(l.base_amount) filter (where l.side = 'CREDIT' and b.value_date >= ?), 0)"
          + " cr_base,"
          + " coalesce(sum(l.amount) filter (where l.side = 'DEBIT' and b.value_date >= ?), 0) dr_fc,"
          + " coalesce(sum(l.amount) filter (where l.side = 'CREDIT' and b.value_date >= ?), 0) cr_fc"
          + " from jnl_line l join jnl_batch b on b.id = l.batch_id"
          + " where b.company_id = ? and b.value_date <= ? and l.account_id = any(?)"
          + " and b.status in ('DRAFT', 'PENDING_APPROVAL')"
          + " and (cast(? as bigint) is null or l.branch_id = ?)"
          + " group by l.account_id, l.branch_id, l.currency";

  private static final String VOUCHER_LINES =
      "select b.id batch_id, b.batch_no, b.journal_type, b.status, b.value_date, b.narration,"
          + " b.reference, b.source_module, b.created_by, b.created_at, b.submitted_by,"
          + " b.authorized_by, b.authorized_at, l.line_no, l.account_id, l.branch_id, l.side,"
          + " l.currency, l.amount, l.base_amount, l.cost_center, l.business_line, l.party_code,"
          + " l.reference line_reference, l.narration line_narration"
          + " from jnl_batch b join jnl_line l on l.batch_id = b.id"
          + " where b.company_id = ? and b.value_date between ? and ?"
          + " and b.status = any(?) and b.journal_type = any(?) and l.account_id = any(?)"
          + " and (cast(? as bigint) is null or b.branch_id = ?)"
          + " and (cast(? as varchar) is null or b.batch_no >= ?)"
          + " and (cast(? as varchar) is null or b.batch_no <= ?)"
          + " and (cast(? as varchar) is null or b.created_by = ?)"
          + " order by b.value_date, b.batch_no, l.line_no";

  private static final String LEDGER_LINES =
      "select account_id, branch_id, value_date, batch_no, journal_type, currency, debit_fc,"
          + " credit_fc, debit_base, credit_base, cost_center, business_line, party_code,"
          + " reference, narration"
          + " from gl_ledger_entry"
          + " where company_id = ? and value_date between ? and ? and account_id = any(?)"
          + " and (cast(? as bigint) is null or branch_id = ?)"
          + " and (cast(? as varchar) is null or cost_center = ?)"
          + " and (cast(? as varchar) is null or party_code >= ?)"
          + " and (cast(? as varchar) is null or party_code <= ?)"
          + " and (? = false or party_code is not null)"
          + " order by value_date, batch_no, line_no";

  private static final String DAILY_AMOUNTS =
      "select l.account_id, b.value_date,"
          + " coalesce(sum(l.base_amount) filter (where l.side = 'DEBIT'), 0) dr_base,"
          + " coalesce(sum(l.base_amount) filter (where l.side = 'CREDIT'), 0) cr_base"
          + " from jnl_batch b join jnl_line l on l.batch_id = b.id"
          + " where b.company_id = ? and b.value_date between ? and ?"
          + " and b.status = any(?) and b.journal_type = any(?) and l.account_id = any(?)"
          + " and l.base_amount > ?"
          + " and (cast(? as bigint) is null or b.branch_id = ?)"
          + " group by l.account_id, b.value_date";

  private static final String BATCH_NUMBERS =
      "select batch_no from jnl_batch"
          + " where company_id = ? and value_date between ? and ? and journal_type = any(?)";

  private static final String PARTY_NAMES =
      "select code, name from pty_party where company_id = ? and code = any(?)";

  private static final String DIMENSION_NAMES =
      "select code, name from dim_value where company_id = ? and dimension_type = ?";

  private static final String CURRENCY_NAME = "select name from cur_currency where code = ?";

  private static final String CODE = "code";
  private static final String ACCOUNT_ID = "account_id";
  private static final String NAME = "name";

  private final JdbcTemplate jdbc;

  /**
   * Creates the query service.
   *
   * @param jdbc JDBC template
   */
  public FinReportQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Opening balances and period movements per account, branch and currency, from the daily balance
   * table.
   *
   * @param q selection (cost centre and party filters are ignored)
   * @return rows
   */
  public List<MovementRow> movements(LedgerQuery q) {
    Date from = Date.valueOf(q.from());
    return jdbc.query(
        DAILY_MOVEMENTS,
        (rs, i) -> movement(rs),
        from,
        from,
        from,
        from,
        from,
        from,
        q.companyId(),
        Date.valueOf(q.to()),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId());
  }

  /**
   * Opening balances and period movements per account, branch, cost centre, line of business and
   * currency, from the ledger (needed when a line dimension is reported or filtered).
   *
   * @param q selection
   * @return rows
   */
  public List<MovementRow> dimensionMovements(LedgerQuery q) {
    Date from = Date.valueOf(q.from());
    return jdbc.query(
        DIMENSION_MOVEMENTS,
        (rs, i) -> movement(rs),
        from,
        from,
        from,
        from,
        from,
        from,
        q.companyId(),
        Date.valueOf(q.to()),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId(),
        q.costCenter(),
        q.costCenter());
  }

  /**
   * Opening balances and period movements per account, party and currency (sub-ledger postings).
   *
   * @param q selection
   * @return rows
   */
  public List<MovementRow> partyMovements(LedgerQuery q) {
    Date from = Date.valueOf(q.from());
    return jdbc.query(
        PARTY_MOVEMENTS,
        (rs, i) -> movement(rs),
        from,
        from,
        from,
        from,
        from,
        from,
        q.companyId(),
        Date.valueOf(q.to()),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId(),
        q.partyFrom(),
        q.partyFrom(),
        q.partyTo(),
        q.partyTo());
  }

  /**
   * Opening balances and period movements of unposted (draft and pending) journals.
   *
   * @param q selection (cost centre and party filters are ignored)
   * @return rows
   */
  public List<MovementRow> unpostedMovements(LedgerQuery q) {
    Date from = Date.valueOf(q.from());
    return jdbc.query(
        UNPOSTED_MOVEMENTS,
        (rs, i) -> movement(rs),
        from,
        from,
        from,
        from,
        from,
        from,
        q.companyId(),
        Date.valueOf(q.to()),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId());
  }

  /**
   * Journal lines with their headers, ordered by date, voucher number and line.
   *
   * @param q selection
   * @return lines
   */
  public List<VoucherLine> voucherLines(VoucherQuery q) {
    return jdbc.query(
        VOUCHER_LINES,
        (rs, i) -> voucherLine(rs),
        q.companyId(),
        Date.valueOf(q.from()),
        Date.valueOf(q.to()),
        q.status().statuses().toArray(String[]::new),
        q.journalTypes().toArray(String[]::new),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId(),
        q.docFrom(),
        q.docFrom(),
        q.docTo(),
        q.docTo(),
        q.user(),
        q.user());
  }

  /**
   * Posted ledger entries in a period, ordered by date and voucher.
   *
   * @param q selection
   * @param partyOnly true to read only entries carrying a party code
   * @return entries
   */
  public List<LedgerLine> ledgerLines(LedgerQuery q, boolean partyOnly) {
    return jdbc.query(
        LEDGER_LINES,
        (rs, i) -> ledgerLine(rs),
        q.companyId(),
        Date.valueOf(q.from()),
        Date.valueOf(q.to()),
        ids(q.accountIds()),
        q.branchId(),
        q.branchId(),
        q.costCenter(),
        q.costCenter(),
        q.partyFrom(),
        q.partyFrom(),
        q.partyTo(),
        q.partyTo(),
        partyOnly);
  }

  /**
   * Debits and credits per account and document date of the selected journals, counting only lines
   * above an amount limit.
   *
   * @param q selection (document and user filters are ignored)
   * @param amountOver lines with a base amount above this value are counted
   * @return daily amounts
   */
  public List<DailyAmount> dailyAmounts(VoucherQuery q, BigDecimal amountOver) {
    return jdbc.query(
        DAILY_AMOUNTS,
        (rs, i) ->
            new DailyAmount(
                rs.getLong(ACCOUNT_ID),
                rs.getObject("value_date", LocalDate.class),
                rs.getBigDecimal("dr_base"),
                rs.getBigDecimal("cr_base")),
        q.companyId(),
        Date.valueOf(q.from()),
        Date.valueOf(q.to()),
        q.status().statuses().toArray(String[]::new),
        q.journalTypes().toArray(String[]::new),
        ids(q.accountIds()),
        amountOver,
        q.branchId(),
        q.branchId());
  }

  /**
   * Voucher numbers of every journal (any status) dated within a period.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @param journalTypes journal types
   * @return voucher numbers
   */
  public List<String> batchNumbers(
      Long companyId, LocalDate from, LocalDate to, Collection<String> journalTypes) {
    return jdbc.queryForList(
        BATCH_NUMBERS,
        String.class,
        companyId,
        Date.valueOf(from),
        Date.valueOf(to),
        journalTypes.toArray(String[]::new));
  }

  /**
   * Names of the given parties.
   *
   * @param companyId company
   * @param codes party codes
   * @return name by code
   */
  public Map<String, String> partyNames(Long companyId, Collection<String> codes) {
    Map<String, String> names = new HashMap<>();
    jdbc.query(
        PARTY_NAMES,
        rs -> {
          names.put(rs.getString(CODE), rs.getString(NAME));
        },
        companyId,
        codes.toArray(String[]::new));
    return names;
  }

  /**
   * Names of the values of a dimension.
   *
   * @param companyId company
   * @param dimensionType dimension type name
   * @return name by code
   */
  public Map<String, String> dimensionNames(Long companyId, String dimensionType) {
    Map<String, String> names = new HashMap<>();
    jdbc.query(
        DIMENSION_NAMES,
        rs -> {
          names.put(rs.getString(CODE), rs.getString(NAME));
        },
        companyId,
        dimensionType);
    return names;
  }

  /**
   * Currency name for amounts in words.
   *
   * @param code ISO code
   * @return name, or the code when unknown
   */
  public String currencyName(String code) {
    return jdbc.queryForList(CURRENCY_NAME, String.class, code).stream().findFirst().orElse(code);
  }

  /**
   * Debits and credits of an account on a document date.
   *
   * @param accountId account
   * @param date document date
   * @param debit debits in base currency
   * @param credit credits in base currency
   */
  public record DailyAmount(Long accountId, LocalDate date, BigDecimal debit, BigDecimal credit) {}

  private static Long[] ids(Collection<Long> ids) {
    return ids.toArray(Long[]::new);
  }

  private static MovementRow movement(ResultSet rs) throws SQLException {
    return new MovementRow(
        rs.getLong(ACCOUNT_ID),
        rs.getObject("branch_id", Long.class),
        rs.getString("cost_center"),
        rs.getString("business_line"),
        rs.getString("party_code"),
        rs.getString("currency"),
        rs.getBigDecimal("open_base"),
        rs.getBigDecimal("open_fc"),
        rs.getBigDecimal("dr_base"),
        rs.getBigDecimal("cr_base"),
        rs.getBigDecimal("dr_fc"),
        rs.getBigDecimal("cr_fc"));
  }

  private static VoucherLine voucherLine(ResultSet rs) throws SQLException {
    return new VoucherLine(
        rs.getLong("batch_id"),
        rs.getString("batch_no"),
        rs.getString("journal_type"),
        rs.getString("status"),
        rs.getObject("value_date", LocalDate.class),
        rs.getString("narration"),
        rs.getString("reference"),
        rs.getString("source_module"),
        rs.getString("created_by"),
        instant(rs.getTimestamp("created_at")),
        rs.getString("submitted_by"),
        rs.getString("authorized_by"),
        instant(rs.getTimestamp("authorized_at")),
        rs.getInt("line_no"),
        rs.getLong(ACCOUNT_ID),
        rs.getLong("branch_id"),
        "DEBIT".equals(rs.getString("side")),
        rs.getString("currency"),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("base_amount"),
        rs.getString("cost_center"),
        rs.getString("business_line"),
        rs.getString("party_code"),
        rs.getString("line_reference"),
        rs.getString("line_narration"));
  }

  private static LedgerLine ledgerLine(ResultSet rs) throws SQLException {
    return new LedgerLine(
        rs.getLong(ACCOUNT_ID),
        rs.getLong("branch_id"),
        rs.getObject("value_date", LocalDate.class),
        rs.getString("batch_no"),
        rs.getString("journal_type"),
        rs.getString("currency"),
        rs.getBigDecimal("debit_fc"),
        rs.getBigDecimal("credit_fc"),
        rs.getBigDecimal("debit_base"),
        rs.getBigDecimal("credit_base"),
        rs.getString("cost_center"),
        rs.getString("business_line"),
        rs.getString("party_code"),
        rs.getString("reference"),
        rs.getString("narration"));
  }

  private static Instant instant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
