package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The read-only Accounts view of the insurer page (G08; FR-CR-061): the general ledger accounts
 * posted with the insurer as party, with their debits, credits and balance in the base currency,
 * and the insurer's sub-ledger open items by document type, direction, currency and status, with
 * the amount, the settled amount and the outstanding balance. The accounts are maintained on the
 * accounting screens.
 */
@Service
@Transactional(readOnly = true)
public class InsurerLedgerService {

  private static final String GL =
      "select a.code, a.name, a.account_class, a.control_account, a.sub_ledger_type,"
          + " count(*) as entries, sum(e.debit_base) as debit, sum(e.credit_base) as credit,"
          + " max(e.value_date) as last_date"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and e.party_code = ?"
          + " group by a.code, a.name, a.account_class, a.control_account, a.sub_ledger_type"
          + " order by a.code";

  private static final String SUB_LEDGER =
      "select document_type, direction, currency, status, count(*) as items,"
          + " sum(amount) as amount, sum(settled_amount) as settled"
          + " from sl_open_item where company_id = ? and party_code = ?"
          + " group by document_type, direction, currency, status"
          + " order by document_type, direction, currency, status";

  private final InsurerService insurers;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param insurers insurer profiles
   * @param jdbc JDBC
   */
  public InsurerLedgerService(InsurerService insurers, JdbcTemplate jdbc) {
    this.insurers = insurers;
    this.jdbc = jdbc;
  }

  /**
   * A general ledger account posted for the insurer.
   *
   * @param code account code
   * @param name account name
   * @param accountClass class (asset, liability...)
   * @param controlAccount control account of a sub-ledger
   * @param subLedgerType sub-ledger of the account
   * @param entries ledger entries
   * @param debit debits in the base currency
   * @param credit credits in the base currency
   * @param balance debit minus credit
   * @param lastDate last value date
   */
  public record GlAccount(
      String code,
      String name,
      String accountClass,
      boolean controlAccount,
      String subLedgerType,
      long entries,
      BigDecimal debit,
      BigDecimal credit,
      BigDecimal balance,
      LocalDate lastDate) {}

  /**
   * Sub-ledger open items of one kind.
   *
   * @param documentType document type
   * @param direction DEBIT or CREDIT
   * @param currency currency
   * @param status status
   * @param items open items
   * @param amount amount
   * @param settled settled amount
   * @param outstanding amount not yet settled
   */
  public record SubLedgerLine(
      String documentType,
      String direction,
      String currency,
      String status,
      long items,
      BigDecimal amount,
      BigDecimal settled,
      BigDecimal outstanding) {}

  /**
   * The accounts of an insurer.
   *
   * @param glAccounts general ledger accounts
   * @param subLedger sub-ledger open items
   */
  public record InsurerAccounts(List<GlAccount> glAccounts, List<SubLedgerLine> subLedger) {}

  /**
   * The accounts of an insurer.
   *
   * @param insurer insurer profile
   * @return accounts
   */
  public InsurerAccounts accounts(InsurerProfile insurer) {
    Long company = insurer.getCompanyId();
    String party = insurer.getPartyCode();
    List<GlAccount> gl =
        jdbc.query(
            GL,
            (rs, n) -> {
              BigDecimal debit = rs.getBigDecimal("debit");
              BigDecimal credit = rs.getBigDecimal("credit");
              Date last = rs.getDate("last_date");
              return new GlAccount(
                  rs.getString("code"),
                  rs.getString("name"),
                  rs.getString("account_class"),
                  rs.getBoolean("control_account"),
                  rs.getString("sub_ledger_type"),
                  rs.getLong("entries"),
                  debit,
                  credit,
                  debit.subtract(credit),
                  last == null ? null : last.toLocalDate());
            },
            company,
            party);
    List<SubLedgerLine> sl =
        jdbc.query(
            SUB_LEDGER,
            (rs, n) -> {
              BigDecimal amount = rs.getBigDecimal("amount");
              BigDecimal settled = rs.getBigDecimal("settled");
              return new SubLedgerLine(
                  rs.getString("document_type"),
                  rs.getString("direction"),
                  rs.getString("currency"),
                  rs.getString("status"),
                  rs.getLong("items"),
                  amount,
                  settled,
                  amount.subtract(settled));
            },
            company,
            party);
    return new InsurerAccounts(gl, sl);
  }

  /**
   * An insurer profile.
   *
   * @param insurerId insurer profile
   * @return insurer
   */
  public InsurerProfile insurer(Long insurerId) {
    return insurers.get(insurerId);
  }
}
