package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The list of the unapplied payments (FRS.CSH.06.01.01 to 06.01.07): filtered by unapplied payment
 * type, payment date, insurer, client, assured, account reference and a partial text (unapplied
 * payment number, reference or payor, without regard to case), with the source, type, account, PN
 * and loan application numbers, client and insurer names, amount, outstanding and status; fully
 * applied payments leave the list. A Marketing user sees the records of his or her marketing unit
 * only (setting {@code CASH_UNAPPLIED_MARKETING_SCOPE}, Appendix R, C13).
 */
@Service
@Transactional(readOnly = true)
public class UnappliedInquiry {

  /** Setting: OWN_UNIT (default) or ALL. */
  public static final String MARKETING_SCOPE = "CASH_UNAPPLIED_MARKETING_SCOPE";

  private static final List<String> CASHIERING =
      List.of(
          "CASH_RECEIPT",
          "CASH_APPROVE",
          "CASH_APPLY",
          "CASH_DISPOSITION",
          "CASH_DISPOSITION_APPROVE",
          "CASH_PRINT",
          "CASH_UPLOAD");

  private static final String TYPE =
      "case when u.origin in ('NO_MATCH', 'CANCELLED_REFERENCE') then 'UNBOOKED'"
          + " when u.origin in ('EXCESS', 'ADJUSTMENT') then 'EXCESS'"
          + " when u.origin = 'PREBOOKED' then 'PREBOOKED'"
          + " when u.origin = 'AR_INSURER_REFUND' then 'AR_INSURER_REFUND'"
          + " when u.origin in ('AP_UNAPPLIED_COMMISSION', 'COMMISSION_RECEIVABLE')"
          + " then 'AP_UNAPPLIED_COMMISSION' else 'UNAPPLIED' end";

  private static final String AND = " and ";
  private static final int SQL_CAPACITY = 512;

  private static final String PAID =
      "coalesce(p.value_date, r.receipt_date, cast(u.created_at as date))";

  private static final String FROM =
      " from csh_unapplied u left join csh_receipt r on r.id = u.receipt_id"
          + " left join csh_payment p on p.id = u.payment_id"
          + " left join ops_invoice i on i.invoice_no = u.invoice_no"
          + " left join acc_account a on a.id = i.account_id"
          + " left join pty_party ins on ins.company_id = u.company_id and ins.code = i.insurer_code"
          + " where u.company_id = :company";

  private static final String SELECT =
      "select u.id, u.reference, u.origin, coalesce(u.source_module, '') as source_module,"
          + " coalesce(u.source_ref, r.receipt_no, p.payment_no) as source_ref, "
          + TYPE
          + " as type, "
          + PAID
          + " as paid_on, coalesce(i.arn, u.invoice_no) as account_no, u.invoice_no, i.pn_nos,"
          + " a.loan_application_no, coalesce(a.client_name, u.payor_name) as client_name,"
          + " coalesce(i.assured_name, u.payor_name) as assured, i.insurer_code,"
          + " coalesce(ins.name, i.insurer_code) as insurer_name, u.sales_unit, u.currency,"
          + " u.amount, u.balance, u.stage";

  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;

  /**
   * Creates the inquiry.
   *
   * @param jdbc JDBC
   * @param currentUser user (marketing unit)
   * @param parameters marketing scope
   */
  public UnappliedInquiry(
      NamedParameterJdbcTemplate jdbc, CurrentUser currentUser, SystemParameterService parameters) {
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.parameters = parameters;
  }

  /**
   * Searches the unapplied payments.
   *
   * @param companyId company
   * @param f filters
   * @param pageable page
   * @return rows, newest first
   */
  public Page<Row> search(Long companyId, Filter f, Pageable pageable) {
    MapSqlParameterSource args = new MapSqlParameterSource("company", companyId);
    StringBuilder where = new StringBuilder(SQL_CAPACITY);
    criteria(f, args, where);
    String unit = marketingUnit();
    if (unit != null) {
      where.append(" and u.sales_unit = :unit");
      args.addValue("unit", unit);
    }
    Long total = jdbc.queryForObject("select count(*)" + FROM + where, args, Long.class);
    args.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
    List<Row> rows =
        jdbc.query(
            SELECT
                + FROM
                + where
                + " order by u.created_at desc, u.id desc limit :limit offset :offset",
            args,
            (rs, n) ->
                new Row(
                    rs.getLong("id"),
                    rs.getString("reference"),
                    source(rs.getString("source_module"), rs.getString("source_ref")),
                    rs.getString("type"),
                    rs.getObject("paid_on", LocalDate.class),
                    new Account(
                        rs.getString("account_no"),
                        rs.getString("invoice_no"),
                        rs.getString("pn_nos"),
                        rs.getString("loan_application_no")),
                    new Parties(
                        rs.getString("client_name"),
                        rs.getString("assured"),
                        rs.getString("insurer_name"),
                        rs.getString("sales_unit")),
                    rs.getString("currency"),
                    rs.getBigDecimal("amount"),
                    rs.getBigDecimal("balance"),
                    rs.getString("stage")));
    return new PageImpl<>(rows, pageable, total == null ? 0 : total);
  }

  private static void criteria(Filter f, MapSqlParameterSource args, StringBuilder where) {
    if (f.openOnly()) {
      where.append(" and u.balance > 0");
    }
    if (present(f.type())) {
      where.append(AND).append(TYPE).append(" = :type");
      args.addValue("type", f.type());
    }
    if (f.from() != null) {
      where.append(AND).append(PAID).append(" >= :from");
      args.addValue("from", f.from());
    }
    if (f.to() != null) {
      where.append(AND).append(PAID).append(" <= :to");
      args.addValue("to", f.to());
    }
    exact(where, args, "i.insurer_code", "insurer", f.insurerCode());
    exact(where, args, "u.client_code", "client", f.clientCode());
    if (present(f.assured())) {
      where.append(" and lower(coalesce(i.assured_name, u.payor_name)) like :assured");
      args.addValue("assured", like(f.assured()));
    }
    if (present(f.accountRef())) {
      where.append(" and (u.invoice_no = :account or i.arn = :account)");
      args.addValue("account", f.accountRef().strip());
    }
    if (present(f.text())) {
      where.append(
          " and (lower(u.reference) like lower(:text) or lower(u.payor_name) like lower(:text)"
              + " or lower(coalesce(u.invoice_no, '')) like lower(:text))");
      args.addValue("text", like(f.text()));
    }
  }

  private static void exact(
      StringBuilder where, MapSqlParameterSource args, String column, String name, String value) {
    if (present(value)) {
      where.append(AND).append(column).append(" = :").append(name);
      args.addValue(name, value.strip());
    }
  }

  /**
   * The marketing unit the list is limited to: the unit of a user who works the list without a
   * Cashiering role, when the scope is OWN_UNIT.
   *
   * @return unit, null for no limit
   */
  private String marketingUnit() {
    boolean cashier = CASHIERING.stream().anyMatch(currentUser::hasAuthority);
    String scope = parameters.text(MARKETING_SCOPE, "OWN_UNIT").strip();
    if (cashier || "ALL".equals(scope)) {
      return null;
    }
    List<String> units =
        jdbc.queryForList(
            "select coalesce(business_unit_code, '') from sec_user where username = :user",
            new MapSqlParameterSource("user", currentUser.username()),
            String.class);
    return units.isEmpty() ? "" : units.get(0);
  }

  private static String source(String module, String ref) {
    List<String> parts = new ArrayList<>();
    if (present(module)) {
      parts.add(module);
    }
    if (present(ref)) {
      parts.add(ref);
    }
    return String.join(" ", parts);
  }

  private static String like(String text) {
    return "%" + text.strip() + "%";
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  /**
   * Filters of the list.
   *
   * @param type unapplied payment type (EXCESS, UNAPPLIED, AR_INSURER_REFUND, UNBOOKED, PREBOOKED,
   *     AP_UNAPPLIED_COMMISSION)
   * @param from payment date from
   * @param to payment date to
   * @param insurerCode insurer
   * @param clientCode client
   * @param assured part of the assured's name
   * @param accountRef account reference or invoice number
   * @param text part of the unapplied payment number, reference or payor
   * @param openOnly only the payments with an outstanding amount
   */
  public record Filter(
      String type,
      LocalDate from,
      LocalDate to,
      String insurerCode,
      String clientCode,
      String assured,
      String accountRef,
      String text,
      boolean openOnly) {}

  /**
   * The account of an unapplied payment.
   *
   * @param accountNo account reference number
   * @param invoiceNo invoice
   * @param pnNo promissory note numbers
   * @param loanApplicationNo loan application number
   */
  public record Account(
      String accountNo, String invoiceNo, String pnNo, String loanApplicationNo) {}

  /**
   * The parties of an unapplied payment.
   *
   * @param clientName client
   * @param assured assured
   * @param insurerName insurer
   * @param salesUnit marketing unit
   */
  public record Parties(String clientName, String assured, String insurerName, String salesUnit) {}

  /**
   * An unapplied payment of the list.
   *
   * @param id id
   * @param reference unapplied payment number
   * @param source source module and record
   * @param type unapplied payment type
   * @param paidOn payment or creation date
   * @param account account, PN and loan application numbers
   * @param parties client, assured, insurer and marketing unit
   * @param currency currency
   * @param amount amount
   * @param outstanding outstanding
   * @param status status
   */
  public record Row(
      long id,
      String reference,
      String source,
      String type,
      LocalDate paidOn,
      Account account,
      Parties parties,
      String currency,
      BigDecimal amount,
      BigDecimal outstanding,
      String status) {}
}
