package com.iortatechnxt.brokerverse.renewal.extraction.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The expiring population read from the Operations ledger (RENEWAL_DESIGN principle 2; BRRN.005,
 * 030): booked root invoices (kind BOOKING, not cancelled) whose policy period ends in a range,
 * with the facts of their account, product and client. A multi-year account yields only its last
 * policy year (the invoice that ends with the account's period). Constant SQL with named
 * parameters.
 */
@Component
@Transactional(readOnly = true)
public class ExpiringPolicies {

  private static final String SQL =
      "select o.invoice_no, o.arn, o.policy_no, o.policy_year, o.pn_nos, o.client_code,"
          + " o.assured_name, o.insurer_code, o.currency, o.inception_date, o.expiry_date,"
          + " o.risk_code, o.product_line, o.segment, o.ao_username, o.sales_unit,"
          + " o.gross_premium, b.code as branch_code, a.client_id, a.client_name,"
          + " a.product_code, a.line_code, a.market_segment, a.source_channel,"
          + " a.total_sum_insured, a.net_premium, a.commission_rate, a.mortgagee_bank,"
          + " a.sales_region, a.sales_department, a.sales_team, a.account_officer,"
          + " a.product_version_no, a.contact_email, a.period_to, p.name as product_name,"
          + " coalesce(p.packaged, false) as packaged, cl.client_type, cl.email as client_email,"
          + " (select string_agg(pn.pn_number, ',' order by pn.pn_index) from acc_account_pn pn"
          + " where pn.account_id = a.id) as account_pns,"
          + " exists (select 1 from rnw_candidate c where c.company_id = o.company_id"
          + " and c.expiring_invoice_no = o.invoice_no) as extracted"
          + " from ops_invoice o"
          + " join org_branch b on b.id = o.branch_id"
          + " left join acc_account a on a.arn = o.arn"
          + " left join cat_product p on p.code = coalesce(a.product_code, o.risk_code)"
          + " left join crm_client cl on cl.id = a.client_id"
          + " where o.company_id = :company and o.kind = 'BOOKING' and not o.cancelled"
          + " and (cast(:invoice as varchar) is null or o.invoice_no = :invoice)"
          + " and (cast(:from as date) is null or o.expiry_date >= :from)"
          + " and (cast(:to as date) is null or o.expiry_date <= :to)"
          + " order by o.expiry_date, o.invoice_no";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc named-parameter JDBC
   */
  public ExpiringPolicies(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The booked root invoices expiring in a range.
   *
   * @param companyId company
   * @param from first expiry date
   * @param to last expiry date
   * @return invoices, earliest expiry first
   */
  public List<ExpiringInvoice> between(Long companyId, LocalDate from, LocalDate to) {
    return query(companyId, null, Date.valueOf(from), Date.valueOf(to));
  }

  /**
   * One booked root invoice with the current facts of its account (snapshot refresh, BRRN.011: the
   * list reads the current account, so endorsements show without a new extraction).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return the invoice, empty when not in the ledger
   */
  public Optional<ExpiringInvoice> invoice(Long companyId, String invoiceNo) {
    return query(companyId, invoiceNo, null, null).stream().findFirst();
  }

  private List<ExpiringInvoice> query(Long companyId, String invoiceNo, Date from, Date to) {
    return jdbc
        .queryForList(
            SQL,
            new MapSqlParameterSource()
                .addValue("company", companyId)
                .addValue("invoice", invoiceNo, Types.VARCHAR)
                .addValue("from", from, Types.DATE)
                .addValue("to", to, Types.DATE))
        .stream()
        .map(ExpiringPolicies::row)
        .toList();
  }

  private static ExpiringInvoice row(Map<String, Object> r) {
    LocalDate periodTo = date(r.get("period_to"));
    LocalDate expiry = date(r.get("expiry_date"));
    return new ExpiringInvoice(
        text(r, "invoice_no"),
        text(r, "arn"),
        r.get("policy_year") == null ? null : ((Number) r.get("policy_year")).intValue(),
        new ExpiringInvoice.Facts(
            text(r, "policy_no"),
            first(text(r, "account_pns"), text(r, "pn_nos")),
            text(r, "insurer_code"),
            text(r, "currency"),
            date(r.get("inception_date")),
            expiry),
        new ExpiringInvoice.Client(
            r.get("client_id") == null ? null : ((Number) r.get("client_id")).longValue(),
            text(r, "client_code"),
            first(text(r, "client_name"), text(r, "assured_name")),
            text(r, "assured_name"),
            first(text(r, "contact_email"), text(r, "client_email")),
            text(r, "client_type")),
        new ExpiringInvoice.Product(
            first(text(r, "product_code"), text(r, "risk_code")),
            text(r, "product_name"),
            first(text(r, "line_code"), text(r, "product_line")),
            first(text(r, "market_segment"), text(r, "segment")),
            text(r, "source_channel"),
            Boolean.TRUE.equals(r.get("packaged")),
            r.get("product_version_no") == null
                ? null
                : ((Number) r.get("product_version_no")).intValue()),
        new ExpiringInvoice.Sales(
            text(r, "branch_code"),
            text(r, "sales_region"),
            text(r, "sales_department"),
            first(text(r, "sales_team"), text(r, "sales_unit")),
            first(text(r, "account_officer"), text(r, "ao_username"))),
        new ExpiringInvoice.Amounts(
            (BigDecimal) r.get("net_premium"),
            (BigDecimal) r.get("gross_premium"),
            (BigDecimal) r.get("total_sum_insured"),
            (BigDecimal) r.get("commission_rate"),
            text(r, "mortgagee_bank")),
        periodTo == null || periodTo.equals(expiry),
        Boolean.TRUE.equals(r.get("extracted")));
  }

  private static String text(Map<String, Object> row, String key) {
    Object v = row.get(key);
    return v == null ? null : v.toString();
  }

  private static String first(String a, String b) {
    return a == null || a.isBlank() ? b : a;
  }

  private static LocalDate date(Object v) {
    return v instanceof Date d ? d.toLocalDate() : null;
  }
}
