package com.iortatechnxt.brokerverse.alert;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.put;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.alert.domain.Alert;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.AlertSeverity;
import com.iortatechnxt.brokerverse.alert.domain.AlertStatus;
import com.iortatechnxt.brokerverse.alert.service.AlertDailyJob;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.alert.service.AlertService.AlertSearch;
import com.iortatechnxt.brokerverse.alert.service.AlertService.CodeSettings;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.JournalFixtures;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.domain.JobRunStatus;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRegistry;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AlertRulesIT {

  private static final BigDecimal ALERT_AMOUNT = new BigDecimal("5000.00");

  @Autowired private AlertService alerts;
  @Autowired private JobRegistry jobs;
  @Autowired private JobRunService jobRuns;
  @Autowired private JournalFixtures journals;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  private List<Alert> alerts(String code, AlertStatus status) {
    return alerts
        .search(
            new AlertSearch(status, null, code, null, Instant.EPOCH, Instant.now().plusSeconds(60)),
            Pageable.ofSize(500))
        .getContent();
  }

  private List<Alert> alertsFor(String code, String entityId) {
    return alerts(code, null).stream().filter(a -> entityId.equals(a.getEntityId())).toList();
  }

  @Test
  void postingRulesRaiseLargeBackDatedAndWeekendAlerts() {
    JournalBatch large =
        journals.posted(journals.request("5603", "1111", "1500000.00", LocalDate.now()));
    assertThat(alertsFor("LARGE_JOURNAL", large.getBatchNo()))
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.getSeverity()).isEqualTo(AlertSeverity.HIGH);
              assertThat(a.getAmount()).isEqualByComparingTo("1500000.00");
              assertThat(a.getCompanyId()).isEqualTo(data.company().getId());
            });

    JournalBatch backDated =
        journals.posted(journals.request("5603", "1111", "10.00", LocalDate.now().minusDays(10)));
    assertThat(alertsFor("BACK_DATED_POSTING", backDated.getBatchNo())).hasSize(1);

    LocalDate saturday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SATURDAY));
    JournalBatch weekend = journals.posted(journals.request("5603", "1111", "10.00", saturday));
    assertThat(alertsFor("WEEKEND_POSTING", weekend.getBatchNo())).hasSize(1);
    assertThat(alertsFor("LARGE_JOURNAL", weekend.getBatchNo())).isEmpty();
  }

  @Test
  void dailyChecksAreDeduplicatedAndReRaisedAfterResolution() {
    // Other test classes post to these accounts too: size the journal so that, whatever their
    // balances are, petty cash ends in credit and the suspense account is not cleared.
    LocalDate today = LocalDate.now();
    BigDecimal amount = journals.balance("1102", today).max(BigDecimal.ZERO).add(ALERT_AMOUNT);
    if (journals.balance("1606", today).add(amount).signum() == 0) {
      amount = amount.add(BigDecimal.ONE);
    }
    journals.posted(journals.request("1606", "1102", amount.toPlainString(), today));
    var run = jobs.run(AlertDailyJob.JOB_NAME, JobTrigger.MANUAL);
    assertThat(run.getStatus()).isEqualTo(JobRunStatus.SUCCEEDED);
    Alert suspense = liveAlert("SUSPENSE_BALANCE", "1606");
    Alert cash = liveAlert("NEGATIVE_CASH_BALANCE", "1102");
    assertThat(cash.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);

    jobs.run(AlertDailyJob.JOB_NAME, JobTrigger.MANUAL);
    assertThat(alertsFor("SUSPENSE_BALANCE", "1606").stream().filter(this::live)).hasSize(1);

    Alert acknowledged = as.run("checker", () -> alerts.acknowledge(suspense.getId(), "Looking"));
    assertThat(acknowledged.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThatThrownBy(() -> as.run("checker", () -> alerts.acknowledge(suspense.getId(), null)))
        .extracting("code")
        .isEqualTo("ALERT_NOT_OPEN");
    Alert resolved = as.run("checker", () -> alerts.resolve(suspense.getId(), "Cleared"));
    assertThat(resolved.getResolvedBy()).isEqualTo("checker");
    assertThatThrownBy(() -> as.run("checker", () -> alerts.resolve(suspense.getId(), "again")))
        .extracting("code")
        .isEqualTo("ALERT_RESOLVED");

    jobs.run(AlertDailyJob.JOB_NAME, JobTrigger.MANUAL);
    assertThat(liveAlert("SUSPENSE_BALANCE", "1606").getId()).isNotEqualTo(suspense.getId());
    assertThat(alerts.liveSummary()).containsKeys(AlertSeverity.values());
  }

  @Test
  void pendingApprovalAgeingUsesTheThresholdDays() {
    JournalBatch pending =
        journals.submitted(journals.request("5603", "1111", "77.00", LocalDate.now()));
    try {
      alerts.configure(
          "PENDING_APPROVAL_AGEING", new CodeSettings(AlertSeverity.MEDIUM, null, 0, true));
      jobs.run(AlertDailyJob.JOB_NAME, JobTrigger.MANUAL);
      assertThat(alertsFor("PENDING_APPROVAL_AGEING", pending.getBatchNo())).hasSize(1);
    } finally {
      alerts.configure(
          "PENDING_APPROVAL_AGEING", new CodeSettings(AlertSeverity.MEDIUM, null, 2, true));
    }
  }

  @Test
  void inactiveCodesAndFailingJobs() {
    AlertFacts facts = new AlertFacts(null, null, "Test", "T1", "Test", BigDecimal.ONE, "TEST:T1");
    try {
      alerts.configure("WEEKEND_POSTING", new CodeSettings(AlertSeverity.LOW, null, null, false));
      assertThat(alerts.raise("WEEKEND_POSTING", facts)).isEmpty();
      assertThat(alerts.raise("NO_SUCH_CODE", facts)).isEmpty();
    } finally {
      alerts.configure("WEEKEND_POSTING", new CodeSettings(AlertSeverity.LOW, null, null, true));
    }
    var failed =
        jobRuns.execute(
            "TEST_FAILING_JOB",
            JobTrigger.MANUAL,
            () -> {
              throw new IllegalStateException("boom");
            });
    assertThat(failed.getStatus()).isEqualTo(JobRunStatus.FAILED);
    assertThat(failed.getMessage()).isEqualTo("boom");
    assertThat(alertsFor("JOB_FAILURE", "TEST_FAILING_JOB")).hasSize(1);
  }

  @Test
  @WithUserDetails("fmanager")
  void exceptionReportRunsAndExports() {
    Map<String, String> params = Map.of("fromDate", "2020-01-01", "status", "ALL");
    assertThat(reports.run("CTL-EXCEPTIONS", params).code()).isEqualTo("CTL-EXCEPTIONS");
    for (ExportFormat format : ExportFormat.values()) {
      assertThat(reports.export("CTL-EXCEPTIONS", Map.of("status", "OPEN"), format).content())
          .isNotEmpty();
    }
  }

  @Test
  @WithUserDetails("fmanager")
  void alertEndpoints() throws Exception {
    AlertFacts facts =
        new AlertFacts(null, null, "Test", "HTTP-1", "HTTP test", null, "TEST:HTTP-1");
    Alert alert = alerts.raise("JOB_FAILURE", facts).orElseThrow();
    mvc.perform(get("/api/v1/alerts?status=OPEN&code=JOB_FAILURE&from=2020-01-01&to=2099-12-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    mvc.perform(get("/api/v1/alerts/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.live").isNumber());
    mvc.perform(post("/api/v1/alerts/" + alert.getId() + "/acknowledge"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    mvc.perform(
            post("/api/v1/alerts/" + alert.getId() + "/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Done\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"));
    mvc.perform(post("/api/v1/alerts/checks/run"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobName").value(AlertDailyJob.JOB_NAME));
    mvc.perform(
            put("/api/v1/alerts/exception-codes/LARGE_JOURNAL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\":\"HIGH\",\"thresholdAmount\":1000000,\"active\":true}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  void administratorTunesExceptionCodes() throws Exception {
    mvc.perform(
            put("/api/v1/alerts/exception-codes/JOB_FAILURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\":\"CRITICAL\",\"active\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.severity").value("CRITICAL"))
        .andExpect(jsonPath("$.updatedBy").value("admin"));
    mvc.perform(
            put("/api/v1/alerts/exception-codes/JOB_FAILURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\":\"HIGH\",\"active\":true}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/alerts/exception-codes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'UNBALANCED_TB')].severity").value("CRITICAL"));
  }

  private boolean live(Alert a) {
    return a.getStatus() != AlertStatus.RESOLVED;
  }

  private Alert liveAlert(String code, String entityId) {
    return alertsFor(code, entityId).stream().filter(this::live).findFirst().orElseThrow();
  }
}
