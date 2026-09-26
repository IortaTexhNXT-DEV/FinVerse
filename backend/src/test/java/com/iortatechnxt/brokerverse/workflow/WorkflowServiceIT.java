package com.iortatechnxt.brokerverse.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.service.QueueCount;
import com.iortatechnxt.brokerverse.workflow.service.QueueQuery;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkQueueService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class WorkflowServiceIT {

  private static final String TYPE = "WorkflowTestRecord";
  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000);

  @Autowired private WorkflowService workflow;
  @Autowired private WorkflowViewService views;
  @Autowired private WorkAssignmentService assignments;
  @Autowired private WorkflowDefinitions definitions;
  @Autowired private WorkQueueService queues;
  @Autowired private NotificationService notifications;
  @Autowired private TransitionCapture capture;
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
                        TYPE,
                        id,
                        "ARN-TEST-" + id,
                        "Juan Dela Cruz - Motor",
                        "/accounts/" + id,
                        "CBG"),
                    null)));
  }

  @Test
  void accountMovesThroughTheStagesWithPermissionChecksHistoryAndNotifications() {
    WorkCase c = open();
    assertThat(c.getStageCode()).isEqualTo("DRAFT");
    assertThat(c.getAssignee()).isEqualTo("ao");
    assertThat(c.getDueAt()).isNotNull();

    // The E-policy Sender cannot submit accounts.
    assertThatThrownBy(
            () ->
                as.run(
                    "epol",
                    () ->
                        workflow.transition(TYPE, c.getEntityId(), "submit", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("WORKFLOW_ACTION_NOT_PERMITTED");
    as.run("ao", () -> workflow.transition(TYPE, c.getEntityId(), "submit", TransitionNote.NONE));
    WorkCase submitted = views.get(c.getId());
    assertThat(submitted.getStageCode()).isEqualTo("SUBMITTED");
    assertThat(submitted.getAssignee()).isNull();

    // Actions not defined from the current stage are refused.
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> workflow.transition(TYPE, c.getEntityId(), "book", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("WORKFLOW_TRANSITION_NOT_ALLOWED");

    // Returning needs a valid reason; the case goes back to its originator.
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> workflow.genericTransition(c.getId(), "return", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        workflow.genericTransition(
                            c.getId(), "return", new TransitionNote("NOPE", null))))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    long unreadBefore = as.run("ao", () -> notifications.unreadCount());
    as.run(
        "proc",
        () ->
            workflow.genericTransition(
                c.getId(),
                "return",
                new TransitionNote("MISSING_DOCUMENTS", "Please attach the IDF")));
    WorkCase returned = views.get(c.getId());
    assertThat(returned.getStageCode()).isEqualTo("RETURNED_TO_MARKETING");
    assertThat(returned.getAssignee()).isEqualTo("ao");
    assertThat(as.run("ao", () -> notifications.unreadCount())).isEqualTo(unreadBefore + 1);
    assertThat(capture.events())
        .anyMatch(e -> e.entityId().equals(c.getEntityId()) && "return".equals(e.action()));

    // Business actions cannot be run from the generic endpoint.
    assertThatThrownBy(
            () -> as.run("ao", () -> workflow.genericTransition(c.getId(), "resubmit", null)))
        .extracting("code")
        .isEqualTo("WORKFLOW_ACTION_NOT_GENERIC");
    as.run(
        "ao",
        () ->
            workflow.transition(
                TYPE, c.getEntityId(), "resubmit", TransitionNote.comment("IDF attached")));
    workflow.systemTransition(TYPE, c.getEntityId(), "validate", TransitionNote.NONE);

    var view = as.run("proc", () -> views.view(TYPE, c.getEntityId()).orElseThrow());
    assertThat(view.stage().getStageCode()).isEqualTo("AWAITING_PAYMENT");
    assertThat(view.history())
        .extracting("action")
        .containsExactly("start", "submit", "return", "resubmit", "validate");
    assertThat(view.history().get(2).getReasonCode()).isEqualTo("MISSING_DOCUMENTS");
    assertThat(view.history().get(4).isAutomatic()).isTrue();
    assertThat(view.actions()).extracting("action").contains("payment_confirmed", "return");
    assertThat(views.stageOf(TYPE, c.getEntityId())).isEqualTo("AWAITING_PAYMENT");
  }

  @Test
  void queuesShowOnlyTheStagesOfTheUsersTeamAndSupportClaimAndAssignment() {
    WorkCase c = open();
    as.run("ao", () -> workflow.transition(TYPE, c.getEntityId(), "submit", TransitionNote.NONE));
    Long company = data.company().getId();

    var procQueue =
        as.run(
            "proc",
            () ->
                queues.queue(
                    new QueueQuery(
                        company,
                        "NB_ACCOUNT",
                        null,
                        QueueQuery.Scope.UNASSIGNED,
                        false,
                        c.getReference()),
                    Pageable.ofSize(10)));
    assertThat(procQueue.getContent()).extracting(WorkCase::getId).containsExactly(c.getId());
    var aoQueue =
        as.run(
            "ao",
            () ->
                queues.queue(
                    new QueueQuery(
                        company, null, null, QueueQuery.Scope.ALL, false, c.getReference()),
                    Pageable.ofSize(10)));
    assertThat(aoQueue.getContent()).isEmpty();

    List<QueueCount> counts = as.run("proc", () -> queues.counts(company));
    assertThat(counts)
        .anySatisfy(
            q -> {
              assertThat(q.stageCode()).isEqualTo("SUBMITTED");
              assertThat(q.open()).isPositive();
            });

    assertThatThrownBy(() -> as.run("ao", () -> assignments.claim(c.getId())))
        .extracting("code")
        .isEqualTo("WORK_CLAIM_NOT_ALLOWED");
    assertThat(as.run("proc", () -> assignments.claim(c.getId())).getAssignee()).isEqualTo("proc");

    var eligible = queues.eligibleUsers(definitions.stageOf(views.get(c.getId())));
    assertThat(eligible).contains("proc", "proctl").doesNotContain("ao");
    assertThatThrownBy(() -> as.run("proctl", () -> assignments.assign(c.getId(), "ao", eligible)))
        .extracting("code")
        .isEqualTo("WORK_ASSIGNEE_NOT_ELIGIBLE");
    assertThat(
            as.run("proctl", () -> assignments.assign(c.getId(), "proctl", eligible)).getAssignee())
        .isEqualTo("proctl");
    assertThat(as.run("proctl", () -> assignments.assign(c.getId(), null, eligible)).getAssignee())
        .isNull();
    var mine =
        as.run(
            "proc",
            () ->
                queues.queue(
                    new QueueQuery(
                        company, "NB_ACCOUNT", "SUBMITTED", QueueQuery.Scope.MINE, true, null),
                    Pageable.ofSize(10)));
    assertThat(mine.getContent()).extracting(WorkCase::getId).doesNotContain(c.getId());
  }

  @Test
  void terminalStageClosesTheCaseAndDuplicatesAreRejected() {
    WorkCase c = open();
    as.run(
        "ao",
        () ->
            workflow.genericTransition(
                c.getId(), "void", new TransitionNote("ENCODING_ERROR", null)));
    WorkCase voided = views.get(c.getId());
    assertThat(voided.isClosed()).isTrue();
    assertThat(voided.getAssignee()).isNull();
    assertThat(as.run("ao", () -> views.view(voided)).actions()).isEmpty();
    assertThatThrownBy(() -> as.run("proctl", () -> assignments.assign(c.getId(), null, List.of())))
        .extracting("code")
        .isEqualTo("WORK_CASE_CLOSED");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        workflow.start(
                            new StartCase(
                                data.company().getId(),
                                "NB_ACCOUNT",
                                new CaseRecord(TYPE, c.getEntityId(), "X", "X", null, null),
                                null))))
        .hasMessageContaining(c.getEntityId());
    workflow.describe(TYPE, c.getEntityId(), "ARN-RENAMED", "Renamed");
    assertThat(views.get(c.getId()).getReference()).isEqualTo("ARN-RENAMED");
    assertThat(definitions.stageNames("NB_QUOTATION")).containsKeys("DRAFT", "CONVERTED");
  }
}
