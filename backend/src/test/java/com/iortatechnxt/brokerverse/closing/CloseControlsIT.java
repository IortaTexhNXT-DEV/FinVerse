package com.iortatechnxt.brokerverse.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.alert.domain.AlertRepository;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck.AlertSignal;
import com.iortatechnxt.brokerverse.closing.domain.PeriodCloseSchedule;
import com.iortatechnxt.brokerverse.closing.service.BrokingBooksCloseJob;
import com.iortatechnxt.brokerverse.closing.service.BrokingBooksCloseService;
import com.iortatechnxt.brokerverse.closing.service.GlPeriodCloseJob;
import com.iortatechnxt.brokerverse.closing.service.PeriodCloseScheduleService;
import com.iortatechnxt.brokerverse.closing.service.YearEndCloseDueCheck;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodModuleLockService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FRBS close controls: scheduled month-end close with the previous-month guard (FRBS 2.6.0 /
 * 2.6.1), cut-off of the broking books (FRBS 3.4.0 / 3.4.1) and the year-end deadline (FRBS 2.7.0).
 */
@IntegrationTest
class CloseControlsIT {

  @Autowired private PeriodCloseScheduleService schedules;
  @Autowired private GlPeriodCloseJob closeJob;
  @Autowired private BrokingBooksCloseService brokingBooks;
  @Autowired private BrokingBooksCloseJob brokingJob;
  @Autowired private PeriodModuleLockService locks;
  @Autowired private PeriodService periods;
  @Autowired private YearEndCloseDueCheck yearEndDue;
  @Autowired private AccountingEventPublisher publisher;
  @Autowired private JournalEntryService journals;
  @Autowired private AlertRepository alerts;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser as;

  private static YearMonth previousMonth() {
    return YearMonth.from(Instant.now().atZone(PeriodCloseScheduleService.MANILA)).minusMonths(1);
  }

  private Long companyWithOpenYears(String code) {
    Long company = companies.create(code, "PHP").getId();
    YearMonth previous = previousMonth();
    companies.openYear(company, previous.getYear());
    if (previous.getYear() != LocalDate.now().getYear()) {
      companies.openYear(company, LocalDate.now().getYear());
    }
    return company;
  }

  @Test
  void scheduledCloseOfThePreviousMonthRunsThroughTheJob() {
    Long company = companyWithOpenYears("TGLC");
    AccountingPeriod previous = companies.period(company, previousMonth().atDay(1));
    PeriodCloseSchedule schedule =
        as.run(
            "gltl",
            () -> schedules.schedule(company, previous.getId(), Instant.now().minusSeconds(60)));
    assertThat(schedule.getStatus()).isEqualTo(PeriodCloseSchedule.SCHEDULED);
    assertThatThrownBy(
            () ->
                as.run("gltl", () -> schedules.schedule(company, previous.getId(), Instant.now())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already has a scheduled close");

    var outcome = as.run("gltl", () -> closeJob.execute(LocalDate.now()));
    assertThat(outcome.itemsProcessed()).isGreaterThanOrEqualTo(1);
    assertThat(schedules.list(company).get(0).getStatus()).isEqualTo(PeriodCloseSchedule.COMPLETED);
    assertThat(periods.getPeriod(previous.getId()).getStatus()).isEqualTo(PeriodStatus.CLOSED);

    AccountingPeriod current = companies.period(company, LocalDate.now());
    assertThatThrownBy(() -> as.run("gltl", () -> schedules.closeNow(company, current.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("may be closed");
  }

  @Test
  void refusedCloseIsRecordedAndRaisesTheAlert() {
    Long company = companyWithOpenYears("TGLF");
    LocalDate lastDay = previousMonth().atEndOfMonth();
    AccountingPeriod previous = companies.period(company, lastDay);
    Long branch = companies.headOffice(company);
    BigDecimal amount = new BigDecimal("75.00");
    JournalBatch draft =
        as.run(
            "glofficer",
            () ->
                journals.createDraft(
                    new JournalRequest(
                        company,
                        branch,
                        JournalType.MANUAL,
                        lastDay,
                        "PHP",
                        "Pending at close",
                        null,
                        List.of(
                            new JournalLineRequest(
                                "5603",
                                BalanceSide.DEBIT,
                                amount,
                                null,
                                null,
                                null,
                                "FIN",
                                null,
                                null,
                                null,
                                null),
                            new JournalLineRequest(
                                "1111",
                                BalanceSide.CREDIT,
                                amount,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)))));
    as.run("glofficer", () -> journals.submit(draft.getId()));

    PeriodCloseSchedule failed =
        as.run("gltl", () -> schedules.closeNow(company, previous.getId()));
    assertThat(failed.getStatus()).isEqualTo(PeriodCloseSchedule.FAILED);
    assertThat(failed.getResult()).isNotBlank();
    assertThat(periods.getPeriod(previous.getId()).getStatus()).isNotEqualTo(PeriodStatus.CLOSED);
    assertThat(alerts.findAll())
        .anyMatch(
            a ->
                PeriodCloseScheduleService.FAILED_ALERT.equals(a.getExceptionCode())
                    && String.valueOf(failed.getId()).equals(a.getEntityId()));
  }

  @Test
  void closedBrokingBooksRefuseBrokingEventsOnly() {
    Long company = companies.create("TGLB", "PHP").getId();
    companies.openYear(company, 2030);
    LocalDate date = LocalDate.of(2030, 1, 20);
    AccountingPeriod january = companies.period(company, date);
    Long branch = companies.headOffice(company);

    assertThat(brokingJob.execute(LocalDate.of(2030, 1, 30)).itemsProcessed()).isZero();
    List<String> closed =
        as.run("gltl", () -> brokingBooks.closeMonthEnd(LocalDate.of(2030, 1, 31)));
    assertThat(closed).contains("TGLB 2030-01");
    assertThat(locks.isLocked(january.getId(), PeriodModuleLock.BROKING)).isTrue();

    assertThatThrownBy(
            () ->
                as.run(
                    "accountant", () -> publisher.publish(event(company, branch, date, "BOOKING"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("broking books of 2030-01 are closed");
    assertThatThrownBy(
            () -> periods.requirePostingPeriod(company, date, true, PeriodModuleLock.BROKING))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(periods.requirePostingPeriod(company, date, true, null).getId())
        .isEqualTo(january.getId());
    assertThat(errorCode(() -> publisher.publish(event(company, branch, date, "UNDERWRITING"))))
        .isNotEqualTo("BOOKS_CLOSED");

    as.run(
        "gltl",
        () -> locks.unlock(company, january.getId(), PeriodModuleLock.BROKING, "Late bookings"));
    assertThat(errorCode(() -> publisher.publish(event(company, branch, date, "BOOKING"))))
        .isNotEqualTo("BOOKS_CLOSED");
  }

  @Test
  void openPreviousYearRaisesTheYearEndDeadlineAlert() {
    Long company = companies.create("TGLY", "PHP").getId();
    companies.openYear(company, 2029);
    assertThat(yearEndDue.evaluate(LocalDate.of(2030, 3, 1))).isEmpty();
    List<AlertSignal> signals = yearEndDue.evaluate(LocalDate.of(2030, 4, 10));
    assertThat(signals)
        .anyMatch(
            s ->
                company.equals(s.facts().companyId())
                    && "2029".equals(s.facts().entityId())
                    && YearEndCloseDueCheck.CODE.equals(s.code()));
    assertThat(yearEndDue.deadline().toString()).isEqualTo("--04-15");
  }

  private String errorCode(Runnable action) {
    try {
      as.run(
          "accountant",
          () -> {
            action.run();
            return null;
          });
      return "";
    } catch (BusinessRuleException ex) {
      return ex.getCode();
    }
  }

  private static BusinessEvent event(Long company, Long branch, LocalDate date, String module) {
    String key = UUID.randomUUID().toString();
    return new BusinessEvent(
        "POLICY_ISSUE",
        company,
        branch,
        date,
        "PHP",
        module,
        key,
        "POL-" + key,
        null,
        "FIRE",
        null,
        "Cut-off test",
        Map.of("GROSS_PREMIUM", new BigDecimal("100.00"), "TOTAL_DUE", new BigDecimal("100.00")),
        Map.of());
  }
}
