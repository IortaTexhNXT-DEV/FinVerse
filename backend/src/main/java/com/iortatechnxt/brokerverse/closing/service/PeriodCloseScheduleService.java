package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseSchedule;
import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseScheduleRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.domain.Holiday;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Scheduled month-end close (FRBS 2.6.0, 2.6.1; AQ04). The GL Team Lead schedules the close of the
 * previous month for a chosen date and time (default proposal: the 2nd banking day of the month,
 * 17:00 Manila time). At that time the job {@code GL_PERIOD_CLOSE} runs the period-end checklist
 * and closes the period, or records the blocking items and raises {@code GL_CLOSE_FAILED}. With
 * {@code CLOSE_ONLY_PREVIOUS_MONTH} on, only the month before the close date may be closed.
 *
 * <p>Not transactional itself: the close attempt and the recording of its outcome run in separate
 * transactions, so a refused close is still recorded.
 */
@Service
public class PeriodCloseScheduleService {

  /** Job name. */
  public static final String JOB_NAME = "GL_PERIOD_CLOSE";

  /** Alert raised when a scheduled close fails. */
  public static final String FAILED_ALERT = "GL_CLOSE_FAILED";

  /** Business time zone of BDOI. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private static final String ENTITY = "PeriodCloseSchedule";
  private static final LocalTime DEFAULT_TIME = LocalTime.of(17, 0);
  private static final int BANKING_DAY = 2;

  private final PeriodCloseScheduleRepository schedules;
  private final PeriodService periods;
  private final ClosingChecklistService checklist;
  private final OrganizationService organization;
  private final SystemParameterService parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param schedules schedules
   * @param periods periods
   * @param checklist period-end checklist
   * @param organization holidays
   * @param parameters CLOSE_ONLY_PREVIOUS_MONTH
   * @param alerts alerts
   * @param audit audit trail
   * @param clock clock
   * @param transactions transaction manager
   */
  @SuppressWarnings("java:S107") // constructor injection
  public PeriodCloseScheduleService(
      PeriodCloseScheduleRepository schedules,
      PeriodService periods,
      ClosingChecklistService checklist,
      OrganizationService organization,
      SystemParameterService parameters,
      AlertService alerts,
      AuditTrailService audit,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.schedules = schedules;
    this.periods = periods;
    this.checklist = checklist;
    this.organization = organization;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Schedules of a company.
   *
   * @param companyId company
   * @return schedules, newest first
   */
  public List<PeriodCloseSchedule> list(Long companyId) {
    return schedules.findByCompanyIdOrderByScheduledAtDescIdDesc(companyId);
  }

  /**
   * Proposed close time of a period: the 2nd banking day (no weekend, no company holiday) of the
   * following month at 17:00 Manila time (FRBS 2.6.0).
   *
   * @param companyId company
   * @param periodId period
   * @return proposed time
   */
  public Instant proposal(Long companyId, Long periodId) {
    AccountingPeriod period = requirePeriod(companyId, periodId);
    YearMonth next = YearMonth.from(period.getEndDate()).plusMonths(1);
    Set<LocalDate> holidays =
        organization.listHolidays(companyId, next.getYear()).stream()
            .filter(h -> h.getBranchId() == null)
            .map(Holiday::getHolidayDate)
            .collect(Collectors.toSet());
    LocalDate day =
        next.atDay(1)
            .datesUntil(next.atEndOfMonth().plusDays(1))
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY)
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SUNDAY)
            .filter(d -> !holidays.contains(d))
            .skip(BANKING_DAY - 1L)
            .findFirst()
            .orElse(next.atEndOfMonth());
    return day.atTime(DEFAULT_TIME).atZone(MANILA).toInstant();
  }

  /**
   * Schedules the close of a period.
   *
   * @param companyId company
   * @param periodId period
   * @param at close time, null for the proposal
   * @return schedule
   */
  public PeriodCloseSchedule schedule(Long companyId, Long periodId, Instant at) {
    Instant when = at == null ? proposal(companyId, periodId) : at;
    return tx.execute(
        s -> {
          AccountingPeriod period = requirePeriod(companyId, periodId);
          requireClosable(period, when);
          if (schedules.existsByPeriodIdAndStatus(periodId, PeriodCloseSchedule.SCHEDULED)) {
            throw new BusinessRuleException(
                "CLOSE_ALREADY_SCHEDULED", period.getName() + " already has a scheduled close");
          }
          PeriodCloseSchedule saved =
              schedules.save(new PeriodCloseSchedule(companyId, periodId, when));
          audit.record(
              ENTITY,
              period.getName(),
              AuditAction.CREATE,
              "Close of " + period.getName() + " scheduled for " + when.atZone(MANILA));
          return saved;
        });
  }

  /**
   * Withdraws a schedule.
   *
   * @param id schedule
   * @param reason reason
   * @return schedule
   */
  public PeriodCloseSchedule cancel(Long id, String reason) {
    return tx.execute(
        s -> {
          PeriodCloseSchedule schedule = get(id);
          schedule.cancel(reason);
          audit.record(ENTITY, id, AuditAction.DEACTIVATE, "Scheduled close cancelled: " + reason);
          return schedule;
        });
  }

  /**
   * Closes a period now (checklist, guard and close), recording the outcome like a scheduled close.
   *
   * @param companyId company
   * @param periodId period
   * @return executed schedule
   */
  public PeriodCloseSchedule closeNow(Long companyId, Long periodId) {
    PeriodCloseSchedule schedule = schedule(companyId, periodId, clock.instant());
    return execute(schedule.getId());
  }

  /**
   * Runs every schedule whose time has come.
   *
   * @return executed schedules
   */
  public List<PeriodCloseSchedule> runDue() {
    List<Long> due =
        schedules
            .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                PeriodCloseSchedule.SCHEDULED, clock.instant())
            .stream()
            .map(PeriodCloseSchedule::getId)
            .toList();
    return due.stream().map(this::execute).toList();
  }

  private PeriodCloseSchedule execute(Long id) {
    String failure = null;
    try {
      tx.executeWithoutResult(s -> attemptClose(id));
    } catch (BusinessRuleException ex) {
      failure = ex.getMessage();
    }
    String outcome = failure;
    return tx.execute(s -> record(id, outcome));
  }

  private void attemptClose(Long id) {
    PeriodCloseSchedule schedule = get(id);
    AccountingPeriod period = periods.getPeriod(schedule.getPeriodId());
    if (period.getStatus() == PeriodStatus.CLOSED) {
      return;
    }
    String blocking =
        checklist.periodEnd(schedule.getCompanyId(), period.getId()).stream()
            .filter(CheckItem::blocks)
            .map(c -> c.label() + ": " + c.detail())
            .collect(Collectors.joining("; "));
    if (!blocking.isEmpty()) {
      throw new BusinessRuleException("GL_CLOSE_CHECKLIST", blocking);
    }
    periods.close(period.getId());
  }

  private PeriodCloseSchedule record(Long id, String failure) {
    PeriodCloseSchedule schedule = get(id);
    AccountingPeriod period = periods.getPeriod(schedule.getPeriodId());
    schedule.executed(failure == null, clock.instant(), failure == null ? "Closed" : failure);
    audit.record(
        ENTITY,
        period.getName(),
        AuditAction.CLOSE,
        failure == null
            ? "Scheduled close of " + period.getName() + " completed"
            : "Scheduled close of " + period.getName() + " failed: " + failure);
    if (failure != null) {
      alerts.raise(
          FAILED_ALERT,
          new AlertFacts(
              schedule.getCompanyId(),
              null,
              ENTITY,
              String.valueOf(id),
              "Close of " + period.getName() + " failed: " + failure,
              null,
              FAILED_ALERT + ":" + id));
    }
    return schedule;
  }

  private void requireClosable(AccountingPeriod period, Instant when) {
    if (period.getStatus() == PeriodStatus.CLOSED || period.getStatus() == PeriodStatus.FUTURE) {
      throw new BusinessRuleException(
          "PERIOD_NOT_CLOSABLE", period.getName() + " is " + period.getStatus());
    }
    boolean previousOnly =
        Boolean.parseBoolean(parameters.text("CLOSE_ONLY_PREVIOUS_MONTH", "true").trim());
    YearMonth previous = YearMonth.from(when.atZone(MANILA)).minusMonths(1);
    if (previousOnly && !YearMonth.from(period.getEndDate()).equals(previous)) {
      throw new BusinessRuleException(
          "CLOSE_ONLY_PREVIOUS_MONTH",
          "On "
              + when.atZone(MANILA).toLocalDate()
              + " only the period of "
              + previous
              + " may be closed, not "
              + period.getName());
    }
  }

  private AccountingPeriod requirePeriod(Long companyId, Long periodId) {
    AccountingPeriod period = periods.getPeriod(periodId);
    if (!Objects.equals(period.getCompanyId(), companyId)) {
      throw new ResourceNotFoundException("Period", periodId);
    }
    return period;
  }

  private PeriodCloseSchedule get(Long id) {
    return schedules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }
}
