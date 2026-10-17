package com.iortatechnxt.brokerverse.period.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriodRepository;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.domain.FiscalYearRepository;
import com.iortatechnxt.brokerverse.period.domain.FiscalYearStatus;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLockRepository;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Financial calendar: fiscal years, monthly periods and their open/close lifecycle.
 *
 * <p>Also answers the key posting question "may this journal post on this date?".
 */
@Service
@Transactional
public class PeriodService {

  private static final int MONTHS = 12;
  private static final String PERIOD = "AccountingPeriod";

  private final FiscalYearRepository years;
  private final AccountingPeriodRepository periods;
  private final PeriodModuleLockRepository moduleLocks;
  private final OrganizationService organization;
  private final List<PeriodCloseGuard> closeGuards;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param years fiscal year repository
   * @param periods period repository
   * @param moduleLocks cut-off of groups of books (FRBS 3.4.0)
   * @param organization organization service
   * @param closeGuards pre-close checks contributed by other modules
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PeriodService(
      FiscalYearRepository years,
      AccountingPeriodRepository periods,
      PeriodModuleLockRepository moduleLocks,
      OrganizationService organization,
      List<PeriodCloseGuard> closeGuards,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.years = years;
    this.periods = periods;
    this.moduleLocks = moduleLocks;
    this.organization = organization;
    this.closeGuards = List.copyOf(closeGuards);
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists fiscal years of a company.
   *
   * @param companyId company
   * @return years, newest first
   */
  @Transactional(readOnly = true)
  public List<FiscalYear> listYears(Long companyId) {
    return years.findByCompanyIdOrderByYearCodeDesc(companyId);
  }

  /**
   * Lists the periods of a fiscal year.
   *
   * @param fiscalYearId year
   * @return periods
   */
  @Transactional(readOnly = true)
  public List<AccountingPeriod> listPeriods(Long fiscalYearId) {
    return periods.findByFiscalYearIdOrderByPeriodNo(fiscalYearId);
  }

  /**
   * Gets a fiscal year.
   *
   * @param id id
   * @return year
   */
  @Transactional(readOnly = true)
  public FiscalYear getYear(Long id) {
    return years.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fiscal year", id));
  }

  /**
   * Gets a period.
   *
   * @param id id
   * @return period
   */
  @Transactional(readOnly = true)
  public AccountingPeriod getPeriod(Long id) {
    return periods.findById(id).orElseThrow(() -> new ResourceNotFoundException("Period", id));
  }

  /**
   * The period containing a date, if one is defined.
   *
   * @param companyId company
   * @param date date
   * @return period
   */
  @Transactional(readOnly = true)
  public Optional<AccountingPeriod> findPeriod(Long companyId, LocalDate date) {
    return periods.findContaining(companyId, date);
  }

  /**
   * Finds the fiscal year containing a date.
   *
   * @param companyId company
   * @param date date
   * @return fiscal year
   */
  @Transactional(readOnly = true)
  public FiscalYear yearContaining(Long companyId, LocalDate date) {
    return years
        .findContaining(companyId, date)
        .orElseThrow(
            () ->
                new BusinessRuleException("NO_FISCAL_YEAR", "No fiscal year defined for " + date));
  }

  /**
   * Creates a fiscal year with twelve monthly periods, all in FUTURE status.
   *
   * @param companyId company
   * @param yearCode year in which the fiscal year starts
   * @return fiscal year
   */
  public FiscalYear createFiscalYear(Long companyId, int yearCode) {
    Company company = organization.getCompany(companyId);
    if (years.existsByCompanyIdAndYearCode(companyId, yearCode)) {
      throw new DuplicateResourceException("Fiscal year", yearCode);
    }
    YearMonth first = YearMonth.of(yearCode, company.getFiscalYearStartMonth());
    FiscalYear year =
        years.save(
            new FiscalYear(
                companyId, yearCode, first.atDay(1), first.plusMonths(MONTHS - 1L).atEndOfMonth()));
    for (int i = 0; i < MONTHS; i++) {
      YearMonth ym = first.plusMonths(i);
      periods.save(
          new AccountingPeriod(year, i + 1, ym.toString(), ym.atDay(1), ym.atEndOfMonth()));
    }
    audit.record("FiscalYear", yearCode, AuditAction.CREATE, "Created fiscal year " + yearCode);
    return year;
  }

  /**
   * Opens a FUTURE period (or re-opens a CLOSING period for normal posting).
   *
   * @param periodId period
   * @return period
   */
  public AccountingPeriod open(Long periodId) {
    return transition(periodId, PeriodStatus.OPEN, null, AuditAction.OPEN);
  }

  /**
   * Starts closing a period (soft close): only system and adjustment journals may post.
   *
   * @param periodId period
   * @return period
   */
  public AccountingPeriod startClosing(Long periodId) {
    return transition(periodId, PeriodStatus.CLOSING, null, AuditAction.CLOSE);
  }

  /**
   * Hard-closes a period after every registered close guard passes.
   *
   * @param periodId period
   * @return period
   */
  public AccountingPeriod close(Long periodId) {
    AccountingPeriod period = getPeriod(periodId);
    List<String> issues =
        closeGuards.stream().flatMap(g -> g.blockingIssues(period).stream()).toList();
    if (!issues.isEmpty()) {
      throw new BusinessRuleException("PERIOD_CLOSE_BLOCKED", String.join("; ", issues));
    }
    return transition(periodId, PeriodStatus.CLOSED, null, AuditAction.CLOSE);
  }

  /**
   * Reopens a closed period for authorized adjustments.
   *
   * @param periodId period
   * @param reason mandatory justification
   * @return period
   */
  public AccountingPeriod reopen(Long periodId, String reason) {
    AccountingPeriod period = getPeriod(periodId);
    if (period.getFiscalYear().getStatus() == FiscalYearStatus.CLOSED) {
      throw new BusinessRuleException(
          "YEAR_CLOSED", "Periods of a closed fiscal year cannot be reopened");
    }
    return transition(periodId, PeriodStatus.REOPENED, reason, AuditAction.REOPEN);
  }

  /**
   * Year-close hook used by year-end closing ({@code closing.service.YearEndService}) after the
   * closing journals are posted: marks the fiscal year CLOSED. Every period must already be CLOSED,
   * so no further posting into the year is possible.
   *
   * @param fiscalYearId year
   * @return closed year
   */
  public FiscalYear closeFiscalYear(Long fiscalYearId) {
    FiscalYear year = getYear(fiscalYearId);
    if (year.getStatus() == FiscalYearStatus.CLOSED) {
      throw new BusinessRuleException(
          "YEAR_ALREADY_CLOSED", "Fiscal year " + year.getYearCode() + " is already closed");
    }
    boolean allClosed =
        periods.findByFiscalYearIdOrderByPeriodNo(fiscalYearId).stream()
            .allMatch(p -> p.getStatus() == PeriodStatus.CLOSED);
    if (!allClosed) {
      throw new BusinessRuleException(
          "YEAR_PERIODS_OPEN",
          "All periods of fiscal year " + year.getYearCode() + " must be closed first");
    }
    year.close(currentUser.username(), clock.instant());
    audit.record(
        "FiscalYear",
        year.getYearCode(),
        AuditAction.CLOSE,
        "Closed fiscal year " + year.getYearCode());
    return year;
  }

  /**
   * Returns the period that a journal will post into, validating that posting is permitted.
   *
   * @param companyId company
   * @param date accounting (value) date
   * @param systemOrAdjustment true for system generated or adjustment journals
   * @return the period
   */
  @Transactional(readOnly = true)
  public AccountingPeriod requirePostingPeriod(
      Long companyId, LocalDate date, boolean systemOrAdjustment) {
    AccountingPeriod period =
        periods
            .findContaining(companyId, date)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "NO_PERIOD", "No accounting period is defined for " + date));
    if (!period.acceptsPosting(systemOrAdjustment)) {
      throw new BusinessRuleException(
          "PERIOD_NOT_OPEN",
          "Period "
              + period.getName()
              + " is "
              + period.getStatus()
              + " and does not accept this posting");
    }
    return period;
  }

  /**
   * As {@link #requirePostingPeriod(Long, LocalDate, boolean)}, and also refuses the posting when
   * the books of a group of modules are closed for the period (FRBS 3.4.0: the broking books are
   * cut off at month end while the GL stays open for adjustments). Existing callers are unchanged.
   *
   * @param companyId company
   * @param date accounting (value) date
   * @param systemOrAdjustment true for system generated or adjustment journals
   * @param module group of books, e.g. {@link PeriodModuleLock#BROKING}; null skips the check
   * @return the period
   */
  @Transactional(readOnly = true)
  public AccountingPeriod requirePostingPeriod(
      Long companyId, LocalDate date, boolean systemOrAdjustment, String module) {
    AccountingPeriod period = requirePostingPeriod(companyId, date, systemOrAdjustment);
    if (module != null
        && moduleLocks.existsByPeriodIdAndModuleAndLockedTrue(period.getId(), module)) {
      throw new BusinessRuleException(
          "BOOKS_CLOSED",
          "The "
              + module.toLowerCase(Locale.ROOT)
              + " books of "
              + period.getName()
              + " are closed");
    }
    return period;
  }

  private AccountingPeriod transition(
      Long periodId, PeriodStatus target, String reason, AuditAction action) {
    AccountingPeriod period = getPeriod(periodId);
    period.transition(target, currentUser.username(), clock.instant(), reason);
    audit.record(
        PERIOD,
        period.getName(),
        action,
        "Period " + period.getName() + " set to " + target + (reason == null ? "" : ": " + reason));
    return period;
  }
}
