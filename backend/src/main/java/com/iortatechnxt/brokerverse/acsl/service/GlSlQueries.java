package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Constant SQL of the GL-SL reconciliation (ACSL 2.13.2) over the platform and Operations tables:
 * the control accounts of a company and the balance of each kind of sub-ledger, debit positive in
 * base currency (the Operations ledger in the configured currency).
 */
@Component
@Transactional(readOnly = true)
public class GlSlQueries {

  private static final String CONTROL_ACCOUNTS_SQL =
      "select id, code, name, account_class from coa_account where company_id = ? and control_account"
          + " and postable and record_status = 'ACTIVE' order by code";

  private static final String PARTY_LEDGER_SQL =
      "select coalesce(sum(debit_base - credit_base), 0) from gl_ledger_entry"
          + " where company_id = ? and account_id = ? and value_date <= ? and party_code is not null";

  private static final String OPEN_ITEMS_SQL =
      "select coalesce(sum(case when direction = 'DEBIT' then 1 else -1 end"
          + " * (base_amount - round(settled_amount * base_amount / nullif(amount, 0), 2))), 0)"
          + " from sl_open_item where company_id = ? and document_date <= ?"
          + " and document_type = any(string_to_array(?, ','))";

  private static final String OPS_LEDGER_SQL =
      "select coalesce(sum(c.balance), 0) from ops_invoice_component c"
          + " join ops_invoice i on i.id = c.invoice_id"
          + " where i.company_id = ? and c.component = any(string_to_array(?, ','))"
          + " and (cast(? as varchar) is null or i.currency = ?)";

  private final JdbcTemplate jdbc;

  /**
   * Creates the queries.
   *
   * @param jdbc JDBC template
   */
  public GlSlQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Postable, active control accounts of a company.
   *
   * @param companyId company
   * @return accounts by code
   */
  public List<ControlAccount> controlAccounts(Long companyId) {
    return jdbc.query(
        CONTROL_ACCOUNTS_SQL,
        (rs, i) ->
            new ControlAccount(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                AccountClass.valueOf(rs.getString("account_class"))),
        companyId);
  }

  /**
   * Postings of an account that carry a sub-ledger party.
   *
   * @param companyId company
   * @param accountId account
   * @param asOf date
   * @return balance, debit positive
   */
  public BigDecimal partyLedger(Long companyId, Long accountId, LocalDate asOf) {
    return Money.round(
        jdbc.queryForObject(
            PARTY_LEDGER_SQL, BigDecimal.class, companyId, accountId, Date.valueOf(asOf)));
  }

  /**
   * Outstanding open items of some document types.
   *
   * @param companyId company
   * @param documentTypes document types
   * @param asOf date
   * @return balance, debit positive
   */
  public BigDecimal openItems(Long companyId, List<String> documentTypes, LocalDate asOf) {
    return Money.round(
        jdbc.queryForObject(
            OPEN_ITEMS_SQL,
            BigDecimal.class,
            companyId,
            Date.valueOf(asOf),
            String.join(",", documentTypes)));
  }

  /**
   * Balance of Operations invoice components.
   *
   * @param companyId company
   * @param components components
   * @param currency invoice currency, null for all
   * @param account the control account (for the sign)
   * @return balance, debit positive
   */
  public BigDecimal opsLedger(
      Long companyId, List<String> components, String currency, ControlAccount account) {
    BigDecimal natural =
        Money.round(
            jdbc.queryForObject(
                OPS_LEDGER_SQL,
                BigDecimal.class,
                companyId,
                String.join(",", components),
                currency,
                currency));
    return account.accountClass().normalBalance() == BalanceSide.CREDIT
        ? natural.negate()
        : natural;
  }

  /**
   * A control account.
   *
   * @param id id
   * @param code code
   * @param name name
   * @param accountClass class (natural side)
   */
  public record ControlAccount(Long id, String code, String name, AccountClass accountClass) {}
}
