package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.frbs.domain.PaidInvoice;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.Unit;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the base of the service fee from the Operations invoice ledger (FRBS 2.10.0): invoices
 * whose payment status became PAID in the period (the commission is then fully collected), not
 * cancelled, with commission, and not yet in a live service-fee run; and the sales units with their
 * cost centres. Read-only SQL, like the other reports over the ledger.
 */
@Service
@Transactional(readOnly = true)
public class ServiceFeeBase {

  /** Business time zone of BDOI: a day is a Manila day. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private static final String PAID =
      "select i.invoice_no, i.root_invoice_no, i.client_code, i.assured_name, i.insurer_code,"
          + " i.segment, i.sales_unit, i.cost_center, i.branch_id, i.currency, i.commission,"
          + " coalesce((select c.booked from ops_invoice_component c"
          + " where c.invoice_id = i.id and c.component = 'WTAX'), 0) wtax,"
          + " s.paid_at"
          + " from ops_invoice i"
          + " join (select invoice_id, max(changed_at) paid_at from ops_invoice_status_change"
          + " where field = 'PAYMENT_STATUS' and to_value = 'PAID' group by invoice_id) s"
          + " on s.invoice_id = i.id"
          + " where i.company_id = ? and i.payment_status = 'PAID' and not i.cancelled"
          + " and i.commission > 0 and s.paid_at >= ? and s.paid_at < ?"
          + " and not exists (select 1 from frbs_service_fee_item f"
          + " where f.invoice_no = i.invoice_no and f.live)"
          + " order by i.invoice_no";

  private static final String UNITS =
      "select code, name, cost_center from cat_sales_unit where company_id = ?";

  private final JdbcTemplate jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc JDBC template
   */
  public ServiceFeeBase(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Invoices fully paid in a period and free for a run.
   *
   * @param companyId company
   * @param from first day (Manila)
   * @param to last day (Manila)
   * @return invoices in number order
   */
  public List<PaidInvoice> paid(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        PAID,
        (rs, i) ->
            new PaidInvoice(
                rs.getString("invoice_no"),
                rs.getString("root_invoice_no"),
                rs.getString("client_code"),
                rs.getString("assured_name"),
                rs.getString("insurer_code"),
                rs.getString("segment"),
                rs.getString("sales_unit"),
                rs.getString("cost_center"),
                rs.getLong("branch_id"),
                rs.getString("currency"),
                rs.getBigDecimal("commission"),
                rs.getBigDecimal("wtax"),
                rs.getTimestamp("paid_at").toInstant().atZone(MANILA).toLocalDate()),
        companyId,
        Timestamp.from(from.atStartOfDay(MANILA).toInstant()),
        Timestamp.from(to.plusDays(1).atStartOfDay(MANILA).toInstant()));
  }

  /**
   * The sales units of a company.
   *
   * @param companyId company
   * @return name and cost centre by unit code
   */
  public Map<String, Unit> units(Long companyId) {
    Map<String, Unit> units = new HashMap<>();
    jdbc.query(
        UNITS,
        rs -> {
          units.put(
              rs.getString("code"), new Unit(rs.getString("name"), rs.getString("cost_center")));
        },
        companyId);
    return units;
  }
}
