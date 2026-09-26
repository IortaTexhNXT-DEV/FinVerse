package com.iortatechnxt.brokerverse.closing.api;

import com.iortatechnxt.brokerverse.closing.api.dto.BooksCutoffRequest;
import com.iortatechnxt.brokerverse.closing.api.dto.BooksCutoffResponse;
import com.iortatechnxt.brokerverse.closing.api.dto.CloseScheduleRequest;
import com.iortatechnxt.brokerverse.closing.api.dto.CloseScheduleResponse;
import com.iortatechnxt.brokerverse.closing.api.dto.YearEndCloseResponse;
import com.iortatechnxt.brokerverse.closing.api.dto.YearEndRequest;
import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseSchedule;
import com.iortatechnxt.brokerverse.closing.service.BrokingBooksCloseService;
import com.iortatechnxt.brokerverse.closing.service.PeriodCloseScheduleService;
import com.iortatechnxt.brokerverse.closing.service.YearEndCloseDueCheck;
import com.iortatechnxt.brokerverse.closing.service.YearEndService;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.service.PeriodModuleLockService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * FRBS close controls: scheduled month-end close (FRBS 2.6.0 / 2.6.1), cut-off of the broking books
 * (FRBS 3.4.0 / 3.4.1) and the year-end verification and deadline (FRBS 2.7.0 / 2.7.1).
 */
@RestController
@RequestMapping("/api/v1/closing")
public class CloseScheduleController {

  private static final String VIEW =
      "hasAnyAuthority('GL_CLOSE_SCHEDULE','PERIOD_END_RUN','PERIOD_MANAGE','YEAR_END_CLOSE')";
  private static final String SCHEDULE = "hasAuthority('GL_CLOSE_SCHEDULE')";

  private final PeriodCloseScheduleService schedules;
  private final BrokingBooksCloseService brokingBooks;
  private final PeriodModuleLockService locks;
  private final PeriodService periods;
  private final YearEndService yearEnd;
  private final YearEndCloseDueCheck deadline;
  private final SystemParameterService parameters;

  /**
   * Creates the controller.
   *
   * @param schedules scheduled closes
   * @param brokingBooks broking books cut-off
   * @param locks cut-off records
   * @param periods periods (names)
   * @param yearEnd year-end close
   * @param deadline year-end deadline
   * @param parameters close parameters
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CloseScheduleController(
      PeriodCloseScheduleService schedules,
      BrokingBooksCloseService brokingBooks,
      PeriodModuleLockService locks,
      PeriodService periods,
      YearEndService yearEnd,
      YearEndCloseDueCheck deadline,
      SystemParameterService parameters) {
    this.schedules = schedules;
    this.brokingBooks = brokingBooks;
    this.locks = locks;
    this.periods = periods;
    this.yearEnd = yearEnd;
    this.deadline = deadline;
    this.parameters = parameters;
  }

  /**
   * Close settings shown on the Planning &amp; Closing screen (AQ04).
   *
   * @return CLOSE_ONLY_PREVIOUS_MONTH, BROKING_CLOSE_TIME and the year-end deadline
   */
  @GetMapping("/settings")
  @PreAuthorize(VIEW)
  public Map<String, String> settings() {
    return Map.of(
        "closeOnlyPreviousMonth", parameters.text("CLOSE_ONLY_PREVIOUS_MONTH", "true"),
        "brokingCloseTime", parameters.text("BROKING_CLOSE_TIME", "23:00"),
        "yearEndDeadline", deadline.deadline().toString().substring(2));
  }

  /**
   * Scheduled closes of a company.
   *
   * @param companyId company
   * @return schedules, newest first
   */
  @GetMapping("/close-schedules")
  @PreAuthorize(VIEW)
  public List<CloseScheduleResponse> schedules(@RequestParam Long companyId) {
    return schedules.list(companyId).stream().map(this::view).toList();
  }

  /**
   * Proposed close time of a period (2nd banking day of the next month, 17:00 Manila).
   *
   * @param companyId company
   * @param periodId period
   * @return {"scheduledAt": time}
   */
  @GetMapping("/close-schedules/proposal")
  @PreAuthorize(VIEW)
  public Map<String, Instant> proposal(@RequestParam Long companyId, @RequestParam Long periodId) {
    return Map.of("scheduledAt", schedules.proposal(companyId, periodId));
  }

  /**
   * Schedules a month-end close.
   *
   * @param request period and time
   * @return schedule
   */
  @PostMapping("/close-schedules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(SCHEDULE)
  public CloseScheduleResponse schedule(@Valid @RequestBody CloseScheduleRequest request) {
    return view(schedules.schedule(request.companyId(), request.periodId(), request.scheduledAt()));
  }

  /**
   * Withdraws a scheduled close.
   *
   * @param id schedule
   * @param request reason
   * @return schedule
   */
  @PostMapping("/close-schedules/{id}/cancel")
  @PreAuthorize(SCHEDULE)
  public CloseScheduleResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return view(schedules.cancel(id, request.reason()));
  }

  /**
   * Closes a period now through the checklist (same guard and record as a scheduled close).
   *
   * @param request period
   * @return executed schedule (COMPLETED or FAILED with the blocking items)
   */
  @PostMapping("/close-now")
  @PreAuthorize(SCHEDULE)
  public CloseScheduleResponse closeNow(@Valid @RequestBody CloseScheduleRequest request) {
    return view(schedules.closeNow(request.companyId(), request.periodId()));
  }

  /**
   * Cut-off status of the broking books.
   *
   * @param companyId company
   * @return cut-off records, newest period first
   */
  @GetMapping("/broking-books")
  @PreAuthorize(VIEW)
  public List<BooksCutoffResponse> brokingBooks(@RequestParam Long companyId) {
    return locks.list(companyId, PeriodModuleLock.BROKING).stream().map(this::view).toList();
  }

  /**
   * Items still pending in the broking books of a period.
   *
   * @param companyId company
   * @param periodId period
   * @return pending items
   */
  @GetMapping("/broking-books/pending")
  @PreAuthorize(VIEW)
  public List<String> pending(@RequestParam Long companyId, @RequestParam Long periodId) {
    return brokingBooks.pending(companyId, periodId);
  }

  /**
   * Closes the broking books of a period now.
   *
   * @param request period
   * @return cut-off record
   */
  @PostMapping("/broking-books/close")
  @PreAuthorize(SCHEDULE)
  public BooksCutoffResponse closeBrokingBooks(@Valid @RequestBody BooksCutoffRequest request) {
    return view(brokingBooks.close(request.companyId(), request.periodId()));
  }

  /**
   * Reopens the broking books of a period.
   *
   * @param request period and reason
   * @return cut-off record
   */
  @PostMapping("/broking-books/reopen")
  @PreAuthorize(SCHEDULE)
  public BooksCutoffResponse reopenBrokingBooks(@Valid @RequestBody BooksCutoffRequest request) {
    return view(
        locks.unlock(
            request.companyId(), request.periodId(), PeriodModuleLock.BROKING, request.reason()));
  }

  /**
   * Verifies a closed fiscal year again (FRBS 2.7.1).
   *
   * @param request fiscal year
   * @return close record with the verification
   */
  @PostMapping("/year-end/verify")
  @PreAuthorize("hasAnyAuthority('YEAR_END_CLOSE','GL_CLOSE_SCHEDULE')")
  public YearEndCloseResponse verify(@Valid @RequestBody YearEndRequest request) {
    return YearEndCloseResponse.from(yearEnd.verify(request.fiscalYearId()));
  }

  private CloseScheduleResponse view(PeriodCloseSchedule s) {
    return CloseScheduleResponse.from(s, periods.getPeriod(s.getPeriodId()).getName());
  }

  private BooksCutoffResponse view(PeriodModuleLock l) {
    return BooksCutoffResponse.from(l, periods.getPeriod(l.getPeriodId()).getName());
  }
}
