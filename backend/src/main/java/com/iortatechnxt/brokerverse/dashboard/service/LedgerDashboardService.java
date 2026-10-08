package com.iortatechnxt.brokerverse.dashboard.service;

import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardLedgerQueries.Accounts;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardLedgerQueries.PayablesDue;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardLedgerQueries.Scope;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executive dashboard widgets computed from the general ledger and the open-item sub-ledger only:
 * vendor payables and the cash position. Accounts are selected through {@link DashboardProperties},
 * so no business module is read.
 */
@Service
@Transactional(readOnly = true)
public class LedgerDashboardService {

  private static final List<String> VENDOR_TYPES =
      Arrays.stream(PartyType.values())
          .filter(t -> t.subLedger() == SubLedgerType.VENDOR)
          .map(PartyType::name)
          .toList();

  private final DashboardLedgerQueries queries;
  private final DashboardCalendar calendar;
  private final DashboardProperties properties;

  /**
   * Creates the service.
   *
   * @param queries ledger read model
   * @param calendar fiscal year and reference date
   * @param properties KPI to account mapping
   */
  public LedgerDashboardService(
      DashboardLedgerQueries queries, DashboardCalendar calendar, DashboardProperties properties) {
    this.queries = queries;
    this.calendar = calendar;
    this.properties = properties;
  }

  /**
   * Vendor payables overdue and falling due within 7 and 30 days.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param asOf reference date, null for today
   * @return payables widget
   */
  public PayablesWidget payables(Long companyId, Long branchId, LocalDate asOf) {
    LocalDate date = calendar.dateOrToday(asOf);
    PayablesDue due = queries.payablesDue(companyId, branchId, date, VENDOR_TYPES);
    return new PayablesWidget(
        date,
        Money.round(due.overdue()),
        Money.round(due.dueIn7Days()),
        Money.round(due.dueIn30Days()),
        Money.round(due.total()),
        due.openItems());
  }

  /**
   * Cash and bank position with the balance per account and the month-end trend of the year.
   *
   * @param companyId company
   * @param branchId branch, null for the whole company
   * @param asOf reference date, null for today
   * @return cash widget
   */
  public CashWidget cash(Long companyId, Long branchId, LocalDate asOf) {
    Period p = period(companyId, branchId, asOf);
    Accounts cash = Accounts.ofGroups(properties.cashGroups());
    List<LabelledAmount> accounts =
        queries.accountBalances(companyId, branchId, p.asOf(), cash).stream()
            .filter(a -> a.balance().signum() != 0)
            .map(a -> new LabelledAmount(a.code() + " " + a.name(), a.balance()))
            .toList();
    BigDecimal balance = p.net(cash, DashboardLedgerQueries.BEGINNING, p.yearStart().minusDays(1));
    Map<YearMonth, BigDecimal> movements =
        queries.monthlyNet(new Scope(companyId, branchId, p.yearStart(), p.asOf()), cash);
    List<MonthlyValue> monthly = new ArrayList<>();
    for (YearMonth m : p.months()) {
      balance = balance.add(movements.getOrDefault(m, BigDecimal.ZERO));
      monthly.add(new MonthlyValue(m.toString(), balance));
    }
    BigDecimal total =
        accounts.stream().map(LabelledAmount::amount).reduce(Money.zero(), BigDecimal::add);
    return new CashWidget(p.asOf(), total, accounts, monthly);
  }

  private Period period(Long companyId, Long branchId, LocalDate asOf) {
    LocalDate date = calendar.dateOrToday(asOf);
    return new Period(companyId, branchId, date, calendar.yearStart(companyId, date));
  }

  /** Reference date and fiscal year of one widget request, with the ledger queries it needs. */
  private final class Period {
    private final Long companyId;
    private final Long branchId;
    private final LocalDate asOf;
    private final LocalDate yearStart;

    Period(Long companyId, Long branchId, LocalDate asOf, LocalDate yearStart) {
      this.companyId = companyId;
      this.branchId = branchId;
      this.asOf = asOf;
      this.yearStart = yearStart;
    }

    LocalDate asOf() {
      return asOf;
    }

    LocalDate yearStart() {
      return yearStart;
    }

    /** Months of the fiscal year up to the reference month. */
    List<YearMonth> months() {
      List<YearMonth> months = new ArrayList<>();
      for (YearMonth m = YearMonth.from(yearStart);
          !m.isAfter(YearMonth.from(asOf));
          m = m.plusMonths(1)) {
        months.add(m);
      }
      return months;
    }

    BigDecimal net(Accounts accounts, LocalDate from, LocalDate to) {
      return Money.round(queries.net(new Scope(companyId, branchId, from, to), accounts));
    }
  }
}
