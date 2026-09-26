package com.iortatechnxt.brokerverse.journal;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.alert.domain.AlertStatus;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.alert.service.AlertService.AlertSearch;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.RecurringTemplateRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.domain.RecurrenceFrequency;
import com.iortatechnxt.brokerverse.journal.domain.RecurringJournalTemplate;
import com.iortatechnxt.brokerverse.journal.service.GenerationResult;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.journal.service.RecurringJournalGenerator;
import com.iortatechnxt.brokerverse.journal.service.RecurringJournalJob;
import com.iortatechnxt.brokerverse.journal.service.RecurringJournalService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.domain.JobRunStatus;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class RecurringJournalIT {

  @Autowired private RecurringJournalService service;
  @Autowired private RecurringJournalGenerator generator;
  @Autowired private RecurringJournalJob job;
  @Autowired private JournalEntryService entries;
  @Autowired private JobRunService jobRuns;
  @Autowired private AlertService alerts;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  /** Generation options of a test template. */
  private enum Flags {
    NONE,
    REVERSE,
    SUBMIT
  }

  private static JournalLineRequest line(String account, BalanceSide side, String amount) {
    return new JournalLineRequest(
        account, side, new BigDecimal(amount), null, null, null, "FIN", null, null, null, null);
  }

  private RecurringTemplateRequest request(
      String name,
      JournalType type,
      int day,
      LocalDate start,
      LocalDate end,
      Flags flags,
      List<JournalLineRequest> lines) {
    return new RecurringTemplateRequest(
        data.company().getId(),
        data.branch("HO").getId(),
        name,
        type,
        "PHP",
        "Recurring test " + name,
        "RJ-TEST",
        RecurrenceFrequency.MONTHLY,
        day,
        start,
        end,
        flags == Flags.REVERSE,
        flags == Flags.SUBMIT,
        lines);
  }

  private static List<JournalLineRequest> balanced(String amount) {
    return List.of(
        line("5603", BalanceSide.DEBIT, amount), line("2502", BalanceSide.CREDIT, amount));
  }

  private RecurringJournalTemplate create(RecurringTemplateRequest r) {
    return as.run("accountant", () -> service.create(r));
  }

  private GenerationResult run(Long id, LocalDate asOf) {
    return as.run("accountant", () -> service.runTemplate(id, asOf));
  }

  @Test
  void generationIsIdempotentPerOccurrenceAndAccrualsReverseNextPeriod() {
    RecurringJournalTemplate t =
        create(
            request(
                "RJ accrual idempotency",
                JournalType.ACCRUAL,
                31,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                Flags.REVERSE,
                balanced("2500.00")));

    GenerationResult first = run(t.getId(), LocalDate.of(2026, 3, 15));
    assertThat(first.generated()).isEqualTo(2);
    assertThat(first.errors()).isEmpty();
    assertThat(first.journals())
        .extracting(j -> j.occurrenceDate())
        .containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28));
    JournalBatch jan = entries.get(first.journals().get(0).batchId());
    assertThat(jan.getStatus()).isEqualTo(JournalStatus.DRAFT);
    assertThat(jan.getValueDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(jan.getSourceModule()).isEqualTo(RecurringJournalGenerator.SOURCE_MODULE);
    assertThat(jan.getCreatedBy()).isEqualTo("accountant");
    assertThat(first.journals().get(0).reversalBatchNo()).isNotBlank();

    assertThat(run(t.getId(), LocalDate.of(2026, 3, 15)).generated()).isZero();
    assertThat(generator.generate(t.getId(), LocalDate.of(2026, 1, 31))).isEmpty();
    assertThat(run(t.getId(), LocalDate.of(2026, 12, 31)).generated()).isEqualTo(1);

    var history = service.history(t.getId());
    assertThat(history).hasSize(3);
    assertThat(history.get(0).occurrenceDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(history.get(0).batchStatus()).isEqualTo("DRAFT");
    assertThat(history.get(0).reversalBatchNo()).isNotBlank();
    JournalBatch reversal = entries.get(history.get(1).reversalBatchId());
    assertThat(reversal.getValueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(
            jdbc.queryForObject(
                "select side from jnl_line where batch_id = ? and line_no = 1",
                String.class,
                reversal.getId()))
        .isEqualTo("CREDIT");
    assertThat(jobRuns.latest(RecurringJournalService.JOB_NAME))
        .hasValueSatisfying(r -> assertThat(r.getTriggeredBy()).isEqualTo("accountant"));
  }

  @Test
  void autoSubmitSendsTheJournalForApproval() {
    LocalDate today = LocalDate.now();
    RecurringJournalTemplate t =
        create(
            request(
                "RJ auto submit",
                JournalType.MANUAL,
                today.getDayOfMonth(),
                today,
                null,
                Flags.SUBMIT,
                balanced("900.00")));
    GenerationResult result = run(t.getId(), today);
    assertThat(result.journals())
        .singleElement()
        .satisfies(j -> assertThat(j.submitted()).isTrue());
    JournalBatch batch = entries.get(result.journals().get(0).batchId());
    assertThat(batch.getStatus()).isEqualTo(JournalStatus.PENDING_APPROVAL);
    assertThat(batch.getSubmittedBy()).isEqualTo("accountant");
  }

  @Test
  void templatesAreValidated() {
    LocalDate start = LocalDate.of(2027, 1, 1);
    assertThatThrownBy(
            () ->
                create(
                    request(
                        "RJ unbalanced",
                        JournalType.MANUAL,
                        1,
                        start,
                        null,
                        Flags.NONE,
                        List.of(
                            line("5603", BalanceSide.DEBIT, "10.00"),
                            line("2502", BalanceSide.CREDIT, "9.00")))))
        .extracting("code")
        .isEqualTo("UNBALANCED_TEMPLATE");
    assertThatThrownBy(
            () ->
                create(
                    request(
                        "RJ unknown account",
                        JournalType.MANUAL,
                        1,
                        start,
                        null,
                        Flags.NONE,
                        List.of(
                            line("NOPE", BalanceSide.DEBIT, "10.00"),
                            line("2502", BalanceSide.CREDIT, "10.00")))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                create(
                    request(
                        "RJ bad type",
                        JournalType.REVERSAL,
                        1,
                        start,
                        null,
                        Flags.NONE,
                        balanced("1.00"))))
        .extracting("code")
        .isEqualTo("INVALID_JOURNAL_TYPE");
    assertThatThrownBy(
            () ->
                create(
                    request(
                        "RJ bad schedule",
                        JournalType.MANUAL,
                        1,
                        start,
                        start.minusDays(1),
                        Flags.NONE,
                        balanced("1.00"))))
        .extracting("code")
        .isEqualTo("INVALID_SCHEDULE");
    create(
        request("RJ unique", JournalType.ADJUSTMENT, 1, start, null, Flags.NONE, balanced("1.00")));
    assertThatThrownBy(
            () ->
                create(
                    request(
                        "RJ unique",
                        JournalType.MANUAL,
                        1,
                        start,
                        null,
                        Flags.NONE,
                        balanced("1.00"))))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void updateDeactivateAndActivate() {
    LocalDate start = LocalDate.of(2027, 2, 1);
    RecurringJournalTemplate t =
        create(
            request(
                "RJ lifecycle", JournalType.MANUAL, 15, start, null, Flags.NONE, balanced("5.00")));
    RecurringJournalTemplate updated =
        as.run(
            "accountant",
            () ->
                service.update(
                    t.getId(),
                    request(
                        "RJ lifecycle",
                        JournalType.MANUAL,
                        20,
                        start,
                        null,
                        Flags.NONE,
                        balanced("7.00"))));
    assertThat(updated.getDayOfMonth()).isEqualTo(20);
    assertThat(updated.getLines().get(0).getAmount()).isEqualByComparingTo("7.00");

    as.run("accountant", () -> service.setActive(t.getId(), false));
    assertThatThrownBy(() -> run(t.getId(), start.plusMonths(2)))
        .extracting("code")
        .isEqualTo("TEMPLATE_INACTIVE");
    assertThat(generator.dueOccurrences(t.getId(), start.plusMonths(2))).isEmpty();
    assertThat(as.run("accountant", () -> service.setActive(t.getId(), true)).isActive()).isTrue();
    assertThat(service.list(data.company().getId()))
        .extracting(RecurringJournalTemplate::getName)
        .contains("RJ lifecycle");
    as.run("accountant", () -> service.setActive(t.getId(), false));
  }

  @Test
  void failedOccurrenceFailsTheRunAndRaisesJobFailure() {
    RecurringJournalTemplate t =
        create(
            request(
                "RJ failing",
                JournalType.MANUAL,
                10,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                Flags.NONE,
                balanced("3.00")));
    jdbc.update(
        "update jnl_recurring_line set account_code = 'GONE' where template_id = ? and line_no = 0",
        t.getId());
    GenerationResult result = run(t.getId(), LocalDate.of(2026, 2, 28));
    assertThat(result.generated()).isZero();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.message()).contains("failed");
    assertThat(jobRuns.latest(RecurringJournalService.JOB_NAME))
        .hasValueSatisfying(r -> assertThat(r.getStatus()).isEqualTo(JobRunStatus.FAILED));
    var failures =
        alerts.search(
            new AlertSearch(
                AlertStatus.OPEN,
                null,
                "JOB_FAILURE",
                null,
                Instant.EPOCH,
                Instant.now().plusSeconds(60)),
            Pageable.ofSize(10));
    assertThat(failures.getContent()).isNotEmpty();
    as.run("accountant", () -> service.setActive(t.getId(), false));
  }

  @Test
  void scheduledJobDescribesItself() {
    assertThat(job.name()).isEqualTo("RECURRING_JOURNALS");
    assertThat(job.description()).isNotBlank();
    assertThat(job.cron()).isEqualTo("0 0 1 * * *");
    assertThat(job.execute(LocalDate.of(2026, 1, 1)).message()).contains("generated");
  }

  @Test
  @WithUserDetails("accountant")
  void httpEndpoints() throws Exception {
    Long company = data.company().getId();
    Long branch = data.branch("HO").getId();
    String body =
        """
        {"companyId":%d,"branchId":%d,"name":"RJ via API","journalType":"ACCRUAL","currency":"PHP",
         "narration":"API accrual","frequency":"QUARTERLY","dayOfMonth":31,
         "startDate":"2026-01-01","endDate":"2026-06-30","autoReverse":true,"autoSubmit":false,
         "lines":[{"accountCode":"5603","side":"DEBIT","amount":100.00,"costCenter":"FIN"},
                  {"accountCode":"2502","side":"CREDIT","amount":100.00}]}
        """
            .formatted(company, branch);
    String created =
        mvc.perform(
                post("/api/v1/journals/recurring")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.frequency").value("QUARTERLY"))
            .andExpect(jsonPath("$.lines.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = created.replaceAll("^\\{\"id\":(\\d+).*$", "$1").strip();
    mvc.perform(post("/api/v1/journals/recurring/" + id + "/run?date=2026-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.journals.length()").value(2));
    mvc.perform(get("/api/v1/journals/recurring/" + id + "/occurrences"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/v1/journals/recurring/" + id)).andExpect(status().isOk());
    mvc.perform(post("/api/v1/journals/recurring/" + id + "/deactivate"))
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(post("/api/v1/journals/recurring/" + id + "/activate"))
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/api/v1/journals/recurring?companyId=" + company)).andExpect(status().isOk());
  }
}
