package com.iortatechnxt.brokerverse.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistory;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseHistoryRepository;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Workflow contract of Sanction Screening (SANCTION_SCREENING_DESIGN section 9): the due-time
 * override of a dated SLA matrix (SNSRP-108) and the re-assignment with a reason (SNSRP-404), both
 * kept in the status history.
 */
@IntegrationTest
class WorkflowSlaOverrideIT {

  private static final String TYPE = "WorkflowSlaTestRecord";
  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000);

  @Autowired private WorkflowService workflow;
  @Autowired private WorkAssignmentService assignments;
  @Autowired private WorkCaseHistoryRepository history;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private WorkCase open() {
    String id = String.valueOf(IDS.incrementAndGet());
    return as.run(
        "ao",
        () ->
            workflow.start(
                new StartCase(
                    data.company().getId(),
                    "NB_ACCOUNT",
                    new CaseRecord(
                        TYPE, id, "SLA-TEST-" + id, "SLA test", "/accounts/" + id, "CBG"),
                    null)));
  }

  private List<WorkCaseHistory> historyOf(WorkCase c) {
    return history.findByCaseIdOrderByIdAsc(c.getId());
  }

  @Test
  void theDueTimeIsOverriddenAndKeptInTheHistory() {
    WorkCase opened = open();
    Instant due = Instant.parse("2030-01-02T03:00:00Z");
    WorkCase changed =
        as.run("ao", () -> workflow.overrideDue(opened.getId(), due, "SLA rule 7: 48 hours"));
    assertThat(changed.getDueAt()).isEqualTo(due);
    assertThat(historyOf(opened))
        .last()
        .satisfies(
            h -> {
              assertThat(h.getAction()).isEqualTo(WorkflowService.SLA_OVERRIDE);
              assertThat(h.getFromStage()).isEqualTo(h.getToStage());
              assertThat(h.getComment()).isEqualTo("SLA rule 7: 48 hours");
              assertThat(h.getActor()).isEqualTo("ao");
            });
    WorkCase cleared = as.run("ao", () -> workflow.overrideDue(opened.getId(), null, null));
    assertThat(cleared.getDueAt()).isNull();
    assertThat(cleared.isOverdue(Instant.now().plus(1, ChronoUnit.DAYS))).isFalse();
  }

  @Test
  void aReassignmentWithAReasonIsKeptAndOneWithoutIsNot() {
    WorkCase opened = open();
    int before = historyOf(opened).size();
    as.run("mkttl", () -> assignments.assign(opened.getId(), "ao2", List.of("ao", "ao2")));
    assertThat(historyOf(opened)).hasSize(before);

    WorkCase moved =
        as.run(
            "mkttl",
            () ->
                assignments.assign(
                    opened.getId(), "ao", List.of("ao", "ao2"), "WORKLOAD", "Balancing the queue"));
    assertThat(moved.getAssignee()).isEqualTo("ao");
    assertThat(historyOf(opened))
        .last()
        .satisfies(
            h -> {
              assertThat(h.getAction()).isEqualTo(WorkAssignmentService.REASSIGN);
              assertThat(h.getReasonCode()).isEqualTo("WORKLOAD");
              assertThat(h.getComment()).isEqualTo("ao2 -> ao: Balancing the queue");
            });
    as.run(
        "mkttl",
        () -> assignments.assign(opened.getId(), null, List.of("ao"), null, "Back to the queue"));
    assertThat(historyOf(opened))
        .last()
        .extracting(WorkCaseHistory::getComment)
        .isEqualTo("ao -> team queue: Back to the queue");
    assertThatThrownBy(
            () ->
                as.run(
                    "mkttl",
                    () ->
                        assignments.assign(
                            opened.getId(), "ghost", List.of("ao"), "ABSENCE", null)))
        .isInstanceOf(BusinessRuleException.class);
  }
}
