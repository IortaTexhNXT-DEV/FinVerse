package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The Cashiering transactions of the renewal accounts counted by the KPI cards Bounced Check,
 * Unapplied Payment and Refund (FRRN.002.02.09): bounced (returned) checks and payment reversals
 * for a bounced check, unapplied payments with a balance, and refund requests, each matched to a
 * renewal account by the expiring or renewal invoice number or account reference.
 */
@Component
public class RenewalCashFacts {

  private static final String BOUNCED =
      "select p.reference as key_ref, p.warehouse_no as txn_ref, p.check_no, p.bank_code as bank,"
          + " cast(p.updated_at as date) as txn_date, p.amount, p.status_reason as remarks"
          + " from csh_pdc_item p where p.company_id = :company and p.status = 'RETURNED'"
          + " and p.reference = any(cast(:keys as varchar[]))"
          + " union all select r.invoice_no, r.request_no, null, null, r.value_date, r.amount, r.reason"
          + " from csh_payment_reversal r where r.company_id = :company"
          + " and r.invoice_no = any(cast(:keys as varchar[])) and lower(coalesce(r.reason, '')) like '%bounc%'";

  private static final String UNAPPLIED =
      "select u.invoice_no as key_ref, u.reference as txn_ref, cast(u.created_at as date) as txn_date,"
          + " u.amount, u.balance, u.origin as remarks from csh_unapplied u"
          + " where u.company_id = :company and u.balance > 0 and u.invoice_no = any(cast(:keys as varchar[]))";

  private static final String REFUND =
      "select coalesce(l.invoice_no, l.root_invoice_no) as key_ref, q.request_no as txn_ref,"
          + " q.request_date as txn_date, l.amount, l.reason_code as remarks,"
          + " case when l.reason_code like '%CANCEL%' then 'Cancellation Refund'"
          + " else 'Excess Payment Refund' end as category"
          + " from prq_request q join prq_request_line l on l.request_id = q.id"
          + " where q.company_id = :company and q.kind = 'REFUND' and q.stage <> 'CANCELLED'"
          + " and (l.invoice_no = any(cast(:keys as varchar[])) or l.root_invoice_no = any(cast(:keys as varchar[])))";

  private final NbReportJdbc jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc SQL
   */
  public RenewalCashFacts(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The transactions of a card for the renewal accounts given.
   *
   * @param companyId company
   * @param card BOUNCED_CHECK, UNAPPLIED_PAYMENT or REFUND
   * @param items renewal accounts
   * @return transactions, each with the renewal account it belongs to ({@code item})
   */
  public List<Map<String, Object>> transactions(
      Long companyId, String card, List<DashboardItem> items) {
    Map<String, DashboardItem> byKey = keys(items);
    if (byKey.isEmpty()) {
      return List.of();
    }
    String sql =
        switch (card) {
          case "BOUNCED_CHECK" -> BOUNCED;
          case "UNAPPLIED_PAYMENT" -> UNAPPLIED;
          case "REFUND" -> REFUND;
          default -> null;
        };
    if (sql == null) {
      return List.of();
    }
    Map<String, Object> args =
        Map.of("company", companyId, "keys", byKey.keySet().toArray(String[]::new));
    return jdbc.rows(sql, args).stream()
        .peek(r -> r.put("item", byKey.get(String.valueOf(r.get("key_ref")))))
        .filter(r -> r.get("item") != null)
        .toList();
  }

  /**
   * The counts of the three cards.
   *
   * @param companyId company
   * @param items renewal accounts
   * @return count by card
   */
  public Map<String, Long> counts(Long companyId, List<DashboardItem> items) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String card : CardRules.CASH) {
      counts.put(card, (long) transactions(companyId, card, items).size());
    }
    return counts;
  }

  private static Map<String, DashboardItem> keys(List<DashboardItem> items) {
    Map<String, DashboardItem> byKey = new LinkedHashMap<>();
    for (DashboardItem i : items) {
      Set<String> keys = new LinkedHashSet<>();
      for (String name : List.of("expiring_invoice_no", "invoice_no", "arn", "expiring_arn")) {
        String v = i.text(name);
        if (v != null && !v.isBlank()) {
          keys.add(v);
        }
      }
      keys.forEach(k -> byKey.putIfAbsent(k, i));
    }
    return byKey;
  }
}
