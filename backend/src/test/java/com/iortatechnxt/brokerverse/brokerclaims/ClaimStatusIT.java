package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimClosureKind;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimStatusChanged;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimFollowUpService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimProgressQuery;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService.StatusOption;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * The status engine of wave CL1-B (BRCLM.005/010-015/019-021/027/035;
 * FR-CL-041/042/044/045/050/051): the status access matrix, phases and the workflow stage,
 * temporary and permanent closure, reopen, follow-up and action plan, the history and the {@code
 * ClaimStatusChanged} events.
 */
@IntegrationTest
@RecordApplicationEvents
class ClaimStatusIT {

  private static final String OFFICER = "clmofficer";
  private static final String TL = "clmtl";
  private static final String TH = "clmth";

  @Autowired private BrokerClaimFixtures fixtures;
  @Autowired private ClaimStatusService statuses;
  @Autowired private ClaimClosureService closures;
  @Autowired private ClaimFollowUpService followUps;
  @Autowired private ClaimProgressQuery query;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ApplicationEvents events;

  private String stage(Long claimId) {
    return jdbc.queryForObject(
        "select stage_code from wf_case where entity_type = 'BrokerClaim' and entity_id = ?",
        String.class,
        String.valueOf(claimId));
  }

  private Long newClaim() {
    return fixtures.recorded(
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(10)), "NEW_COMPLETE_DOCS");
  }

  @Test
  void theFirstStatusOpensTheCaseAndPublishesTheChange() {
    Long id = newClaim();
    Long company = fixtures.company();
    assertThat(stage(id)).isEqualTo("NEW");
    assertThat(fixtures.column(id, "status_code", String.class)).isEqualTo("NEW_COMPLETE_DOCS");
    assertThat(fixtures.column(id, "next_follow_up_date", LocalDate.class))
        .isEqualTo(BrokerClaimFixtures.today().plusDays(7));
    ClaimStatusChanged first =
        events.stream(ClaimStatusChanged.class)
            .filter(e -> e.claimId().equals(id))
            .findFirst()
            .orElseThrow();
    assertThat(first.fromStatus()).isNull();
    assertThat(first.fromPhase()).isNull();
    assertThat(first.toPhase()).isEqualTo(ClaimPhase.NEW);
    ClaimProgressQuery.History history = as.run(OFFICER, () -> query.history(company, id));
    assertThat(history.statusChanges())
        .singleElement()
        .satisfies(
            h -> {
              assertThat(h.fromStatus()).isNull();
              assertThat(h.toLabel()).startsWith("Newly Filed");
            });
  }

  @Test
  void theMatrixLimitsTheOfficerAndIsCheckedOnSave() {
    Long id = newClaim();
    Long company = fixtures.company();
    List<StatusOption> officer = as.run(OFFICER, () -> statuses.allowedStatuses(company, id));
    assertThat(officer)
        .extracting(StatusOption::code)
        .containsExactlyInAnyOrder(
            "NEW_INCOMPLETE_DOCS", "TEMP_CLOSED_NO_DOCS", "TEMP_CLOSED_WITH_OFFER");
    assertThat(as.run(TL, () -> statuses.allowedStatuses(company, id)))
        .hasSizeGreaterThanOrEqualTo(17);
    assertThatThrownBy(
            () ->
                as.run(OFFICER, () -> statuses.change(company, id, "INSURER_CHECK_ISSUANCE", null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("You are not allowed to set the status For Insurer's Issuance of Check");
    assertThatThrownBy(() -> as.run("clmrisk", () -> statuses.allowedStatuses(company, id)))
        .hasMessage("Your claims unit is not set. Contact the Unit Head");
    assertThatThrownBy(() -> as.run(TL, () -> statuses.change(company, id, " ", null)))
        .hasMessage("Select the new status");
    assertThat(fixtures.column(id, "status_code", String.class)).isEqualTo("NEW_COMPLETE_DOCS");
  }

  @Test
  void statusChangesMoveThePhaseKeepHistoryAndRefuseTheWayBackToNew() {
    Long id = newClaim();
    Long company = fixtures.company();
    fixtures.statusSince(id, BrokerClaimFixtures.today().minusDays(5));
    as.run(TL, () -> statuses.change(company, id, "ADJUSTER_REVIEW", "Adjuster appointed"));
    assertThat(stage(id)).isEqualTo("IN_PROGRESS");
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("IN_PROGRESS");
    ClaimProgressQuery.Progress progress = as.run(TL, () -> query.progress(company, id));
    assertThat(progress.ages().thisStage()).isZero();
    assertThat(progress.ages().overall()).isEqualTo(10);
    ClaimProgressQuery.History history = as.run(TL, () -> query.history(company, id));
    assertThat(history.statusChanges()).hasSize(2);
    assertThat(history.statusChanges().get(1).daysInPrevious()).isEqualTo(5);
    assertThat(history.statusChanges().get(1).stamp().remark()).isEqualTo("Adjuster appointed");
    assertThatThrownBy(
            () -> as.run(TL, () -> statuses.change(company, id, "NEW_INCOMPLETE_DOCS", null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("has left the newly filed phase");
    assertThat(as.run(TL, () -> statuses.allowedStatuses(company, id)))
        .extracting(StatusOption::phase)
        .doesNotContain(ClaimPhase.NEW);
  }

  @Test
  void temporaryClosureKeepsTheClaimOutstandingAndResumes() {
    Long id = newClaim();
    Long company = fixtures.company();
    as.run(OFFICER, () -> statuses.change(company, id, "TEMP_CLOSED_WITH_OFFER", null));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("TEMP_CLOSED");
    assertThat(fixtures.column(id, "closure_kind", String.class))
        .isEqualTo(ClaimClosureKind.TEMPORARY.name());
    assertThat(stage(id)).isEqualTo("TEMP_CLOSED");
    as.run(TL, () -> statuses.change(company, id, "CLAIMANT_DOCS_SUBMISSION", null));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("IN_PROGRESS");
    assertThat(fixtures.column(id, "closure_kind", String.class)).isNull();
    assertThat(stage(id)).isEqualTo("IN_PROGRESS");
  }

  @Test
  void followUpOverrideIsKeptAcrossStatusChangesAndTheActionPlanIsVersioned() {
    Long id = newClaim();
    Long company = fixtures.company();
    LocalDate override = BrokerClaimFixtures.today().plusDays(20);
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        followUps.overrideFollowUp(
                            company, id, BrokerClaimFixtures.today().minusDays(1), "OTHER")))
        .hasMessage("The follow-up date cannot be before today");
    assertThatThrownBy(
            () -> as.run(TL, () -> followUps.overrideFollowUp(company, id, override, "")))
        .hasMessage("Enter the reason for the change");
    String reason =
        jdbc.queryForObject(
            "select code from lov_value where type_code = 'BCL_OVERRIDE_REASON' order by sort_order"
                + " limit 1",
            String.class);
    as.run(TL, () -> followUps.overrideFollowUp(company, id, override, reason));
    as.run(TL, () -> statuses.change(company, id, "INSURER_REVIEW", null));
    assertThat(fixtures.column(id, "next_follow_up_date", LocalDate.class)).isEqualTo(override);
    assertThat(fixtures.column(id, "follow_up_overridden", Boolean.class)).isTrue();

    as.run(OFFICER, () -> followUps.planNextAction(company, id, "Follow up LOA with INS-A"));
    as.run(OFFICER, () -> followUps.planNextAction(company, id, "x".repeat(2000)));
    assertThatThrownBy(
            () -> as.run(OFFICER, () -> followUps.planNextAction(company, id, "x".repeat(2001))))
        .hasMessage("The action plan can have up to 2000 characters");
    String adjuster =
        jdbc.queryForObject(
            "select code from lov_value where type_code = 'BCL_ADJUSTER' order by sort_order limit 1",
            String.class);
    as.run(TL, () -> followUps.assignAdjuster(company, id, adjuster, null));
    ClaimProgressQuery.History history = as.run(TL, () -> query.history(company, id));
    assertThat(history.fieldChanges())
        .extracting(f -> f.field().name())
        .containsExactly("FOLLOW_UP", "ACTION_PLAN", "ACTION_PLAN", "ADJUSTER");
    assertThat(as.run(TL, () -> query.progress(company, id)).followUp().adjusterName())
        .isNotBlank();
  }

  @Test
  void settlementClosesPermanentlyAndOnlyReopenFollows() {
    Long id = newClaim();
    Long company = fixtures.company();
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        closures.settle(
                            company,
                            id,
                            new ClaimClosureService.Settlement("SETTLED", null, null, null))))
        .hasMessage("Enter the settlement amount and the date settled");
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        closures.settle(
                            company,
                            id,
                            new ClaimClosureService.Settlement(
                                "SETTLED",
                                BigDecimal.TEN,
                                BrokerClaimFixtures.today().plusDays(1),
                                null))))
        .hasMessage("The date settled cannot be in the future");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        closures.settle(
                            company,
                            id,
                            new ClaimClosureService.Settlement("CLOSED_DENIED", null, null, null))))
        .isInstanceOf(AccessDeniedException.class);

    as.run(
        TL,
        () ->
            closures.settle(
                company,
                id,
                new ClaimClosureService.Settlement(
                    "SETTLED_RELEASE_PAPERS",
                    new BigDecimal("85000"),
                    BrokerClaimFixtures.today().minusDays(1),
                    "Release papers returned")));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("CLOSED");
    assertThat(fixtures.column(id, "closure_kind", String.class)).isEqualTo("PERMANENT");
    assertThat(fixtures.column(id, "closed_on", LocalDate.class))
        .isEqualTo(BrokerClaimFixtures.today());
    assertThat(stage(id)).isEqualTo("CLOSED");
    assertThatThrownBy(() -> as.run(TL, () -> statuses.change(company, id, "INSURER_REVIEW", null)))
        .hasMessageEndingWith("is closed. Reopen it before changing the status");
    assertThat(
            events.stream(ClaimStatusChanged.class)
                .filter(e -> e.claimId().equals(id))
                .anyMatch(e -> e.entered(ClaimPhase.CLOSED)))
        .isTrue();

    assertThatThrownBy(() -> as.run(TH, () -> closures.reopen(company, id, null, null)))
        .hasMessage("Select a reason for 'reopen'");
    String reason =
        jdbc.queryForObject(
            "select code from lov_value where type_code = 'BCL_REOPEN_REASON' order by sort_order"
                + " limit 1",
            String.class);
    as.run(TH, () -> closures.reopen(company, id, reason, "Insurer reconsidered"));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("IN_PROGRESS");
    assertThat(fixtures.column(id, "settlement_type_code", String.class)).isNull();
    assertThat(stage(id)).isEqualTo("IN_PROGRESS");
    assertThat(
            events.stream(ClaimStatusChanged.class)
                .filter(e -> e.claimId().equals(id))
                .anyMatch(
                    e ->
                        e.fromPhase() == ClaimPhase.CLOSED
                            && e.toPhase() == ClaimPhase.IN_PROGRESS))
        .isTrue();
    ClaimProgressQuery.History history = as.run(TH, () -> query.history(company, id));
    assertThat(history.fieldChanges())
        .extracting(f -> f.field().name())
        .containsExactly("SETTLEMENT", "SETTLEMENT");
    assertThat(history.fieldChanges().get(0).newValue()).contains("85000.00");
  }

  @Test
  void aTypeThatDoesNotCloseKeepsTheClaimOpen() {
    Long id = newClaim();
    Long company = fixtures.company();
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                id,
                new ClaimClosureService.Settlement(
                    "SETTLED_LOA_REPAIR_SCHEDULE",
                    new BigDecimal("1000"),
                    BrokerClaimFixtures.today(),
                    null)));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("NEW");
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                id,
                new ClaimClosureService.Settlement(
                    "CLOSED_DENIED", null, null, "Denied by the insurer")));
    assertThat(fixtures.column(id, "phase", String.class)).isEqualTo("CLOSED");
    assertThat(fixtures.column(id, "settlement_type_code", String.class))
        .isEqualTo("CLOSED_DENIED");
  }
}
