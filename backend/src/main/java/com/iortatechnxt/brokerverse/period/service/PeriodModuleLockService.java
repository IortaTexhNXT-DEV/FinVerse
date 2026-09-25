package com.iortatechnxt.brokerverse.period.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriodRepository;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLockRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cut-off of a group of books per period (FRBS 3.4.0 / 3.4.1): the broking books are closed at
 * month end, automatically by the job {@code BROKING_BOOKS_CLOSE} or by the GL Team Lead, and can
 * be reopened with a reason. Postings are refused through {@link
 * PeriodService#requirePostingPeriod(Long, java.time.LocalDate, boolean, String)}.
 */
@Service
@Transactional
public class PeriodModuleLockService {

  private static final String ENTITY = "PeriodModuleLock";

  private final PeriodModuleLockRepository locks;
  private final PeriodService periods;
  private final AccountingPeriodRepository periodRepository;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param locks cut-off records
   * @param periods periods
   * @param periodRepository period lookup by date
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PeriodModuleLockService(
      PeriodModuleLockRepository locks,
      PeriodService periods,
      AccountingPeriodRepository periodRepository,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.locks = locks;
    this.periods = periods;
    this.periodRepository = periodRepository;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Cut-off records of a company.
   *
   * @param companyId company
   * @param module group of books
   * @return records, newest period first
   */
  @Transactional(readOnly = true)
  public List<PeriodModuleLock> list(Long companyId, String module) {
    return locks.findByCompanyIdAndModuleOrderByPeriodIdDesc(companyId, module);
  }

  /**
   * Whether a group of books is closed in a period.
   *
   * @param periodId period
   * @param module group of books
   * @return true when closed
   */
  @Transactional(readOnly = true)
  public boolean isLocked(Long periodId, String module) {
    return locks.existsByPeriodIdAndModuleAndLockedTrue(periodId, module);
  }

  /**
   * Refuses a posting of a group of books into a period whose books of that group are closed (FRBS
   * 3.4.0). Dates outside any period are left to the period check of the journal.
   *
   * @param companyId company
   * @param date value date
   * @param module group of books
   */
  @Transactional(readOnly = true)
  public void requireOpen(Long companyId, LocalDate date, String module) {
    periodRepository
        .findContaining(companyId, date)
        .filter(p -> locks.existsByPeriodIdAndModuleAndLockedTrue(p.getId(), module))
        .ifPresent(
            p -> {
              throw new BusinessRuleException(
                  "BOOKS_CLOSED",
                  "The "
                      + module.toLowerCase(Locale.ROOT)
                      + " books of "
                      + p.getName()
                      + " are closed");
            });
  }

  /**
   * Closes a group of books for a period (idempotent).
   *
   * @param companyId company
   * @param periodId period
   * @param module group of books
   * @param note note, e.g. the items still pending at cut-off
   * @return record
   */
  public PeriodModuleLock lock(Long companyId, Long periodId, String module, String note) {
    AccountingPeriod period = requirePeriod(companyId, periodId);
    PeriodModuleLock lock = record(companyId, periodId, module);
    if (!lock.isLocked()) {
      lock.lock(currentUser.username(), clock.instant(), note);
      audit.record(
          ENTITY,
          module + "/" + period.getName(),
          AuditAction.CLOSE,
          "Closed the " + module + " books of " + period.getName() + note(note));
    }
    return lock;
  }

  /**
   * Reopens a group of books for a period.
   *
   * @param companyId company
   * @param periodId period
   * @param module group of books
   * @param reason mandatory reason
   * @return record
   */
  public PeriodModuleLock unlock(Long companyId, Long periodId, String module, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "REASON_REQUIRED", "Enter the reason for reopening the books");
    }
    AccountingPeriod period = requirePeriod(companyId, periodId);
    PeriodModuleLock lock = record(companyId, periodId, module);
    if (lock.isLocked()) {
      lock.unlock(currentUser.username(), clock.instant(), reason);
      audit.record(
          ENTITY,
          module + "/" + period.getName(),
          AuditAction.REOPEN,
          "Reopened the " + module + " books of " + period.getName() + note(reason));
    }
    return lock;
  }

  private PeriodModuleLock record(Long companyId, Long periodId, String module) {
    return locks
        .findByPeriodIdAndModule(periodId, module)
        .orElseGet(() -> locks.save(new PeriodModuleLock(companyId, periodId, module)));
  }

  private AccountingPeriod requirePeriod(Long companyId, Long periodId) {
    AccountingPeriod period = periods.getPeriod(periodId);
    if (!Objects.equals(period.getCompanyId(), companyId)) {
      throw new BusinessRuleException(
          "PERIOD_OF_OTHER_COMPANY", "The period belongs to another company");
    }
    return period;
  }

  private static String note(String text) {
    return text == null || text.isBlank() ? "" : ": " + text;
  }
}
