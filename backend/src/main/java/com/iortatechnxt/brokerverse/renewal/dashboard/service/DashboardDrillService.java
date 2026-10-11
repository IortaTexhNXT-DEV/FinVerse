package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The drill-down of every figure of the Renewal dashboard (FRRN.002.02): the accounts a metric
 * counts with the columns of the marketing drill-down (reference, invoice and expiring invoice
 * number, assured, product line, risk code, premium, commission, renewal status, assigned user,
 * insurer disposition and remarks) and the columns of the card (returned date and reason,
 * cancellation date and reason, adjustment type, check details, refund details, hold cover end).
 */
@Service
@Transactional(readOnly = true)
public class DashboardDrillService {

  private static final String DATE = "DATE";
  private static final String AMOUNT = "AMOUNT";
  private static final String TEXT = "TEXT";
  private static final String AMOUNT_KEY = "amount";
  private static final Map<String, String> ADJUSTMENT_TYPES =
      Map.of("FINANCIAL", "Financial", "NON_FINANCIAL", "Non-financial", "INTERNAL", "Internal");

  private final RenewalDashboardService dashboard;
  private final RenewalCashFacts cash;

  /**
   * Creates the service.
   *
   * @param dashboard dashboard (accounts and metrics)
   * @param cash Cashiering transactions
   */
  public DashboardDrillService(RenewalDashboardService dashboard, RenewalCashFacts cash) {
    this.dashboard = dashboard;
    this.cash = cash;
  }

  /**
   * The accounts of a metric.
   *
   * @param filter filters of the dashboard
   * @param metric metric key
   * @param topSize size of the Biggest Open Deals
   * @return columns and rows
   */
  public Drill drill(DashboardFilter filter, String metric, Integer topSize) {
    RenewalDashboardService.Context x = dashboard.context(filter);
    String card = metric.startsWith("CARD|") ? metric.substring("CARD|".length()) : "";
    if (CardRules.CASH.contains(card)) {
      return cashDrill(x, card);
    }
    List<DashboardItem> items =
        metric.startsWith("TOP")
            ? x.metrics().top(x.items(), topSize == null ? DashboardServiceDefaults.TOP : topSize)
            : RenewalDashboardService.filter(x.items(), x.metrics().of(metric));
    List<Column> extra = extraColumns(metric, card);
    List<Row> rows = new ArrayList<>();
    int rank = 0;
    for (DashboardItem i : items) {
      rank++;
      Map<String, Object> values = new LinkedHashMap<>();
      for (Column c : extra) {
        values.put(c.key(), value(c, i, rank));
      }
      rows.add(row(i, values));
    }
    return new Drill(metric, extra, rows);
  }

  private Drill cashDrill(RenewalDashboardService.Context x, String card) {
    List<DashboardItem> period =
        RenewalDashboardService.filter(x.items(), x.metrics()::expiringInPeriod);
    List<Column> extra =
        switch (card) {
          case "BOUNCED_CHECK" ->
              List.of(
                  new Column("checkNo", "Check Number", TEXT, "check_no"),
                  new Column("bank", "Bank Name", TEXT, "bank"),
                  new Column("txnDate", "Bounced Date", DATE, "txn_date"),
                  new Column(AMOUNT_KEY, "Amount", AMOUNT, AMOUNT_KEY));
          case "UNAPPLIED_PAYMENT" ->
              List.of(
                  new Column("txnRef", "Transaction Reference", TEXT, "txn_ref"),
                  new Column("txnDate", "Payment Date", DATE, "txn_date"),
                  new Column(AMOUNT_KEY, "Amount", AMOUNT, AMOUNT_KEY),
                  new Column("balance", "Unapplied Balance", AMOUNT, "balance"));
          default ->
              List.of(
                  new Column("txnRef", "Refund Request", TEXT, "txn_ref"),
                  new Column(AMOUNT_KEY, "Amount", AMOUNT, AMOUNT_KEY),
                  new Column("txnDate", "Refund Date", DATE, "txn_date"),
                  new Column("category", "Category", TEXT, "category"));
        };
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> t : cash.transactions(x.filter().companyId(), card, period)) {
      Map<String, Object> values = new LinkedHashMap<>();
      DashboardItem txn = new DashboardItem(t);
      extra.forEach(
          c ->
              values.put(
                  c.key(), DATE.equals(c.kind()) ? txn.date(c.source()) : t.get(c.source())));
      rows.add(row((DashboardItem) t.get("item"), values));
    }
    return new Drill("CARD|" + card, extra, rows);
  }

  private static List<Column> extraColumns(String metric, String card) {
    List<Column> extra = new ArrayList<>();
    if (metric.startsWith("TOP")) {
      extra.add(new Column("rank", "Ranking", "NUMBER", "rank"));
      extra.add(new Column("team", "Team", TEXT, "team"));
    }
    switch (card) {
      case "RETURNED" -> {
        extra.add(new Column("returnedOn", "Returned Date", DATE, "returned_at"));
        extra.add(new Column("returnReason", "Return Reason", TEXT, "return_reason_name"));
        extra.add(new Column("returnRemarks", "Remarks", TEXT, "return_remarks"));
      }
      case "CANCELLED" -> {
        extra.add(new Column("cancelledOn", "Cancellation Date", DATE, "cancelled_at"));
        extra.add(new Column("cancelReason", "Cancellation Reason", TEXT, "cancellation_reason"));
      }
      case "ADJUSTMENTS" ->
          extra.add(new Column("adjustmentType", "Adjustment Type", TEXT, "adjustment_types"));
      case "EXPIRING_HOLD_COVER" ->
          extra.add(new Column("holdCoverUntil", "Hold Cover Until", DATE, "hold_cover_until"));
      default -> {
        // the marketing drill-down columns only
      }
    }
    if (metric.startsWith("PRODUCTION") || metric.startsWith("MIX")) {
      extra.add(new Column("bookedOn", "Booking Date", DATE, "booked_at"));
    }
    return extra;
  }

  private static Object value(Column c, DashboardItem i, int rank) {
    return switch (c.key()) {
      case "rank" -> rank;
      case "adjustmentType" -> adjustmentLabel(i.get(c.source()));
      default -> DATE.equals(c.kind()) ? i.date(c.source()) : i.get(c.source());
    };
  }

  private static Row row(DashboardItem i, Map<String, Object> extra) {
    return new Row(
        i.text("ref"),
        i.renewal() ? "Renewal" : "New Business",
        i.text("arn"),
        i.text("invoice_no"),
        i.text("expiring_invoice_no"),
        i.text("assured"),
        i.text("line_name") == null ? i.text("line_code") : i.text("line_name"),
        i.text("product_code"),
        i.amount("premium"),
        i.amount("commission"),
        i.text("status"),
        i.text("assigned_user"),
        insurerDisposition(i.text("insurer_response")),
        i.text("insurer_remarks"),
        i.date("expiry_date"),
        extra);
  }

  private static Object adjustmentLabel(Object v) {
    if (v == null) {
      return null;
    }
    List<String> labels = new ArrayList<>();
    for (String c : v.toString().split(",")) {
      labels.add(ADJUSTMENT_TYPES.getOrDefault(c, c));
    }
    return String.join(", ", labels);
  }

  /**
   * The insurer disposition as users read it.
   *
   * @param response RENEW_AS_IS, REVISE, REJECT or null
   * @return label or null
   */
  static String insurerDisposition(String response) {
    if (response == null) {
      return null;
    }
    return switch (response) {
      case "RENEW_AS_IS" -> "Approved";
      case "REVISE" -> "Approved with Revision";
      case "REJECT" -> "Rejected";
      default -> response;
    };
  }

  /**
   * A column of a drill-down beyond the marketing columns.
   *
   * @param key value key
   * @param label header
   * @param kind TEXT, DATE, AMOUNT or NUMBER
   * @param source item column
   */
  public record Column(String key, String label, String kind, String source) {}

  /**
   * An account of a drill-down.
   *
   * @param ref reference number (renewal reference or account reference)
   * @param businessType Renewal or New Business
   * @param arn account reference of the renewal account
   * @param invoiceNo invoice number
   * @param expiringInvoiceNo expiring invoice number
   * @param assured assured's name
   * @param productLine product line
   * @param riskCode risk code
   * @param premium premium
   * @param commission commission
   * @param status renewal status
   * @param assignedUser assigned user
   * @param insurerDisposition insurer disposition
   * @param insurerRemarks insurer remarks
   * @param expiryDate expiry date
   * @param extra values of the extra columns
   */
  public record Row(
      String ref,
      String businessType,
      String arn,
      String invoiceNo,
      String expiringInvoiceNo,
      String assured,
      String productLine,
      String riskCode,
      BigDecimal premium,
      BigDecimal commission,
      String status,
      String assignedUser,
      String insurerDisposition,
      String insurerRemarks,
      LocalDate expiryDate,
      Map<String, Object> extra) {}

  /**
   * A drill-down list.
   *
   * @param metric metric key
   * @param columns extra columns
   * @param rows accounts
   */
  public record Drill(String metric, List<Column> columns, List<Row> rows) {

    /**
     * The total premium of the rows (equals the figure of the metric).
     *
     * @return sum
     */
    public BigDecimal totalPremium() {
      return rows.stream().map(Row::premium).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }

  /** Defaults shared with the dashboard. */
  static final class DashboardServiceDefaults {
    static final int TOP = 50;

    private DashboardServiceDefaults() {}
  }
}
