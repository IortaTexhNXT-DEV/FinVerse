package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.report.core.ReportSubscriptionRunner;
import com.iortatechnxt.brokerverse.report.core.ReportSubscriptionService;
import com.iortatechnxt.brokerverse.report.core.ReportSubscriptionService.Request;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Frequency;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Schedule;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun.Outcome;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Report subscriptions (G04): subscribe, scheduled runs as the subscriber, run log. */
@IntegrationTest
class ReportSubscriptionIT {

  private static final String REPORT = "GL-COA";

  @Autowired private ReportSubscriptionService service;
  @Autowired private ReportSubscriptionRunner runner;
  @Autowired private ReportSubscriptionRepository repository;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Request daily() {
    return new Request(
        Map.of("companyId", String.valueOf(data.company().getId())),
        Frequency.DAILY,
        null,
        null,
        ExportFormat.CSV,
        List.of("finance@subscriber.example"));
  }

  @Test
  void aSubscriptionRunsOncePerDayAsItsSubscriber() {
    ReportSubscription s = as.run("comptrol", () -> service.subscribe(REPORT, daily()));
    LocalDate date = LocalDate.of(2026, 3, 2);

    assertThat(runner.runOne(s.getId(), date)).isEqualTo(Outcome.SUCCEEDED);
    runner.execute(date);
    List<ReportSubscriptionRun> runs = as.run("comptrol", () -> service.runs(s.getId()));
    assertThat(runs).hasSize(1);
    assertThat(runs.get(0).getReportRunId()).isNotNull();
    assertThat(runs.get(0).getMessageId()).isNotNull();

    assertThatThrownBy(() -> as.run("cashier", () -> service.runs(s.getId())))
        .hasMessageContaining("Report subscription");
    as.run("comptrol", () -> service.deactivate(s.getId()));
    assertThat(repository.findById(s.getId()).orElseThrow().isDueOn(date.plusDays(1))).isFalse();
  }

  @Test
  void aSubscriberWithoutTheReportPermissionIsDenied() {
    ReportSubscription s =
        repository.save(
            new ReportSubscription(
                REPORT,
                "norole",
                new Schedule(
                    "{}", Frequency.DAILY, null, null, "CSV", List.of("x@subscriber.example"))));
    assertThat(runner.runOne(s.getId(), LocalDate.of(2026, 3, 3))).isEqualTo(Outcome.DENIED);
  }

  @Test
  void subscribingNeedsTheExportPermission() {
    assertThatThrownBy(() -> as.run("norole", () -> service.subscribe(REPORT, daily())))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }
}
