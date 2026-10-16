package com.iortatechnxt.brokerverse.report.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Frequency;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Schedule;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
}
