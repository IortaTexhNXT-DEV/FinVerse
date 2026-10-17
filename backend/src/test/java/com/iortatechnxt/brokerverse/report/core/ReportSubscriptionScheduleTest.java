package com.iortatechnxt.brokerverse.report.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.report.domain.RelativePeriod;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Frequency;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Schedule;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportSubscriptionScheduleTest {

  private static ReportSubscription of(Frequency f, DayOfWeek dow, Integer dom) {
    return new ReportSubscription(
        "R", "u", new Schedule("{}", f, dow, dom, "CSV", List.of("a@b.example")));
  }

  @Test
  void weeklyAndMonthlyRunsFallOnTheirDay() {
    ReportSubscription weekly = of(Frequency.WEEKLY, DayOfWeek.MONDAY, null);
    assertThat(weekly.isDueOn(LocalDate.of(2026, 10, 12))).isTrue();
    assertThat(weekly.isDueOn(LocalDate.of(2026, 10, 13))).isFalse();
    ReportSubscription monthly = of(Frequency.MONTHLY, null, 31);
    assertThat(monthly.isDueOn(LocalDate.of(2026, 2, 28))).isTrue();
    assertThat(monthly.isDueOn(LocalDate.of(2026, 3, 30))).isFalse();
    assertThat(of(Frequency.DAILY, null, null).isDueOn(LocalDate.of(2026, 1, 1))).isTrue();
  }

  @Test
  void aScheduleNeedsItsDayAndRecipients() {
    assertThatThrownBy(() -> of(Frequency.WEEKLY, null, null))
        .extracting("code")
        .isEqualTo("DAY_REQUIRED");
    assertThatThrownBy(
            () ->
                new ReportSubscription(
                    "R", "u", new Schedule("{}", Frequency.DAILY, null, null, "CSV", List.of())))
        .extracting("code")
        .isEqualTo("RECIPIENTS_REQUIRED");
  }

  @Test
  void theDataScopeIsCheckedOnTheParameters() {
    ReportMetadata metadata =
        new ReportMetadata(
            "R",
            "Report",
            ReportCategory.GENERAL_LEDGER,
            "d",
            List.of(
                new ParameterSpec("companyId", "Company", ParameterType.COMPANY, false, null, null),
                new ParameterSpec("branchId", "Branch", ParameterType.BRANCH, false, null, null)),
            Permission.REPORT_VIEW);
    UserDataScope one = UserDataScope.of(List.of(CompanyScope.allBranchesOf(1L)));

    assertThat(ReportSubscriptionRunner.outsideScope(metadata, Map.of("companyId", "1"), one))
        .isNull();
    assertThat(ReportSubscriptionRunner.outsideScope(metadata, Map.of("companyId", "2"), one))
        .contains("outside");
    assertThat(ReportSubscriptionRunner.outsideScope(metadata, Map.of(), one))
        .contains("name a company");
    assertThat(ReportSubscriptionRunner.outsideScope(metadata, Map.of(), UserDataScope.ALL))
        .isNull();
  }

  @Test
  void theLastWorkingDayFollowsTheCalendar() {
    ReportSubscription s =
        new ReportSubscription(
            "R",
            "u",
            new Schedule(
                "{}", Frequency.LAST_WORKING_DAY, null, null, "CSV", List.of("a@b.example")));
    // 31-Oct-2026 is a Saturday: the last working day is Friday 30-Oct.
    assertThat(s.isDueOn(LocalDate.of(2026, 10, 30))).isTrue();
    assertThat(s.isDueOn(LocalDate.of(2026, 10, 31))).isFalse();
    assertThat(s.isDueOn(LocalDate.of(2026, 10, 29))).isFalse();
    // With 30-Oct a holiday, Thursday 29-Oct is the last working day.
    LocalDate holiday = LocalDate.of(2026, 10, 30);
    assertThat(
            s.isDueOn(
                LocalDate.of(2026, 10, 29),
                d -> !d.equals(holiday) && d.getDayOfWeek().getValue() <= 5))
        .isTrue();
  }

  @Test
  void theRunWaitsForItsTimeOfDay() {
    ReportSubscription s =
        new ReportSubscription(
            "R",
            "u",
            new Schedule(
                "{}",
                Frequency.DAILY,
                null,
                null,
                "CSV",
                List.of("a@b.example"),
                LocalTime.of(14, 30),
                null));
    assertThat(s.isTimeReached(LocalTime.of(14, 29))).isFalse();
    assertThat(s.isTimeReached(LocalTime.of(14, 30))).isTrue();
    assertThat(of(Frequency.DAILY, null, null).isTimeReached(LocalTime.of(5, 59))).isFalse();
  }

  @Test
  void relativePeriodsSetTheDatesOfTheRun() {
    LocalDate run = LocalDate.of(2026, 5, 14);
    assertThat(RelativePeriod.PREVIOUS_MONTH.on(run))
        .isEqualTo(new RelativePeriod.Range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)));
    assertThat(RelativePeriod.MONTH_TO_DATE.on(run).from()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(RelativePeriod.PREVIOUS_QUARTER.on(run))
        .isEqualTo(new RelativePeriod.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)));
    assertThat(RelativePeriod.QUARTER_TO_DATE.on(run).from()).isEqualTo(LocalDate.of(2026, 4, 1));
    assertThat(RelativePeriod.PREVIOUS_YEAR.on(run).to()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(RelativePeriod.YEAR_TO_DATE.on(run).from()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(RelativePeriod.PREVIOUS_DAY.on(run).from()).isEqualTo(LocalDate.of(2026, 5, 13));
    ReportMetadata metadata =
        new ReportMetadata(
            "R",
            "Report",
            ReportCategory.GENERAL_LEDGER,
            "d",
            List.of(
                new ParameterSpec("fromDate", "From", ParameterType.DATE, true, null, null),
                new ParameterSpec("toDate", "To", ParameterType.DATE, true, null, null)),
            Permission.REPORT_VIEW);
    Map<String, String> dates =
        SubscriptionCalendar.withPeriod(
            RelativePeriod.PREVIOUS_MONTH,
            metadata,
            Map.of("fromDate", "2020-01-01", "toDate", "2020-01-31"),
            run);
    assertThat(dates).containsEntry("fromDate", "2026-04-01").containsEntry("toDate", "2026-04-30");
    assertThat(SubscriptionCalendar.withPeriod(null, metadata, Map.of("a", "b"), run))
        .containsEntry("a", "b");
  }
}
