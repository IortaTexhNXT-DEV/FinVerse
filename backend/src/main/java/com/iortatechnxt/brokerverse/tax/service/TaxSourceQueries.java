package com.iortatechnxt.brokerverse.tax.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SQL read model of the tax module over other modules' tables (the finance-report pattern):
 * approved supplier invoices, party facts, and ledger movements and balances. Every statement is a
 * constant with bind parameters; nothing is concatenated from input.
 *
 * <p>Ledger movements used for reconciliation exclude journals posted by the tax module itself
 * (remittances) and year-end closing journals, so a period's figure is the tax booked in it.
 */
@Service
@Transactional(readOnly = true)
public class TaxSourceQueries {

  /** Source module of journals posted by the tax module. */
  public static final String MODULE = "TAX";

  private static final String SUPPLIER_INVOICES =
      "select i.id, i.document_no, i.supplier_invoice_no, i.invoice_date, i.party_code,"
          + " p.name party_name, i.net_amount, i.vat_amount, i.wht_amount, i.payable_amount,"
          + " coalesce(i.base_payable_amount, i.payable_amount) base_payable,"
          + " exists (select 1 from pay_supplier_invoice_line l join coa_account a"
          + "   on a.company_id = i.company_id and a.code = l.expense_account_code"
          + "   where l.invoice_id = i.id and a.account_class = 'ASSET') capital_goods"
          + " from pay_supplier_invoice i join pty_party p on p.id = i.party_id"
          + " where i.company_id = ? and i.status = 'APPROVED' and i.invoice_date between ? and ?"
          + " order by i.invoice_date, i.document_no";

  private static final String LEDGER_MOVEMENT =
      "select coalesce(sum(e.debit_base - e.credit_base), 0)"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " join jnl_batch b on b.id = e.batch_id"
          + " where e.company_id = ? and a.code = ? and e.value_date between ? and ?"
          + " and e.journal_type <> 'CLOSING' and coalesce(b.source_module, '') <> ?";

  private static final String PARTIES =
      "select code, name, tax_id, address, party_type from pty_party"
          + " where company_id = ? and code = any(?)";

  private static final String ACCOUNT_BALANCES =
      "select a.code, a.report_group, cast(null as varchar) business_line,"
          + " coalesce(sum(b.debit_base - b.credit_base), 0) net"
          + " from gl_daily_balance b join coa_account a on a.id = b.account_id"
          + " where b.company_id = ? and b.balance_date <= ?"
          + " group by a.code, a.report_group";

  private static final String ACCOUNT_MOVEMENTS =
      "select a.code, a.report_group, e.business_line,"
          + " coalesce(sum(e.debit_base - e.credit_base), 0) net"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and e.value_date between ? and ?"
          + " and e.journal_type <> 'CLOSING'"
          + " group by a.code, a.report_group, e.business_line";

  private static final String CODE = "code";

  private final JdbcTemplate jdbc;

  /**
   * Creates the query service.
   *
   * @param jdbc JDBC template
   */
  public TaxSourceQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Approved supplier invoices dated in a period.
   *
   * @param companyId company
   * @param from invoice date from
   * @param to invoice date to
   * @return invoices ordered by date and number
   */
  public List<InvoiceTaxRow> supplierInvoices(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        SUPPLIER_INVOICES, (rs, i) -> invoice(rs), companyId, Date.valueOf(from), Date.valueOf(to));
  }

  /**
   * Posted movement (debit − credit, base currency) of an account in a period, excluding tax
   * remittance and year-end closing journals.
   *
   * @param companyId company
   * @param accountCode account
   * @param from value date from
   * @param to value date to
   * @return net movement
   */
  public BigDecimal ledgerMovement(
      Long companyId, String accountCode, LocalDate from, LocalDate to) {
    return jdbc.queryForObject(
        LEDGER_MOVEMENT,
        BigDecimal.class,
        companyId,
        accountCode,
        Date.valueOf(from),
        Date.valueOf(to),
        MODULE);
  }

  /**
   * Master facts of parties.
   *
   * @param companyId company
   * @param codes party codes
   * @return facts by code
   */
  public Map<String, PartyFacts> parties(Long companyId, Collection<String> codes) {
    Map<String, PartyFacts> out = new HashMap<>();
    if (codes.isEmpty()) {
      return out;
    }
    jdbc.query(
        PARTIES,
        rs -> {
          out.put(
              rs.getString(CODE),
              new PartyFacts(
                  rs.getString(CODE),
                  rs.getString("name"),
                  rs.getString("tax_id"),
                  rs.getString("address"),
                  rs.getString("party_type")));
        },
        companyId,
        codes.toArray(String[]::new));
    return out;
  }

  /**
   * Closing balance (debit − credit, base currency) of every account as of a date.
   *
   * @param companyId company
   * @param asOf date
   * @return balances per account
   */
  public List<AccountAmount> balances(Long companyId, LocalDate asOf) {
    return jdbc.query(
        ACCOUNT_BALANCES, (rs, i) -> accountAmount(rs), companyId, Date.valueOf(asOf));
  }

  /**
   * Movement (debit − credit, base currency) of every account and line of business in a period,
   * excluding year-end closing journals.
   *
   * @param companyId company
   * @param from value date from
   * @param to value date to
   * @return movements per account and line of business
   */
  public List<AccountAmount> movements(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        ACCOUNT_MOVEMENTS,
        (rs, i) -> accountAmount(rs),
        companyId,
        Date.valueOf(from),
        Date.valueOf(to));
  }

  private static InvoiceTaxRow invoice(ResultSet rs) throws SQLException {
    return new InvoiceTaxRow(
        rs.getLong("id"),
        rs.getString("document_no"),
        rs.getString("supplier_invoice_no"),
        rs.getObject("invoice_date", LocalDate.class),
        rs.getString("party_code"),
        rs.getString("party_name"),
        rs.getBigDecimal("net_amount"),
        rs.getBigDecimal("vat_amount"),
        rs.getBigDecimal("wht_amount"),
        rs.getBigDecimal("payable_amount"),
        rs.getBigDecimal("base_payable"),
        rs.getBoolean("capital_goods"));
  }

  private static AccountAmount accountAmount(ResultSet rs) throws SQLException {
    return new AccountAmount(
        rs.getString(CODE),
        rs.getString("report_group"),
        rs.getString("business_line"),
        rs.getBigDecimal("net"));
  }

  /**
   * Tax facts of an approved supplier invoice (invoice currency, plus the base payable).
   *
   * @param id invoice id
   * @param documentNo internal document number
   * @param supplierInvoiceNo supplier's invoice number
   * @param invoiceDate invoice date
   * @param partyCode supplier
   * @param partyName supplier name
   * @param net net amount
   * @param vat input VAT
   * @param withholding EWT withheld
   * @param payable amount payable
   * @param basePayable amount payable in base currency
   * @param capitalGoods true when a line is booked to an asset account
   */
  public record InvoiceTaxRow(
      Long id,
      String documentNo,
      String supplierInvoiceNo,
      LocalDate invoiceDate,
      String partyCode,
      String partyName,
      BigDecimal net,
      BigDecimal vat,
      BigDecimal withholding,
      BigDecimal payable,
      BigDecimal basePayable,
      boolean capitalGoods) {}

  /**
   * Party master facts.
   *
   * @param code code
   * @param name name
   * @param taxId TIN as captured
   * @param address address
   * @param partyType party type
   */
  public record PartyFacts(
      String code, String name, String taxId, String address, String partyType) {}

  /**
   * Ledger amount of one account (and line of business for movements).
   *
   * @param accountCode account code
   * @param reportGroup report group of the account
   * @param businessLine line of business, null for balances or untagged lines
   * @param net debit − credit in base currency
   */
  public record AccountAmount(
      String accountCode, String reportGroup, String businessLine, BigDecimal net) {}
}
