package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The KPI summary cards of the Renewal dashboard (FRRN.002.02.09) and the expiry cards of the
 * Renewal Home (FRRN.002.02): key, label and the accounts each counts. The cards Bounced Check,
 * Unapplied Payment and Refund count Cashiering transactions of the renewal accounts ({@link
 * RenewalCashFacts}).
 */
public final class CardRules {

  /** Card keys and labels in display order. */
  public static final List<String[]> CARDS =
      List.of(
          new String[] {"TOTAL_EXPIRING", "Total Expiring"},
          new String[] {"FOR_DISPOSITION", "For Disposition"},
          new String[] {"FOR_RENEWAL", "For Renewal"},
          new String[] {"FOR_QUOTATION_PROPOSAL", "For Quotation / Proposal"},
          new String[] {"NOT_FOR_RENEWAL", "Not for Renewal"},
          new String[] {"RENEWED", "Renewed"},
          new String[] {"UNRENEWED", "Unrenewed"},
          new String[] {"RETURNED", "Returned Accounts"},
          new String[] {"CANCELLED", "Cancelled"},
          new String[] {"ADJUSTMENTS", "Adjustments"},
          new String[] {"BOUNCED_CHECK", "Bounced Check"},
          new String[] {"UNAPPLIED_PAYMENT", "Unapplied Payment"},
          new String[] {"REFUND", "Refund"},
          new String[] {"EXPIRING_HOLD_COVER", "Expiring Hold Cover"},
          new String[] {"EXPIRING_30", "Expiring within 30 days"},
          new String[] {"EXPIRING_60", "Expiring within 60 days"},
          new String[] {"EXPIRING_90", "Expiring within 90 days"},
          new String[] {"EXPIRING_140", "Expiring within 140 days"});

  /** Cards whose figures are Cashiering transactions. */
  public static final Set<String> CASH = Set.of("BOUNCED_CHECK", "UNAPPLIED_PAYMENT", "REFUND");

  private static final String DISPOSITION = "disposition";
  private static final String EXPIRING = "EXPIRING_";
  private static final String HOLD_COVER = "EXPIRING_HOLD_COVER";
  private static final Map<String, Function<DashboardMetrics, Predicate<DashboardItem>>> RULES =
      rules();

  private CardRules() {}

  /**
   * The accounts a card counts.
   *
   * @param key card key
   * @param m metrics of the request
   * @return predicate
   */
  static Predicate<DashboardItem> card(String key, DashboardMetrics m) {
    if (key.startsWith(EXPIRING) && !HOLD_COVER.equals(key)) {
      int days = Integer.parseInt(key.substring(EXPIRING.length()));
      return i -> i.renewal() && i.open() && within(i.date("expiry_date"), m.today(), days);
    }
    Function<DashboardMetrics, Predicate<DashboardItem>> card = RULES.get(key);
    return card == null ? i -> false : card.apply(m);
  }

  private static boolean disposed(DashboardItem i, String... codes) {
    String d = i.text(DISPOSITION);
    return d != null && List.of(codes).contains(d);
  }

  private static Map<String, Function<DashboardMetrics, Predicate<DashboardItem>>> rules() {
    Map<String, Function<DashboardMetrics, Predicate<DashboardItem>>> r = new HashMap<>();
    r.put("TOTAL_EXPIRING", m -> m::expiringInPeriod);
    r.put("FOR_DISPOSITION", m -> i -> m.openDeal(i) && i.text(DISPOSITION) == null);
    r.put("FOR_RENEWAL", m -> i -> m.expiringInPeriod(i) && disposed(i, "FOR_RENEWAL"));
    r.put(
        "FOR_QUOTATION_PROPOSAL",
        m -> i -> m.expiringInPeriod(i) && disposed(i, "FOR_QUOTATION", "FOR_PROPOSAL"));
    r.put("NOT_FOR_RENEWAL", m -> i -> m.expiringInPeriod(i) && disposed(i, "NOT_FOR_RENEWAL"));
    statusRules(r);
    return r;
  }

  private static void statusRules(
      Map<String, Function<DashboardMetrics, Predicate<DashboardItem>>> r) {
    r.put("RENEWED", m -> i -> m.expiringInPeriod(i) && "RENEWED".equals(i.stage()));
    r.put("UNRENEWED", m -> m::openDeal);
    r.put("RETURNED", m -> i -> i.renewal() && i.open() && i.flag("returned"));
    r.put("CANCELLED", m -> i -> m.expiringInPeriod(i) && cancelled(i));
    r.put("ADJUSTMENTS", m -> i -> m.expiringInPeriod(i) && i.text("adjustment_types") != null);
    r.put(
        HOLD_COVER,
        m ->
            i ->
                i.renewal()
                    && "CONFIRMED".equals(i.text("hold_cover_status"))
                    && within(i.date("hold_cover_until"), m.today(), m.holdCoverDays()));
  }

  private static boolean cancelled(DashboardItem i) {
    return "CANCELLED".equals(i.text("account_status"))
        || "CANCELED_POLICY".equals(i.text("nonrenewal_reason"));
  }

  private static boolean within(LocalDate date, LocalDate today, int days) {
    return date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(days));
  }
}
