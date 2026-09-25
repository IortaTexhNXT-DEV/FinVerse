package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the port {@link UnappliedDirectory} (BRCLXN.034-036, 040;
 * COLLECTIONS_DESIGN 9): the open unapplied items with the fields of the payment they came from
 * (payment date, upload batch as the payment file, transaction no., type, bank, check, reference)
 * and the status of their current disposition or collector request, read in place so Collections
 * never copies them. The Cashiering tab is derived from the stage of the workflow {@code
 * OPS_DISPOSITION} (UNAPPLIED, MONITORING, FOR_APPROVAL, FOR_REVERSAL, DONE).
 */
@Service
@Transactional(readOnly = true)
public class CashieringUnappliedDirectory implements UnappliedDirectory {

  private static final String SELECT =
      "select * from (select u.id, u.reference, u.company_id, u.currency, u.amount, u.balance,"
          + " u.client_code, u.invoice_no, u.sales_unit, u.payor_name,"
          + " coalesce(p.value_date, r.receipt_date, cast(u.created_at as date)) as payment_date,"
          + " p.batch_ref, coalesce(p.payment_no, r.receipt_no) as transaction_no,"
          + " coalesce(p.payment_mode, r.payment_mode) as payment_type,"
          + " coalesce(p.check_bank, r.check_bank) as bank_code,"
          + " coalesce(p.check_no, r.check_no) as check_no, p.reference as payment_reference,"
          + " case when u.stage = 'UNAPPLIED' then 'UNAPPLIED'"
          + " when u.stage in ('MONITORING', 'IN_PROCESS') then 'MONITORING'"
          + " when u.stage in ('FOR_APPROVAL', 'FOR_REVERSAL') then u.stage else 'DONE' end as tab,"
          + " coalesce((select d.status from csh_disposition d where d.unapplied_id = u.id"
          + " and d.status not in ('WITHDRAWN', 'REVERSED') order by d.id desc limit 1),"
          + " (select 'REQUEST_' || c.status from csh_collector_request c"
          + " where c.unapplied_id = u.id order by c.id desc limit 1)) as disposition_status"
          + " from csh_unapplied u left join csh_payment p on p.id = u.payment_id"
          + " left join csh_receipt r on r.id = u.receipt_id) x";

  private static final String OPEN_WHERE =
      " where x.company_id = :companyId and x.balance > 0"
          + " and (cast(:text as varchar) is null or lower(x.reference) like :text"
          + " or lower(coalesce(x.payor_name, '')) like :text"
          + " or lower(coalesce(x.transaction_no, '')) like :text"
          + " or lower(coalesce(x.check_no, '')) like :text"
          + " or lower(coalesce(x.payment_reference, '')) like :text"
          + " or lower(coalesce(x.invoice_no, '')) like :text)"
          + " and (cast(:client as varchar) is null or x.client_code = :client)"
          + " and (cast(:unit as varchar) is null or x.sales_unit = :unit)"
          + " and (cast(:tab as varchar) is null or x.tab = :tab)"
          + " and (cast(:paidFrom as date) is null or x.payment_date >= :paidFrom)"
          + " and (cast(:paidTo as date) is null or x.payment_date <= :paidTo)";

  private final NamedParameterJdbcTemplate jdbc;
  private final UnappliedHistory history;

  /**
   * Creates the directory.
   *
   * @param jdbc named-parameter JDBC
   * @param history item history
   */
  public CashieringUnappliedDirectory(NamedParameterJdbcTemplate jdbc, UnappliedHistory history) {
    this.jdbc = jdbc;
    this.history = history;
  }

  @Override
  public Page<UnappliedView> open(Long companyId, UnappliedFilter filter, Pageable pageable) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("text", like(filter.text()));
    args.put("client", blankToNull(filter.clientCode()));
    args.put("unit", blankToNull(filter.salesUnit()));
    args.put("tab", blankToNull(filter.tab()));
    args.put("paidFrom", filter.paidFrom());
    args.put("paidTo", filter.paidTo());
    Long total =
        jdbc.queryForObject(
            "select count(*) from (" + SELECT + OPEN_WHERE + ") c", args, Long.class);
    if (total == null || total == 0) {
      return Page.empty(pageable);
    }
    args.put("limit", pageable.getPageSize());
    args.put("offset", pageable.getOffset());
    List<UnappliedView> rows =
        jdbc.query(
            SELECT
                + OPEN_WHERE
                + " order by x.payment_date desc, x.id desc limit :limit offset :offset",
            args,
            (rs, i) -> view(rs));
    return new PageImpl<>(rows, pageable, total);
  }

  @Override
  public Optional<UnappliedView> find(String unappliedRef) {
    return jdbc
        .query(
            SELECT + " where x.reference = :ref",
            Map.of("ref", unappliedRef == null ? "" : unappliedRef),
            (rs, i) -> view(rs))
        .stream()
        .findFirst();
  }

  @Override
  public List<UnappliedEvent> history(String unappliedRef) {
    return history.of(unappliedRef);
  }

  private static UnappliedView view(ResultSet rs) throws SQLException {
    return new UnappliedView(
        rs.getString("reference"),
        rs.getLong("company_id"),
        rs.getDate("payment_date").toLocalDate(),
        rs.getString("batch_ref"),
        rs.getString("transaction_no"),
        rs.getString("currency"),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("balance"),
        rs.getString("payment_type"),
        rs.getString("payor_name"),
        rs.getString("bank_code"),
        rs.getString("check_no"),
        rs.getString("payment_reference"),
        rs.getString("client_code"),
        rs.getString("invoice_no"),
        rs.getString("sales_unit"),
        rs.getString("tab"),
        rs.getString("disposition_status"));
  }

  private static String like(String text) {
    String clean = blankToNull(text);
    return clean == null ? null : "%" + clean.toLowerCase(Locale.ROOT) + "%";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
