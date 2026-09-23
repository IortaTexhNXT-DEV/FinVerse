package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
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
 * Read model for receivables reports: open items with their balance as of a date (matches dated
 * after the date are ignored), cheque receipts with the invoices they settled, and cheques not yet
 * deposited. Aggregation happens in SQL; reports only lay the rows out.
 */
@Service
@Transactional(readOnly = true)
public class ReceivablesQueries {

  /** Lower bound used for "everything up to" queries. */
  public static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

  private static final String ITEMS_SQL =
      "select i.id, b.code as branch_code, i.party_id, i.party_code, p.name as party_name,"
          + " p.party_type, i.direction, i.document_type, i.document_no, i.document_date,"
          + " i.due_date, i.currency, i.amount, i.base_amount, i.narration, r.instrument_no,"
          + " r.instrument_date, i.amount - coalesce((select sum(m.amount) from sl_item_match m"
          + " where (m.debit_item_id = i.id or m.credit_item_id = i.id) and m.match_date <= ?), 0)"
          + " as balance"
          + " from sl_open_item i join pty_party p on p.id = i.party_id"
          + " join org_branch b on b.id = i.branch_id"
          + " left join rcv_receipt r on r.company_id = i.company_id and r.receipt_no = i.document_no"
          + " where i.company_id = ? and i.document_date between ? and ? and i.status <> 'WRITTEN_OFF'"
          + " order by i.party_code, i.document_date, i.id";

  private static final String RECEIPT_COLUMNS =
      "select r.id, r.receipt_no, r.receipt_date, r.party_code, r.payer_name, r.instrument_no,"
          + " r.instrument_date, r.drawee_bank, r.bank_account_code, a.name as bank_name,"
          + " r.currency, r.amount, r.base_amount, r.status";

  private static final String CHEQUE_ALLOCATIONS_SQL =
      RECEIPT_COLUMNS
          + ", al.document_no as invoice_no, d.document_date as invoice_date,"
          + " d.amount as invoice_amount, al.amount as adjusted"
          + " from rcv_receipt r left join coa_account a on a.company_id = r.company_id"
          + " and a.code = r.bank_account_code"
          + " left join rcv_receipt_allocation al on al.receipt_id = r.id"
          + " left join sl_open_item d on d.id = al.debit_item_id"
          + " where r.company_id = ? and r.mode = 'CHEQUE' and r.receipt_date between ? and ?"
          + " and r.status in ('APPROVED', 'BOUNCED', 'CANCELLED')"
          + " order by r.party_code, r.instrument_no, r.id, al.id";

  private static final String UNDEPOSITED_SQL =
      RECEIPT_COLUMNS
          + ", null as invoice_no, null as invoice_date, null as invoice_amount, null as adjusted"
          + " from rcv_receipt r left join coa_account a on a.company_id = r.company_id"
          + " and a.code = r.bank_account_code"
          + " where r.company_id = ? and r.mode = 'CHEQUE' and r.receipt_date <= ?"
          + " and coalesce(r.instrument_date, r.receipt_date) <= ?"
          + " and r.status in ('APPROVED', 'BOUNCED', 'CANCELLED')"
          + " and (r.deposited_on is null or r.deposited_on > ?)"
          + " and (r.reversal_date is null or r.reversal_date > ?)"
          + " order by r.receipt_no";

  private final JdbcTemplate jdbc;

  /**
   * Creates the query service.
   *
   * @param jdbc JDBC template
   */
  public ReceivablesQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Open items documented up to a date, with their balance as of that date.
   *
   * @param companyId company
   * @param asOf date
   * @return items (including settled ones; callers filter on the balance)
   */
  public List<ArItem> itemsAsOf(Long companyId, LocalDate asOf) {
    return items(companyId, BEGINNING, asOf, asOf);
  }

  /**
   * Items documented in a period, with their balance as of a date.
   *
   * @param companyId company
   * @param from first document date
   * @param to last document date
   * @param balanceDate matches up to this date reduce the balance
   * @return items
   */
  public List<ArItem> items(Long companyId, LocalDate from, LocalDate to, LocalDate balanceDate) {
    return jdbc.query(
        ITEMS_SQL,
        ReceivablesQueries::mapItem,
        Date.valueOf(balanceDate),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to));
  }

  /**
   * Non-PDC cheque receipts in a period with the invoices they settled (one row per allocation).
   *
   * @param companyId company
   * @param from first receipt date
   * @param to last receipt date
   * @return rows
   */
  public List<ChequeRow> chequeAllocations(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        CHEQUE_ALLOCATIONS_SQL,
        ReceivablesQueries::mapCheque,
        companyId,
        Date.valueOf(from),
        Date.valueOf(to));
  }

  /**
   * Non-PDC cheques received and not deposited as of a date.
   *
   * @param companyId company
   * @param asOf date
   * @return rows
   */
  public List<ChequeRow> undeposited(Long companyId, LocalDate asOf) {
    Date d = Date.valueOf(asOf);
    return jdbc.query(UNDEPOSITED_SQL, ReceivablesQueries::mapCheque, companyId, d, d, d, d);
  }

  private static ArItem mapItem(ResultSet rs, int row) throws SQLException {
    return new ArItem(
        rs.getLong("id"),
        rs.getString("branch_code"),
        rs.getString("party_code"),
        rs.getString("party_name"),
        PartyType.valueOf(rs.getString("party_type")),
        ItemDirection.valueOf(rs.getString("direction")),
        rs.getString("document_type"),
        rs.getString("document_no"),
        rs.getDate("document_date").toLocalDate(),
        rs.getDate("due_date").toLocalDate(),
        rs.getString("currency"),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("base_amount"),
        rs.getBigDecimal("balance"),
        rs.getString("narration"),
        rs.getString("instrument_no"),
        toLocalDate(rs.getDate("instrument_date")));
  }

  private static ChequeRow mapCheque(ResultSet rs, int row) throws SQLException {
    return new ChequeRow(
        rs.getLong("id"),
        rs.getString("receipt_no"),
        rs.getDate("receipt_date").toLocalDate(),
        rs.getString("party_code"),
        rs.getString("payer_name"),
        rs.getString("instrument_no"),
        toLocalDate(rs.getDate("instrument_date")),
        rs.getString("drawee_bank"),
        rs.getString("bank_account_code"),
        rs.getString("bank_name"),
        rs.getString("currency"),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("base_amount"),
        rs.getString("status"),
        rs.getString("invoice_no"),
        toLocalDate(rs.getDate("invoice_date")),
        rs.getBigDecimal("invoice_amount"),
        rs.getBigDecimal("adjusted"));
  }

  private static LocalDate toLocalDate(Date d) {
    return d == null ? null : d.toLocalDate();
  }

  /**
   * Open item with its balance as of a date.
   *
   * @param id item id
   * @param branchCode branch (division)
   * @param partyCode party
   * @param partyName party name
   * @param partyType party type
   * @param direction DEBIT (owed to the company) or CREDIT
   * @param documentType document type
   * @param documentNo document number
   * @param documentDate document date
   * @param dueDate due date
   * @param currency currency
   * @param amount original amount
   * @param baseAmount original base amount
   * @param balance balance as of the date (item currency)
   * @param narration narration
   * @param chequeNo cheque number (receipts)
   * @param chequeDate cheque date (receipts)
   */
  public record ArItem(
      Long id,
      String branchCode,
      String partyCode,
      String partyName,
      PartyType partyType,
      ItemDirection direction,
      String documentType,
      String documentNo,
      LocalDate documentDate,
      LocalDate dueDate,
      String currency,
      BigDecimal amount,
      BigDecimal baseAmount,
      BigDecimal balance,
      String narration,
      String chequeNo,
      LocalDate chequeDate) {

    /**
     * Sign of the item from the company's point of view.
     *
     * @return 1 for DEBIT, -1 for CREDIT
     */
    public int sign() {
      return direction == ItemDirection.DEBIT ? 1 : -1;
    }

    /**
     * Signed balance in item currency.
     *
     * @return balance (CREDIT negative)
     */
    public BigDecimal signedBalance() {
      return sign() > 0 ? balance : balance.negate();
    }

    /**
     * Signed balance in base currency (pro-rated with the historical rate of the document).
     *
     * @return base balance (CREDIT negative)
     */
    public BigDecimal signedBaseBalance() {
      BigDecimal base =
          amount.signum() == 0
              ? BigDecimal.ZERO
              : baseAmount.multiply(balance).divide(amount, Money.SCALE, Money.ROUNDING);
      return sign() > 0 ? base : base.negate();
    }

    /**
     * Signed original amount in item currency or base currency.
     *
     * @param foreign true for item currency
     * @return signed amount
     */
    public BigDecimal signedOriginal(boolean foreign) {
      BigDecimal value = foreign ? amount : baseAmount;
      return sign() > 0 ? value : value.negate();
    }

    /**
     * Signed balance in item currency or base currency.
     *
     * @param foreign true for item currency
     * @return signed balance
     */
    public BigDecimal signedBalance(boolean foreign) {
      return foreign ? signedBalance() : signedBaseBalance();
    }
  }

  /**
   * Cheque receipt row (with one settled invoice, when any).
   *
   * @param receiptId receipt id
   * @param receiptNo receipt number
   * @param receiptDate receipt date
   * @param partyCode payer party
   * @param payerName payer name
   * @param chequeNo cheque number
   * @param chequeDate cheque date
   * @param draweeBank customer's bank
   * @param bankAccountCode company bank GL account
   * @param bankName company bank account name
   * @param currency currency
   * @param amount cheque amount
   * @param baseAmount cheque amount in base currency
   * @param status receipt status
   * @param invoiceNo invoice (debit note) number
   * @param invoiceDate invoice date
   * @param invoiceAmount invoice amount
   * @param adjusted amount of this receipt knocked off against the invoice
   */
  public record ChequeRow(
      Long receiptId,
      String receiptNo,
      LocalDate receiptDate,
      String partyCode,
      String payerName,
      String chequeNo,
      LocalDate chequeDate,
      String draweeBank,
      String bankAccountCode,
      String bankName,
      String currency,
      BigDecimal amount,
      BigDecimal baseAmount,
      String status,
      String invoiceNo,
      LocalDate invoiceDate,
      BigDecimal invoiceAmount,
      BigDecimal adjusted) {}
}
