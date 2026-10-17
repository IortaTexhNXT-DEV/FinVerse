package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * The figures of the Renewal dashboard as predicates on its accounts (FRRN.002.02): every count and
 * amount of a widget, and the drill-down list behind it, use the same predicate. A metric key is
 * {@code WIDGET|part|part}, e.g. {@code CARD|FOR_DISPOSITION}, {@code PIPELINE|RENEWAL|FOR_SURVEY},
 * {@code AGEING|PLUS1}, {@code CLOSING|RENEWAL|BOOKED}, {@code PERSISTENCY|M1|RENEWED}.
 */
public final class DashboardMetrics {

  /** Every value of a dimension. */
  public static final String ALL = "ALL";

  private static final String EXPIRY = "expiry_date";
  private static final String SEP = "\\|";
  private static final Map<Long, String> AGEING = Map.of(0L, "CURRENT", 1L, "PLUS1", 2L, "PLUS2");

  private final DashboardFilter filter;
  private final LocalDate today;
  private final int holdCoverDays;

  /**
   * Creates the metrics of a request.
   *
   * @param filter filters with the period
   * @param today business date
   * @param holdCoverDays days of the Expiring Hold Cover card
   */
  public DashboardMetrics(DashboardFilter filter, LocalDate today, int holdCoverDays) {
    this.filter = filter;
    this.today = today;
    this.holdCoverDays = holdCoverDays;
  }

  /**
   * The predicate of a metric key.
   *
   * @param metric metric key
   * @return predicate (never matches for an unknown key)
   */
  public Predicate<DashboardItem> of(String metric) {
    String[] p = (metric == null ? "" : metric).split(SEP);
    String part1 = p.length > 1 ? p[1] : ALL;
    String part2 = p.length > 2 ? p[2] : ALL;
    BiFunction<String, String, Predicate<DashboardItem>> widget = widgets().get(p[0]);
    return widget == null ? i -> false : widget.apply(part1, part2);
  }

  private Map<String, BiFunction<String, String, Predicate<DashboardItem>>> widgets() {
    Map<String, BiFunction<String, String, Predicate<DashboardItem>>> w = new HashMap<>();
    w.put("PRODUCTION", (a, b) -> this::booked);
    w.put("MIX", (a, b) -> i -> booked(i) && (ALL.equals(a) || a.equals(line(i))));
    w.put("PIPELINE", (a, b) -> i -> pipeline(i, a, b));
    w.put("AGEING", (a, b) -> i -> ageing(i, a));
    w.put("CLOSING", (a, b) -> i -> closing(i, a, b));
    w.put("PERSISTENCY", (a, b) -> i -> persistency(i, a, b));
    w.put("INSURER", (a, b) -> i -> insurer(i, a, b));
    w.put("INSURER_PENDING", (a, b) -> i -> pendingInsurer(i) && a.equals(i.text("insurer_code")));
    w.put("CARD", (a, b) -> CardRules.card(a, this));
    w.put("TOP", (a, b) -> this::openDeal);
    w.put("WORKLOAD", (a, b) -> i -> i.renewal() && i.open() && a.equals(i.text("assigned_user")));
    return w;
  }

  /**
   * The open renewals of the period ranked by premium, highest first (Biggest Open Deals).
   *
   * @param items items
   * @param size 50, 75 or 100
   * @return the deals
   */
  public List<DashboardItem> top(List<DashboardItem> items, int size) {
    return items.stream()
        .filter(this::openDeal)
        .sorted(
            Comparator.comparing((DashboardItem i) -> i.amount("premium"))
                .reversed()
                .thenComparing(i -> Objects.toString(i.text("ref"), "")))
        .limit(size)
        .toList();
  }

  boolean expiringInPeriod(DashboardItem i) {
    return i.renewal() && filter.inPeriod(i.date(EXPIRY));
  }

  boolean booked(DashboardItem i) {
    return i.renewal()
        && "BOOKED".equals(i.text("account_status"))
        && filter.inPeriod(i.date("booked_at"));
  }

  boolean openDeal(DashboardItem i) {
    return expiringInPeriod(i) && i.open();
  }

  private boolean inPipelinePeriod(DashboardItem i) {
    return !i.renewal() || filter.inPeriod(i.date(EXPIRY)) || "DEFERRED".equals(i.get("category"));
  }

  private boolean pipeline(DashboardItem i, String category, String stage) {
    Object pstage = i.get("pstage");
    boolean inCategory = ALL.equals(category) || category.equals(i.get("category"));
    boolean inStage = ALL.equals(stage) || stage.equals(pstage);
    return pstage != null && inPipelinePeriod(i) && inCategory && inStage;
  }

  private boolean closing(DashboardItem i, String category, String tier) {
    boolean inPeriod = !i.renewal() || filter.inPeriod(i.date(EXPIRY));
    return inPeriod
        && (ALL.equals(category) || category.equals(i.get("category")))
        && (ALL.equals(tier) || tier.equals(i.get("tier")));
  }

  /**
   * The ageing bucket of an outstanding renewal (not yet renewed or booked) by its expiry month
   * against the current month: PRIOR, CURRENT, PLUS1, PLUS2; null when later or not outstanding.
   *
   * @param i item
   * @return bucket
   */
  String ageingBucket(DashboardItem i) {
    LocalDate expiry = i.date(EXPIRY);
    if (!i.renewal() || !i.open() || expiry == null) {
      return null;
    }
    long months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(expiry));
    return months < 0 ? "PRIOR" : AGEING.get(months);
  }

  private boolean ageing(DashboardItem i, String bucket) {
    String b = ageingBucket(i);
    return b != null && (ALL.equals(bucket) || bucket.equals(b));
  }

  /**
   * The month of a persistency column: M0 the month of the end of the period, M1 to M6 the months
   * before; YTD from January of that year to that month.
   *
   * @param key M0 to M6 or YTD
   * @param expiry expiry date
   * @return whether the expiry falls in the column
   */
  boolean persistencyPeriod(String key, LocalDate expiry) {
    if (expiry == null) {
      return false;
    }
    YearMonth ref = YearMonth.from(filter.to());
    YearMonth m = YearMonth.from(expiry);
    if ("YTD".equals(key)) {
      return m.getYear() == ref.getYear() && !m.isAfter(ref);
    }
    int back = Integer.parseInt(key.substring(1));
    return m.equals(ref.minusMonths(back));
  }

  private boolean persistency(DashboardItem i, String key, String what) {
    boolean renewable =
        i.renewal()
            && !"NON_RENEWABLE_ACCOUNT".equals(i.text("nonrenewal_reason"))
            && persistencyPeriod(key, i.date(EXPIRY));
    return renewable && (!"RENEWED".equals(what) || "RENEWED".equals(i.stage()));
  }

  /**
   * The row of an account in the Insurer Renewal Approval Monitoring: its market segment, or its
   * unit when a segment is selected.
   *
   * @param i item
   * @return group
   */
  String insurerGroup(DashboardItem i) {
    String g = filter.segment() == null ? i.text("segment") : i.text("team");
    return g == null ? "-" : g;
  }

  boolean renewAsIs(DashboardItem i) {
    return expiringInPeriod(i) && "FOR_RENEWAL".equals(i.text("disposition"));
  }

  boolean pendingInsurer(DashboardItem i) {
    return renewAsIs(i) && i.text("insurer_response") == null;
  }

  private boolean insurer(DashboardItem i, String group, String what) {
    if (!renewAsIs(i) || !(ALL.equals(group) || group.equals(insurerGroup(i)))) {
      return false;
    }
    String r = i.text("insurer_response");
    return switch (what) {
      case "APPROVED" -> "RENEW_AS_IS".equals(r);
      case "PENDING" -> r == null;
      case "RETURNED" -> r != null && !"RENEW_AS_IS".equals(r);
      default -> true;
    };
  }

  /**
   * The product line of an account.
   *
   * @param i item
   * @return line code
   */
  static String line(DashboardItem i) {
    String l = i.text("line_code");
    return l == null ? "-" : l;
  }

  DashboardFilter filter() {
    return filter;
  }

  LocalDate today() {
    return today;
  }

  int holdCoverDays() {
    return holdCoverDays;
  }

  /**
   * The sum of an amount over the items.
   *
   * @param items items
   * @param name amount column
   * @return sum
   */
  public static BigDecimal sum(List<DashboardItem> items, String name) {
    return items.stream().map(i -> i.amount(name)).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
