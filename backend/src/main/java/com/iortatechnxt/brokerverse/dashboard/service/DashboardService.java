package com.iortatechnxt.brokerverse.dashboard.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardSummary.CompositionItem;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardSummary.MonthlyPoint;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.report.gl.FinancialStatementService;
import com.iortatechnxt.brokerverse.report.gl.FinancialStatementService.Position;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes the executive finance dashboard from the general ledger. */
@Service
@Transactional(readOnly = true)
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardService {

  private static final int TOP_ITEMS = 6;

  private final FinancialStatementService statements;
  private final PeriodService periods;
  private final JournalBatchRepository journals;
  private final DashboardProperties properties;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param statements statement computations
   * @param periods period service
   * @param journals journal repository
   * @param properties KPI mapping
   * @param clock clock
   */
  public DashboardService(
      FinancialStatementService statements,
      PeriodService periods,
      JournalBatchRepository journals,
      DashboardProperties properties,
      Clock clock) {
    this.statements = statements;
    this.periods = periods;
    this.journals = journals;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Builds the dashboard as of today.
   *
   * @param companyId company
   * @param branchId branch or null for the whole company
   * @return summary
   */
  public DashboardSummary summary(Long companyId, Long branchId) {
    LocalDate asOf = LocalDate.now(clock);
    LocalDate yearStart = periods.yearContaining(companyId, asOf).getStartDate();
    var ytd = statements.performance(companyId, branchId, yearStart, asOf);
    BigDecimal income = FinancialStatementService.total(ytd, AccountClass.INCOME);
    BigDecimal expense = FinancialStatementService.total(ytd, AccountClass.EXPENSE);
    Position position = statements.position(companyId, branchId, asOf);
    BigDecimal equity =
        FinancialStatementService.total(position.lines(), AccountClass.EQUITY)
            .add(position.currentYearResult())
            .add(position.unclosedPriorResults());
    return new DashboardSummary(
        asOf,
        yearStart,
        income,
        expense,
        income.subtract(expense),
        sumGroups(position, AccountClass.ASSET, properties.cashGroups()),
        sumGroups(position, AccountClass.ASSET, properties.receivableGroups()),
        sumGroups(position, AccountClass.LIABILITY, properties.reserveGroups()),
        FinancialStatementService.total(position.lines(), AccountClass.ASSET),
        equity,
        journals.countByCompanyIdAndStatusIn(companyId, EnumSet.of(JournalStatus.PENDING_APPROVAL)),
        journals.countByCompanyIdAndStatusIn(
            companyId, EnumSet.of(JournalStatus.DRAFT, JournalStatus.REJECTED)),
        monthly(companyId, branchId, yearStart, asOf),
        composition(ytd, AccountClass.INCOME),
        composition(ytd, AccountClass.EXPENSE));
  }

  private List<MonthlyPoint> monthly(
      Long companyId, Long branchId, LocalDate start, LocalDate end) {
    List<MonthlyPoint> points = new ArrayList<>();
    for (YearMonth m = YearMonth.from(start);
        !m.isAfter(YearMonth.from(end));
        m = m.plusMonths(1)) {
      var lines = statements.performance(companyId, branchId, m.atDay(1), m.atEndOfMonth());
      BigDecimal income = FinancialStatementService.total(lines, AccountClass.INCOME);
      BigDecimal expense = FinancialStatementService.total(lines, AccountClass.EXPENSE);
      points.add(new MonthlyPoint(m.toString(), income, expense, income.subtract(expense)));
    }
    return points;
  }

  private static List<CompositionItem> composition(
      Map<AccountClass, Map<String, BigDecimal>> lines, AccountClass cls) {
    return lines.getOrDefault(cls, Map.of()).entrySet().stream()
        .map(e -> new CompositionItem(e.getKey(), e.getValue()))
        .sorted(Comparator.comparing((CompositionItem c) -> c.amount().abs()).reversed())
        .limit(TOP_ITEMS)
        .toList();
  }

  private static BigDecimal sumGroups(Position position, AccountClass cls, List<String> groups) {
    Map<String, BigDecimal> lines = position.lines().getOrDefault(cls, Map.of());
    return groups.stream()
        .map(g -> lines.getOrDefault(g, BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
