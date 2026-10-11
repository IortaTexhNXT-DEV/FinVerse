package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.budget.service.RenewalBudgetService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.renewal.service.RenewalStatusNames;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Renewal dashboard (BDOI Renewal FRS FRRN.002.02): production against the Annual Renewal
 * Budget, the pipeline, the outstanding renewal accounts by ageing, the closing ratio, the biggest
 * open deals, the renewal persistency, the product mix, the insurer renewal approvals, the KPI
 * cards and the workload per officer, within the user's data scope and the filters. Each figure is
 * a metric key whose drill-down list ({@link DashboardDrillService}) holds the accounts it counts.
 */
@Service
@Transactional(readOnly = true)
public class RenewalDashboardService {

  /** Parameter: days of the Expiring Hold Cover card. */
  public static final String HOLD_COVER_DAYS = "RNW_HOLD_COVER_EXPIRING_DAYS";

  /** Parameter: sizes of the Biggest Open Deals. */
  public static final String TOP_OPTIONS = "RNW_TOP_DEALS_OPTIONS";

  private static final int DEFAULT_HOLD_COVER_DAYS = 7;
  private static final int DEFAULT_TOP = 50;
  private static final int PERSISTENCY_MONTHS = 6;
  private static final String PREMIUM = "premium";
  private static final String TOTAL = "TOTAL";
  private static final String COMMISSION = "commission";

  private final DashboardItems reader;
  private final PipelineRules pipeline;
  private final RenewalCashFacts cash;
  private final RenewalBudgetService budgets;
  private final SystemParameterService parameters;
  private final RenewalStatusNames statusNames;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reader dashboard accounts
   * @param pipeline pipeline rules
   * @param cash Cashiering transactions
   * @param budgets renewal budget
   * @param parameters system parameters
   * @param statusNames status names
   * @param clock clock
   */
  public RenewalDashboardService(
      DashboardItems reader,
      PipelineRules pipeline,
      RenewalCashFacts cash,
      RenewalBudgetService budgets,
      SystemParameterService parameters,
      RenewalStatusNames statusNames,
      Clock clock) {
    this.reader = reader;
    this.pipeline = pipeline;
    this.cash = cash;
    this.budgets = budgets;
    this.parameters = parameters;
    this.statusNames = statusNames;
    this.clock = clock;
  }

  /**
   * The dashboard.
   *
   * @param filter filters
   * @param topSize size of the Biggest Open Deals (50, 75 or 100)
   * @return figures
   */
  public Dashboard dashboard(DashboardFilter filter, Integer topSize) {
    Context x = context(filter);
    List<DashboardItem> items = x.items();
    DashboardMetrics m = x.metrics();
    int size = topSize == null ? DEFAULT_TOP : topSize;
    return new Dashboard(
        x.filter(),
        production(x),
        pipelineRows(items, m),
        ageing(items, m),
        closing(items, m),
        m.top(items, size).stream().map(RenewalDashboardService::deal).toList(),
        topOptions(),
        persistency(items, m),
        productMix(items, m),
        insurer(items, m),
        insurerPending(items, m),
        cards(items, m, x.filter()),
        workload(items));
  }

  /**
   * The accounts and metrics of a request (shared with the drill-down).
   *
   * @param filter filters
   * @return context
   */
  public Context context(DashboardFilter filter) {
    LocalDate today = BusinessClock.today(clock);
    DashboardFilter f = filter.completed(today);
    List<DashboardItem> items = new ArrayList<>(reader.renewals(f, today));
    items.addAll(reader.newBusiness(f));
    pipeline.classify(items, today);
    items.forEach(i -> label(i));
    DashboardMetrics m =
        new DashboardMetrics(
            f, today, parameters.intValue(HOLD_COVER_DAYS, DEFAULT_HOLD_COVER_DAYS));
    return new Context(f, items, m);
  }

  private void label(DashboardItem i) {
    if (i.renewal()) {
      String placement = i.text("placement_status");
      i.put(
          "status",
          statusNames.of(
              i.stage(),
              i.flag("returned"),
              placement == null ? i.text("account_status") : placement));
    } else {
      i.put("status", i.stage());
    }
  }

  private List<Integer> topOptions() {
    List<Integer> options =
        parameters.items(TOP_OPTIONS).stream()
            .map(String::strip)
            .filter(v -> v.matches("\\d{1,4}"))
            .map(Integer::valueOf)
            .toList();
    return options.isEmpty() ? List.of(DEFAULT_TOP) : options;
  }

  private List<Production> production(Context x) {
    DashboardFilter f = x.filter();
    List<DashboardItem> booked = filter(x.items(), x.metrics().of("PRODUCTION"));
    DashboardFilter prev = f.previousYear();
    DashboardMetrics pm = new DashboardMetrics(prev, x.metrics().today(), 0);
    List<DashboardItem> previous =
        filter(reader.renewals(prev, x.metrics().today()), pm.of("PRODUCTION"));
    List<Production> rows = new ArrayList<>();
    for (Measure measure :
        List.of(
            new Measure(RenewalBudgetService.PREMIUM, "Basic Premium", PREMIUM, "adjustment_basic"),
            new Measure(
                RenewalBudgetService.COMMISSION,
                "Gross Commission",
                COMMISSION,
                "adjustment_commission"))) {
      BigDecimal actual = DashboardMetrics.sum(booked, measure.amount());
      BigDecimal budget =
          budgets.budgetOf(
              f.companyId(),
              new RenewalBudgetService.BudgetQuery(
                  measure.code(),
                  f.from(),
                  f.to(),
                  f.segment(),
                  f.officer(),
                  RenewalBudget.Month::renewalAmount));
      rows.add(
          new Production(
              measure.code(),
              measure.label(),
              actual,
              budget,
              DashboardMath.percent(actual, budget),
              DashboardMath.variance(actual, budget),
              DashboardMath.growth(DashboardMetrics.sum(previous, measure.amount()), budget),
              DashboardMetrics.sum(booked, measure.adjustment())));
    }
    return rows;
  }

  private static Pipeline pipelineRows(List<DashboardItem> items, DashboardMetrics m) {
    List<PipelineRow> rows = new ArrayList<>();
    for (String category : PipelineRules.CATEGORIES) {
      rows.add(pipelineRow(category, items, m));
    }
    return new Pipeline(rows, pipelineRow(DashboardMetrics.ALL, items, m));
  }

  private static PipelineRow pipelineRow(
      String category, List<DashboardItem> items, DashboardMetrics m) {
    List<Cell> cells = new ArrayList<>();
    for (String stage : PipelineRules.STAGES) {
      cells.add(cell(stage, "PIPELINE|" + category + "|" + stage, items, m));
    }
    return new PipelineRow(category, cells, cell(TOTAL, "PIPELINE|" + category + "|ALL", items, m));
  }

  private static Cell cell(
      String key, String metric, List<DashboardItem> items, DashboardMetrics m) {
    List<DashboardItem> in = filter(items, m.of(metric));
    return new Cell(
        key,
        metric,
        in.size(),
        DashboardMetrics.sum(in, PREMIUM),
        DashboardMetrics.sum(in, COMMISSION));
  }

  private static List<Cell> ageing(List<DashboardItem> items, DashboardMetrics m) {
    List<Cell> cells = new ArrayList<>();
    for (String b : List.of("PRIOR", "CURRENT", "PLUS1", "PLUS2")) {
      cells.add(cell(b, "AGEING|" + b, items, m));
    }
    cells.add(cell(TOTAL, "AGEING|ALL", items, m));
    return cells;
  }

  private static List<Closing> closing(List<DashboardItem> items, DashboardMetrics m) {
    List<Closing> rows = new ArrayList<>();
    List<String> categories = new ArrayList<>(PipelineRules.CATEGORIES);
    categories.add(DashboardMetrics.ALL);
    for (String c : categories) {
      String base = "CLOSING|" + c + "|";
      long booked = count(items, m.of(base + PipelineRules.BOOKED));
      long total = count(items, m.of(base + DashboardMetrics.ALL));
      rows.add(
          new Closing(
              c,
              count(items, m.of(base + PipelineRules.IN_PROCESS)),
              count(items, m.of(base + PipelineRules.POSTED)),
              booked,
              total,
              DashboardMath.percent(booked, total)));
    }
    return rows;
  }

  private static Deal deal(DashboardItem i) {
    return new Deal(
        i.text("arn") == null ? i.text("expiring_arn") : i.text("arn"),
        i.text("ref"),
        i.text("assured"),
        i.text("product_code"),
        "Renewal",
        i.text("team"),
        i.amount(PREMIUM),
        i.amount(COMMISSION));
  }

  private static List<Persistency> persistency(List<DashboardItem> items, DashboardMetrics m) {
    List<Persistency> rows = new ArrayList<>();
    List<String> keys = new ArrayList<>();
    for (int k = 0; k <= PERSISTENCY_MONTHS; k++) {
      keys.add("M" + k);
    }
    keys.add("YTD");
    YearMonth ref = YearMonth.from(m.filter().to());
    for (String key : keys) {
      List<DashboardItem> base = filter(items, m.of("PERSISTENCY|" + key + "|ALL"));
      List<DashboardItem> renewed = filter(items, m.of("PERSISTENCY|" + key + "|RENEWED"));
      String label =
          "YTD".equals(key)
              ? "YTD " + ref.getYear()
              : ref.minusMonths(Integer.parseInt(key.substring(1))).toString();
      rows.add(
          new Persistency(
              key,
              label,
              base.size(),
              renewed.size(),
              DashboardMath.percent(renewed.size(), base.size()),
              DashboardMath.percent(
                  DashboardMetrics.sum(renewed, PREMIUM), DashboardMetrics.sum(base, PREMIUM)),
              DashboardMath.percent(
                  DashboardMetrics.sum(renewed, COMMISSION),
                  DashboardMetrics.sum(base, COMMISSION))));
    }
    return rows;
  }

  private static List<Cell> productMix(List<DashboardItem> items, DashboardMetrics m) {
    Map<String, String> lines = new LinkedHashMap<>();
    filter(items, m.of("PRODUCTION")).stream()
        .sorted(Comparator.comparing(DashboardMetrics::line))
        .forEach(
            i ->
                lines.putIfAbsent(
                    DashboardMetrics.line(i),
                    i.text("line_name") == null ? DashboardMetrics.line(i) : i.text("line_name")));
    List<Cell> cells = new ArrayList<>();
    for (Map.Entry<String, String> line : lines.entrySet()) {
      Cell c = cell(line.getKey(), "MIX|" + line.getKey(), items, m);
      cells.add(new Cell(line.getValue(), c.metric(), c.count(), c.premium(), c.commission()));
    }
    cells.add(cell(TOTAL, "MIX|ALL", items, m));
    return cells;
  }

  private static List<InsurerRow> insurer(List<DashboardItem> items, DashboardMetrics m) {
    Map<String, Boolean> groups = new LinkedHashMap<>();
    items.stream()
        .filter(m::renewAsIs)
        .map(m::insurerGroup)
        .sorted()
        .forEach(g -> groups.put(g, true));
    List<InsurerRow> rows = new ArrayList<>();
    List<String> keys = new ArrayList<>(groups.keySet());
    keys.add(DashboardMetrics.ALL);
    for (String g : keys) {
      String base = "INSURER|" + g + "|";
      long total = count(items, m.of(base + TOTAL));
      long approved = count(items, m.of(base + "APPROVED"));
      rows.add(
          new InsurerRow(
              g,
              total,
              approved,
              count(items, m.of(base + "PENDING")),
              count(items, m.of(base + "RETURNED")),
              DashboardMath.percent(approved, total)));
    }
    return rows;
  }

  private static List<Count> insurerPending(List<DashboardItem> items, DashboardMetrics m) {
    Map<String, Long> byInsurer = new LinkedHashMap<>();
    items.stream()
        .filter(m::pendingInsurer)
        .map(i -> i.text("insurer_code") == null ? "-" : i.text("insurer_code"))
        .sorted()
        .forEach(code -> byInsurer.merge(code, 1L, Long::sum));
    return byInsurer.entrySet().stream()
        .map(e -> new Count(e.getKey(), "INSURER_PENDING|" + e.getKey(), e.getValue()))
        .toList();
  }

  private List<Count> cards(List<DashboardItem> items, DashboardMetrics m, DashboardFilter f) {
    List<DashboardItem> period = filter(items, m::expiringInPeriod);
    Map<String, Long> cashCounts = cash.counts(f.companyId(), period);
    List<Count> cards = new ArrayList<>();
    for (String[] card : CardRules.CARDS) {
      long n =
          CardRules.CASH.contains(card[0])
              ? cashCounts.getOrDefault(card[0], 0L)
              : count(items, m.of("CARD|" + card[0]));
      cards.add(new Count(card[0], "CARD|" + card[0], n));
    }
    return cards;
  }

  private static List<Count> workload(List<DashboardItem> items) {
    Map<String, Long> byUser = new LinkedHashMap<>();
    items.stream()
        .filter(i -> i.renewal() && i.open() && i.text("assigned_user") != null)
        .map(i -> i.text("assigned_user"))
        .sorted()
        .forEach(u -> byUser.merge(u, 1L, Long::sum));
    return byUser.entrySet().stream()
        .map(e -> new Count(e.getKey(), "WORKLOAD|" + e.getKey(), e.getValue()))
        .toList();
  }

  static List<DashboardItem> filter(List<DashboardItem> items, Predicate<DashboardItem> p) {
    return items.stream().filter(p).toList();
  }

  private static long count(List<DashboardItem> items, Predicate<DashboardItem> p) {
    return items.stream().filter(p).count();
  }

  /** A production measure: budget code, label, amount and adjustment columns. */
  private record Measure(String code, String label, String amount, String adjustment) {}

  /**
   * The accounts and metrics of one request.
   *
   * @param filter completed filters
   * @param items accounts
   * @param metrics metrics
   */
  public record Context(
      DashboardFilter filter, List<DashboardItem> items, DashboardMetrics metrics) {}

  /**
   * The Renewal dashboard.
   *
   * @param filter filters applied
   * @param production basic premium and gross commission against budget
   * @param pipeline pipeline summary
   * @param ageing outstanding renewal accounts by ageing
   * @param closing closing ratio per business type
   * @param topDeals biggest open deals
   * @param topOptions sizes of the ranking
   * @param persistency renewal persistency
   * @param productMix production per product line
   * @param insurer insurer renewal approval monitoring
   * @param insurerPending pending accounts per insurer
   * @param cards KPI cards
   * @param workload open renewal accounts per officer
   */
  public record Dashboard(
      DashboardFilter filter,
      List<Production> production,
      Pipeline pipeline,
      List<Cell> ageing,
      List<Closing> closing,
      List<Deal> topDeals,
      List<Integer> topOptions,
      List<Persistency> persistency,
      List<Cell> productMix,
      List<InsurerRow> insurer,
      List<Count> insurerPending,
      List<Count> cards,
      List<Count> workload) {}

  /**
   * A production measure.
   *
   * @param measure PREMIUM or COMMISSION
   * @param label Basic Premium or Gross Commission
   * @param actual actual of the period
   * @param budget budget of the period
   * @param percentVsBudget actual over budget times 100
   * @param variance actual minus budget
   * @param growth previous year's actual over the budget times 100
   * @param adjustments sum of the adjusted amounts
   */
  public record Production(
      String measure,
      String label,
      BigDecimal actual,
      BigDecimal budget,
      BigDecimal percentVsBudget,
      BigDecimal variance,
      BigDecimal growth,
      BigDecimal adjustments) {}

  /**
   * A count with premium and commission.
   *
   * @param key stage, bucket or line
   * @param metric metric key of the drill-down
   * @param count accounts
   * @param premium sum of premium
   * @param commission sum of commission
   */
  public record Cell(
      String key, String metric, long count, BigDecimal premium, BigDecimal commission) {}

  /**
   * A business type of the pipeline.
   *
   * @param category business type
   * @param cells one per stage
   * @param total all stages
   */
  public record PipelineRow(String category, List<Cell> cells, Cell total) {}

  /**
   * The pipeline summary.
   *
   * @param rows one per business type
   * @param total all business types
   */
  public record Pipeline(List<PipelineRow> rows, PipelineRow total) {}

  /**
   * Closing ratio of a business type.
   *
   * @param category business type or ALL
   * @param inProcess Tier 3
   * @param posted Tier 2
   * @param booked Tier 1
   * @param total all accounts
   * @param ratio booked over total times 100
   */
  public record Closing(
      String category, long inProcess, long posted, long booked, long total, BigDecimal ratio) {}

  /**
   * An open deal.
   *
   * @param accountNo account reference
   * @param renewalRef renewal reference (opens the account)
   * @param assured assured's name
   * @param riskCode risk code
   * @param businessType business type
   * @param team team
   * @param premium premium
   * @param commission commission
   */
  public record Deal(
      String accountNo,
      String renewalRef,
      String assured,
      String riskCode,
      String businessType,
      String team,
      BigDecimal premium,
      BigDecimal commission) {}

  /**
   * Renewal persistency of a month or the year to date.
   *
   * @param key M0 to M6 or YTD
   * @param label month or year
   * @param renewable renewable accounts
   * @param renewed renewed accounts
   * @param countRatio by count
   * @param premiumRatio by premium
   * @param commissionRatio by commission
   */
  public record Persistency(
      String key,
      String label,
      long renewable,
      long renewed,
      BigDecimal countRatio,
      BigDecimal premiumRatio,
      BigDecimal commissionRatio) {}

  /**
   * A row of the insurer renewal approval monitoring.
   *
   * @param group market segment, or unit when a segment is selected
   * @param totalRenewAsIs accounts For Renewal
   * @param approved insurer approved
   * @param pending without insurer disposition
   * @param returned insurer disposition other than approved
   * @param approvalRatio approved over total times 100
   */
  public record InsurerRow(
      String group,
      long totalRenewAsIs,
      long approved,
      long pending,
      long returned,
      BigDecimal approvalRatio) {}

  /**
   * A count with its drill-down.
   *
   * @param key card, insurer or officer
   * @param metric metric key
   * @param count count
   */
  public record Count(String key, String metric, long count) {}
}
