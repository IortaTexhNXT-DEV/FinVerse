package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningCaseFixtures.INVESTIGATOR;
import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseTypes;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseApprovalService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseAssignmentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseQueries;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseRetentionProvider;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSearch;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSubmissionService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.cases.service.SlaMonitor;
import com.iortatechnxt.brokerverse.screening.cases.service.SlaMonitorJob;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecisionService;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningCompleted;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningResult;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOutcome;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The rules around screening cases (SNSRP-303, 404, 405, 702, 802, 401; FR-SS-034, 040, 043, 044,
 * 061, 081): SLA reminder and breach once per stage entry with the alert, missing-document
 * reminders, re-assignment with reason and eligibility, the four-eyes refusal of the unit head,
 * re-opening, ACCOUNT_APPLICATION and MONITOR cases from screening results, "Open Case" joining the
 * open case, the case list scope and filters, and the retention candidates.
 */
@IntegrationTest
class ScreeningCaseRulesIT {

  @Autowired private ScreeningCaseFixtures fx;
  @Autowired private ScreeningCaseRepository cases;
  @Autowired private SlaMonitor monitor;
  @Autowired private SlaMonitorJob job;
  @Autowired private CaseAssignmentService assignments;
  @Autowired private CaseSubmissionService submissions;
  @Autowired private CaseApprovalService approvals;
  @Autowired private CaseQueries queries;
  @Autowired private CaseTimeline timeline;
  @Autowired private CaseRetentionProvider retention;
  @Autowired private MatchDecisionService decisions;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private PlatformTransactionManager txManager;
  @Autowired private JdbcTemplate jdbc;

  private long count(ScreeningCase c, CaseEventType type) {
    return timeline.of(c.getId()).stream().map(CaseEvent::getEvent).filter(type::equals).count();
  }

  private void due(ScreeningCase c, String dueSql, String remindSql) {
    jdbc.update(
        "update scr_case set due_at = "
            + dueSql
            + ", remind_at = "
            + remindSql
            + ", reminded_at = null, breached = false where id = ?",
        c.getId());
  }

  @Test
  void theSlaMonitorRemindsOnceAndEscalatesABreachOnce() {
    ScreeningCase c = fx.investigatedCase();
    due(c, "now() + interval '5 hours'", "now() + interval '1 hour'");
    monitor.run();
    assertThat(fx.reload(c).getRemindedAt()).isNull();

    due(c, "now() + interval '2 hours'", "now() - interval '2 hours'");
    monitor.run();
    monitor.run();
    assertThat(fx.reload(c).getRemindedAt()).isNotNull();
    assertThat(count(c, CaseEventType.REMINDER)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where lower(recipient) = ? and link = ?",
                Integer.class,
                INVESTIGATOR,
                "/screening/cases/" + c.getId()))
        .isPositive();
    // The KYC form is missing: the assignee and the UCC get one document reminder a day.
    assertThat(count(c, CaseEventType.DOCUMENT_REMINDER)).isEqualTo(1);

    jdbc.update(
        "update scr_case set due_at = now() - interval '1 hour', remind_at = now() - interval '5 hours'"
            + " where id = ?",
        c.getId());
    assertThat(job.execute(LocalDate.now()).message()).contains("breach");
    monitor.run();
    ScreeningCase breached = fx.reload(c);
    assertThat(breached.isBreached()).isTrue();
    assertThat(breached.getEscalatedTo()).startsWith("COMPLIANCE_OFFICER");
    assertThat(count(c, CaseEventType.BREACH)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where exception_code = 'SCR_SLA_BREACH' and entity_id = ?",
                Integer.class,
                c.getCaseNo()))
        .isEqualTo(1);
    assertThat(
            fx.as(
                    "ucc",
                    () ->
                        queries.search(
                            new CaseSearch(
                                c.getCompanyId(),
                                CaseSearch.Tab.ALL,
                                c.getCaseNo(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                com.iortatechnxt.brokerverse.screening.cases.service.CaseSla
                                    .SlaState.BREACHED,
                                null,
                                null),
                            PageRequest.of(0, 5)))
                .getContent())
        .extracting(ScreeningCase::getId)
        .containsExactly(c.getId());
  }

  @Test
  void reassignmentNeedsAReasonAndAnEligibleUser() {
    ScreeningCase c = fx.investigatedCase();
    assertThat(fx.as("ucc", () -> assignments.eligible(c.getId())))
        .contains("investigator2")
        .doesNotContain(INVESTIGATOR, "ao", "compchk");
    assertThatThrownBy(
            () -> fx.as("ucc", () -> assignments.reassign(c.getId(), "investigator2", null, null)))
        .hasMessage("Select the reason for the re-assignment");
    assertThatThrownBy(
            () ->
                fx.as("ucc", () -> assignments.reassign(c.getId(), "investigator2", "OTHERS", " ")))
        .hasMessage("Enter a comment for reason Others");
    assertThatThrownBy(
            () -> fx.as("ucc", () -> assignments.reassign(c.getId(), "compchk", "ABSENCE", null)))
        .hasMessage("compchk cannot take cases in stage INVESTIGATION");
    assertThatThrownBy(
            () ->
                fx.as(
                    INVESTIGATOR,
                    () -> assignments.reassign(c.getId(), "investigator2", "ABSENCE", null)))
        .isInstanceOf(AccessDeniedException.class);
    long before = count(c, CaseEventType.REASSIGNED);
    ScreeningCase moved =
        fx.as("ucc", () -> assignments.reassign(c.getId(), "investigator2", "ABSENCE", "On leave"));
    assertThat(moved.getAssignee()).isEqualTo("investigator2");
    assertThat(moved.getStage()).isEqualTo(CaseStage.INVESTIGATION);
    assertThat(count(c, CaseEventType.REASSIGNED)).isEqualTo(before + 1);
    assertThatThrownBy(() -> fx.completeReview(c))
        .hasMessageContaining("is assigned to investigator2");
  }

  @Test
  void theInvestigatorNeverApprovesTheOwnCaseAndACloseCaseIsReopenedByCompliance() {
    ScreeningCase c = fx.investigatedCase();
    fx.as("ucc", () -> assignments.reassign(c.getId(), "scrdual", "WORKLOAD", null));
    fx.as(
        "scrdual",
        () ->
            submissions.submit(
                c.getId(), new Decision("NEED_MORE_INFO", "Ask for the valid ID", false)));
    assertThat(fx.reload(c).getStage()).isEqualTo(CaseStage.INVESTIGATION);
    assertThat(fx.reload(c).getAssignee()).isEqualTo("scrdual");
    assertThat(count(c, CaseEventType.INFO_REQUESTED)).isEqualTo(1);
    assertThatThrownBy(
            () ->
                fx.as(
                    "scrdual", () -> submissions.submit(c.getId(), new Decision(null, "x", false))))
        .hasMessage("Select the disposition");
    ScreeningCase other = fx.investigatedCase();
    fx.as("ucc", () -> assignments.reassign(other.getId(), "scrdual", "WORKLOAD", null));
    fx.completeReviewAs(other, "scrdual");
    fx.uploadKycFormAs(other, "scrdual");
    fx.as(
        "scrdual",
        () ->
            submissions.submit(other.getId(), new Decision("TRUE_MATCH_REVIEW", "Escalate", true)));
    assertThat(fx.reload(other).getStage()).isEqualTo(CaseStage.UNIT_HEAD_APPROVAL);
    assertThatThrownBy(
            () ->
                fx.as(
                    "scrdual",
                    () ->
                        approvals.unitHead(
                            other.getId(), new CaseApprovalService.Step("CONCUR", null, null))))
        .hasMessage("A case is approved by someone other than its Investigator");
    assertThatThrownBy(
            () ->
                fx.as(
                    "scrapprover",
                    () ->
                        approvals.unitHead(
                            other.getId(),
                            new CaseApprovalService.Step("NOT_CONCUR", "INCOMPLETE_DETAILS", " "))))
        .hasMessage("Write the rationale for disapproving");
    ScreeningCase returned =
        fx.as(
            "scrapprover",
            () ->
                approvals.unitHead(
                    other.getId(),
                    new CaseApprovalService.Step(
                        "NOT_CONCUR", "INCOMPLETE_DETAILS", "KYC form is expired")));
    assertThat(returned.getStage()).isEqualTo(CaseStage.RETURNED);
    assertThat(returned.getReturnedFrom()).isEqualTo(CaseStage.UNIT_HEAD_APPROVAL);

    // A false positive is closed without approval by the seed route (INVESTIGATION /
    // FALSE_POSITIVE).
    fx.as("ucc", () -> assignments.reassign(c.getId(), INVESTIGATOR, "WORKLOAD", null));
    fx.completeReview(c);
    fx.uploadKycForm(c);
    fx.as(
        INVESTIGATOR,
        () ->
            submissions.submit(
                c.getId(), new Decision("FALSE_POSITIVE", "Different person", false)));
    ScreeningCase closed = fx.reload(c);
    assertThat(closed.getStatus()).isEqualTo(CaseStatus.CLOSED);
    assertThatThrownBy(
            () ->
                fx.as(
                    INVESTIGATOR,
                    () ->
                        approvals.reopen(
                            c.getId(), new CaseApprovalService.Step(null, "OTHERS", "x"))))
        .isInstanceOf(AccessDeniedException.class);
    ScreeningCase reopened =
        fx.as(
            "compoff",
            () ->
                approvals.reopen(
                    c.getId(), new CaseApprovalService.Step(null, "OTHERS", "New adverse media")));
    assertThat(reopened.getStage()).isEqualTo(CaseStage.INVESTIGATION);
    assertThat(reopened.getAssignee()).isEqualTo(INVESTIGATOR);
    assertThat(
            retention.countEligible(
                new RetentionCriteria(Set.of("CLOSED"), LocalDate.now().plusDays(1))))
        .isPositive();
    assertThat(
            retention.eligible(
                new RetentionCriteria(Set.of("CLOSED"), LocalDate.now().plusDays(1)), 5))
        .isNotEmpty();
  }

  @Test
  void screeningResultsOpenAccountApplicationAndMonitorCases() {
    Client client = fx.listedClient();
    ScreeningCase nameMatch = fx.caseOf(client);
    RiskOutcome pep =
        new RiskOutcome(
            client.getId(),
            "PEP",
            "Politically exposed person",
            2,
            "HIGH",
            "HIGH",
            Set.of(),
            CaseTypes.PEP,
            true,
            nameMatch.getRiskVersionId(),
            null,
            null,
            null,
            false);
    ScreeningResult result =
        new ScreeningResult(
            -1L,
            "SCN-TEST",
            client.getCompanyId(),
            ScreeningTrigger.ACCOUNT_SUBMITTED,
            "ARN-" + word(),
            nameMatch.getMatchVersionId(),
            nameMatch.getRiskVersionId(),
            1,
            1,
            List.of(),
            List.of(pep));
    new TransactionTemplate(txManager)
        .executeWithoutResult(s -> events.publishEvent(new ScreeningCompleted(result)));

    ScreeningCase monitor =
        cases
            .findByClientIdAndCaseTypeAndStatus(client.getId(), CaseTypes.MONITOR, CaseStatus.OPEN)
            .orElseThrow();
    assertThat(monitor.isActivePolicy()).isFalse();
    assertThat(monitor.getTemplateType()).isEqualTo("KYC_REVIEW");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where lower(recipient) = 'ucc' and link = ?",
                Integer.class,
                "/screening/cases/" + monitor.getId()))
        .isEqualTo(1);
    ScreeningCase application =
        cases
            .findByClientIdAndCaseTypeAndStatus(
                client.getId(), CaseTypes.ACCOUNT_APPLICATION, CaseStatus.OPEN)
            .orElseThrow();
    assertThat(application.getTemplateType()).isEqualTo("TRANSACTION_REVIEW");
    assertThat(application.getTriggerCode()).isEqualTo("ACCOUNT_SUBMITTED");

    // "Open Case" on a potential match not yet in a case joins the client's open NAME_MATCH case.
    Long matchId =
        jdbc.queryForObject(
            "select max(id) from scr_match where client_id = ?", Long.class, client.getId());
    jdbc.update("update scr_match set case_id = null where id = ?", matchId);
    var opened = fx.as(INVESTIGATOR, () -> decisions.openCase(matchId));
    assertThat(opened.caseId()).isEqualTo(nameMatch.getId());
    assertThat(opened.joined()).isTrue();

    // The case list: scope, search and date filters.
    CaseSearch search =
        new CaseSearch(
            client.getCompanyId(),
            CaseSearch.Tab.ALL,
            client.getCode(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1));
    assertThat(fx.as("ucc", () -> queries.search(search, PageRequest.of(0, 10)).getContent()))
        .hasSize(3);
    assertThatThrownBy(
            () ->
                fx.as(
                    "ucc",
                    () ->
                        queries.search(
                            new CaseSearch(
                                client.getCompanyId(),
                                CaseSearch.Tab.ALL,
                                "de",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                            PageRequest.of(0, 10))))
        .hasMessage("Enter at least 3 characters");
    assertThat(fx.as("ucc", () -> queries.tiles(client.getCompanyId())).openByStage())
        .containsKey(CaseStage.INVESTIGATION);
  }
}
