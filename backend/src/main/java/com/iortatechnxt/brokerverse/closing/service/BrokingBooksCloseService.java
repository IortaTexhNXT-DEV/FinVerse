package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodModuleLockService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic close of the broking books (FRBS 3.4.0 / 3.4.1; AQ04): on the last day of the month, at
 * {@code BROKING_CLOSE_TIME} (the cron of {@code BROKING_BOOKS_CLOSE}), the broking books of the
 * month are cut off for every company. From then on the accounting engine refuses events of the
 * broking source modules ({@code BROKING_SOURCE_MODULES}) dated in that month, while FRBS may still
 * post GL adjustments until the month-end close (FRBS 2.6.0). Pending items reported by the {@link
 * BrokingCutoffCheck} ports are recorded on the cut-off.
 */
@Service
@Transactional
public class BrokingBooksCloseService {

  /** Job name. */
  public static final String JOB_NAME = "BROKING_BOOKS_CLOSE";

  private final OrganizationService organization;
  private final PeriodService periods;
  private final PeriodModuleLockService locks;
  private final List<BrokingCutoffCheck> checks;

  /**
   * Creates the service.
   *
   * @param organization companies
   * @param periods periods
   * @param locks cut-off records
   * @param checks pending-item ports of the broking modules
   */
  public BrokingBooksCloseService(
      OrganizationService organization,
      PeriodService periods,
      PeriodModuleLockService locks,
      List<BrokingCutoffCheck> checks) {
    this.organization = organization;
    this.periods = periods;
    this.locks = locks;
    this.checks = List.copyOf(checks);
  }

  /**
   * Cuts off the broking books of the month of a date, for every company, when the date is the last
   * day of the month (the scheduled run); on any other day nothing happens.
   *
   * @param date business date
   * @return periods cut off, as "company period"
   */
  public List<String> closeMonthEnd(LocalDate date) {
    if (!date.equals(YearMonth.from(date).atEndOfMonth())) {
      return List.of();
    }
    List<String> closed = new ArrayList<>();
    for (Company company : organization.listCompanies()) {
      periods
          .findPeriod(company.getId(), date)
          .filter(p -> p.getStatus() != PeriodStatus.CLOSED)
          .filter(p -> !locks.isLocked(p.getId(), PeriodModuleLock.BROKING))
          .ifPresent(
              p -> {
                close(company.getId(), p);
                closed.add(company.getCode() + " " + p.getName());
              });
    }
    return closed;
  }

  /**
   * Cuts off the broking books of one period now (GL Team Lead).
   *
   * @param companyId company
   * @param periodId period
   * @return cut-off record
   */
  public PeriodModuleLock close(Long companyId, Long periodId) {
    return close(companyId, periods.getPeriod(periodId));
  }

  /**
   * Items still pending in the broking books of a period.
   *
   * @param companyId company
   * @param periodId period
   * @return pending items
   */
  @Transactional(readOnly = true)
  public List<String> pending(Long companyId, Long periodId) {
    AccountingPeriod period = periods.getPeriod(periodId);
    return checks.stream()
        .flatMap(
            c -> c.pendingItems(companyId, period.getStartDate(), period.getEndDate()).stream())
        .toList();
  }

  private PeriodModuleLock close(Long companyId, AccountingPeriod period) {
    List<String> pending = pending(companyId, period.getId());
    String note =
        pending.isEmpty() ? "Nothing pending" : "Pending at cut-off: " + String.join("; ", pending);
    return locks.lock(companyId, period.getId(), PeriodModuleLock.BROKING, note);
  }
}
