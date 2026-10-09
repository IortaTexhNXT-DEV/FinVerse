package com.iortatechnxt.brokerverse.cashiering.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What an OR of other income settles (FRS.CSH.02.02.08): for an incentive payment the Valid for
 * Collection SOAs of the insurer (the incentive runs posted for it), for a Fee Policy payment the
 * open fee invoices of the client, each with its outstanding amount (less the ORs already posted
 * for it).
 */
@Service
@Transactional(readOnly = true)
public class OtherIncomeItems {

  private static final String SETTLED =
      " coalesce((select sum(rr.amount) from csh_receipt_record rr where rr.company_id = :company"
          + " and rr.other_income_ref = x.reference and rr.record_kind = 'CREATION'"
          + " and rr.stage = 'POSTED'), 0)";

  private static final String INCENTIVE_SOAS =
      "select x.reference, x.description, x.item_date, x.amount, x.amount -"
          + SETTLED
          + " as outstanding from (select r.run_no as reference, 'Incentive '"
          + " || to_char(r.period_from, 'DD-Mon-YYYY') || ' to ' || to_char(r.period_to, 'DD-Mon-YYYY')"
          + " as description, r.period_to as item_date, sum(l.incentive) as amount"
          + " from cmr_incentive_run r join cmr_incentive_run_line l on l.run_id = r.id"
          + " where r.company_id = :company and r.status = 'POSTED' and not l.excluded"
          + " and l.insurer_code = :party group by r.run_no, r.period_from, r.period_to) x"
          + " order by x.item_date, x.reference";

  private static final String FEE_INVOICES =
      "select x.reference, x.description, x.item_date, x.amount, x.amount -"
          + SETTLED
          + " as outstanding from (select si.si_no as reference,"
          + " coalesce('Fee invoice ' || si.invoice_no, 'Fee invoice') as description,"
          + " si.issue_date as item_date, si.net_amount as amount from bkg_service_invoice si"
          + " where si.company_id = :company and si.recipient_code = :party"
          + " and si.type_code = 'BROKER_BOOKING' and si.credit_of is null) x"
          + " order by x.item_date, x.reference";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the lookup.
   *
   * @param jdbc JDBC
   */
  public OtherIncomeItems(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The open items an OR of a type can settle for a party.
   *
   * @param companyId company
   * @param orType INCENTIVE (SOAs of an insurer) or SERVICE_FEE (fee invoices of a client)
   * @param party insurer or client code
   * @return items with an outstanding amount
   */
  public List<Item> open(Long companyId, String orType, String party) {
    String sql =
        switch (orType == null ? "" : orType) {
          case "INCENTIVE" -> INCENTIVE_SOAS;
          case "SERVICE_FEE" -> FEE_INVOICES;
          default -> null;
        };
    if (sql == null || party == null || party.isBlank()) {
      return List.of();
    }
    return jdbc
        .query(
            sql,
            new MapSqlParameterSource("company", companyId).addValue("party", party.strip()),
            (rs, n) ->
                new Item(
                    rs.getString("reference"),
                    rs.getString("description"),
                    rs.getObject("item_date", LocalDate.class),
                    rs.getBigDecimal("amount"),
                    rs.getBigDecimal("outstanding")))
        .stream()
        .filter(i -> i.outstanding() != null && i.outstanding().signum() > 0)
        .toList();
  }

  /**
   * An item an OR can settle.
   *
   * @param reference SOA or invoice number
   * @param description description
   * @param date period end or issue date
   * @param amount amount
   * @param outstanding amount not settled yet
   */
  public record Item(
      String reference,
      String description,
      LocalDate date,
      BigDecimal amount,
      BigDecimal outstanding) {}
}
