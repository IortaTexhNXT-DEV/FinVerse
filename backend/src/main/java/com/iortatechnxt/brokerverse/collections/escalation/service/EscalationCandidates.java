package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule.Filters;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The open collection accounts of a company as the escalation rules see them (BRCLXN.049): ledger
 * invoices that are client receivables (not direct payment, not cancelled, not written off) with a
 * premium receivable balance above the minimum threshold, filtered by segment, sales unit, product
 * line and outstanding range. A read-only query of the Operations ledger in this package, so that
 * the escalation does not depend on the worklist of wave C1-A.
 */
@Component
@Transactional(readOnly = true)
public class EscalationCandidates {

  private static final String SQL =
      "select i.invoice_no, i.arn, i.client_code, i.assured_name, i.policy_no, i.currency,"
          + " i.booking_date, i.inception_date, i.ao_username, b.balance"
          + " from ops_invoice i join (select c.invoice_id, sum(c.balance) as balance"
          + " from ops_invoice_component c"
          + " where c.component in ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER')"
          + " group by c.invoice_id) b on b.invoice_id = i.id"
          + " where i.company_id = :companyId and not i.dp_flag and not i.cancelled"
          + " and not i.written_off and b.balance > :threshold"
          + " and (cast(:invoiceNo as varchar) is null or i.invoice_no = :invoiceNo)"
          + " and (cast(:segment as varchar) is null or i.segment = :segment)"
          + " and (cast(:unit as varchar) is null or i.sales_unit = :unit)"
          + " and (cast(:line as varchar) is null or i.product_line = :line)"
          + " and (cast(:amountFrom as numeric) is null or b.balance >= :amountFrom)"
          + " and (cast(:amountTo as numeric) is null or b.balance <= :amountTo)"
          + " order by i.booking_date, i.invoice_no";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the query.
   *
   * @param jdbc named-parameter JDBC
   */
  public EscalationCandidates(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Open accounts matching a rule's filters.
   *
   * @param companyId company
   * @param filters rule filters
   * @param threshold minimum outstanding followed up
   * @return accounts, oldest booking first
   */
  public List<Candidate> open(Long companyId, Filters filters, BigDecimal threshold) {
    return query(companyId, null, filters, threshold);
  }

  /**
   * One invoice, if it is an open account matching a rule's filters.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param filters rule filters
   * @param threshold minimum outstanding followed up
   * @return the account, or nothing
   */
  public List<Candidate> one(
      Long companyId, String invoiceNo, Filters filters, BigDecimal threshold) {
    return query(companyId, invoiceNo, filters, threshold);
  }

  private List<Candidate> query(
      Long companyId, String invoiceNo, Filters filters, BigDecimal threshold) {
    Map<String, Object> args = new HashMap<>();
    args.put("invoiceNo", invoiceNo);
    args.put("companyId", companyId);
    args.put("threshold", threshold);
    args.put("segment", filters.segment());
    args.put("unit", filters.salesUnit());
    args.put("line", filters.productLine());
    args.put("amountFrom", filters.amountFrom());
    args.put("amountTo", filters.amountTo());
    return jdbc.query(
        SQL,
        args,
        (rs, n) ->
            new Candidate(
                rs.getString("invoice_no"),
                rs.getString("arn"),
                rs.getString("client_code"),
                rs.getString("assured_name"),
                rs.getString("policy_no"),
                rs.getString("currency"),
                new Dates(
                    rs.getDate("booking_date").toLocalDate(),
                    rs.getDate("inception_date").toLocalDate()),
                rs.getString("ao_username"),
                rs.getBigDecimal("balance")));
  }

  /**
   * An open collection account.
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param policyNo policy number
   * @param currency currency
   * @param dates booking and inception dates
   * @param aoUsername account officer
   * @param balance outstanding premium receivable
   */
  public record Candidate(
      String invoiceNo,
      String arn,
      String clientCode,
      String assuredName,
      String policyNo,
      String currency,
      Dates dates,
      String aoUsername,
      BigDecimal balance) {

    /**
     * The account of a ledger invoice (manual escalation).
     *
     * @param invoice invoice with its components
     * @return account
     */
    public static Candidate of(OpsInvoice invoice) {
      return new Candidate(
          invoice.getInvoiceNo(),
          invoice.getArn(),
          invoice.getClientCode(),
          invoice.getAssuredName(),
          invoice.getPolicyNo(),
          invoice.getCurrency(),
          new Dates(invoice.getBookingDate(), invoice.getClassification().inceptionDate()),
          invoice.getClassification().aoUsername(),
          invoice.premiumBalance());
    }
  }

  /**
   * The dates aging counts from.
   *
   * @param booking booking date
   * @param inception inception date
   */
  public record Dates(LocalDate booking, LocalDate inception) {}
}
