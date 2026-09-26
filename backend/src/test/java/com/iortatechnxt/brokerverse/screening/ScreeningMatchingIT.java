package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.person;
import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.matching.service.BatchScreening;
import com.iortatechnxt.brokerverse.screening.matching.service.FalsePositive;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecision;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecisionService;
import com.iortatechnxt.brokerverse.screening.matching.service.PeriodicScreeningJob;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningEngine;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningQueries;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningResult;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntry;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskSource;
import com.iortatechnxt.brokerverse.screening.risk.service.ManualRiskChange;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOverrideService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Matching and risk profiling end to end (SNSRP-301, 302, 304, 602; FR-SS-030 to 033, 035): a new
 * client with a listed name is screened on registration, matched and tagged; a list change screens
 * the clients sharing a key and moves the KYC review date; identity changes and account submission
 * trigger runs; re-runs are idempotent; false positives are suppressed until the entry changes;
 * manual risk changes need justification and evidence; the batch window screens the clients in
 * scope only.
 */
@IntegrationTest
class ScreeningMatchingIT {

  private static final String INVESTIGATOR = "investigator";
  private static final String HIGH = "HIGH";
  private static final String WATCHLIST_REVIEW = "WATCHLIST_REVIEW";

  @Autowired private ScreeningMatchingFixtures fx;
  @Autowired private ScreeningEngine engine;
  @Autowired private BatchScreening batch;
  @Autowired private PeriodicScreeningJob job;
  @Autowired private ScreeningQueries queries;
  @Autowired private MatchDecisionService decisions;
  @Autowired private RiskOverrideService overrides;
  @Autowired private ClientService clients;
  @Autowired private ClientRiskService clientRisk;
  @Autowired private AttachmentService attachments;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private PlatformTransactionManager txManager;
  @Autowired private CompletedRuns completed;
  @Autowired private AsUser asUser;
  @Autowired private JdbcTemplate jdbc;

  private <T> T as(String user, Supplier<T> action) {
    return asUser.run(user, action);
  }

  private static LocalDate someBirthDate() {
    return LocalDate.of(1975, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(9000));
  }

  private ScreeningRun lastRun(String reference, ScreeningTrigger trigger) {
    Long id =
        jdbc.queryForObject(
            "select max(id) from scr_screening_run where reference = ? and trigger_code = ?",
            Long.class,
            reference,
            trigger.name());
    assertThat(id).as("run " + trigger + " " + reference).isNotNull();
    return queries.run(id);
  }

  private List<ScreeningMatch> matchesOf(Long clientId) {
    return queries.clientMatches(clientId);
  }

  private Long evidence(Long matchId) {
    return as(
            INVESTIGATOR,
            () ->
                attachments.upload(
                    new AttachmentTarget(
                        MatchDecisionService.MATCH_ENTITY, String.valueOf(matchId)),
                    "id_copy.pdf",
                    ScreeningMatchingFixtures.PDF,
                    "ID copy"))
        .getId();
  }

  @Test
  void aNewClientWithAListedNameIsMatchedAndTaggedOnRegistration() {
    Client c = fx.prospect(fx.demoCompany(), person("Juan", "Dela Cruz", someBirthDate()));

    ScreeningRun run = lastRun(c.getCode(), ScreeningTrigger.CLIENT_REGISTERED);
    assertThat(run.getStatus()).isEqualTo(ScreeningRunStatus.SUCCESS);
    assertThat(run.getClientsScreened()).isEqualTo(1);
    assertThat(run.getMatches()).isGreaterThanOrEqualTo(1);
    assertThat(run.getRiskChanges()).isEqualTo(1);
    assertThat(run.getMatchVersionId()).isNotNull();
    assertThat(run.getRiskVersionId()).isNotNull();

    ScreeningMatch match =
        matchesOf(c.getId()).stream()
            .filter(m -> m.getEntryName().equals("Juan de la Cruz"))
            .findFirst()
            .orElseThrow();
    assertThat(match.getStatus()).isEqualTo(MatchStatus.POTENTIAL);
    assertThat(match.getListType()).isEqualTo("SANCTION");
    assertThat(match.getScore()).isGreaterThanOrEqualTo(new BigDecimal("0.85"));
    assertThat(match.fields()).contains(MatchField.NAME);
    assertThat(completed.of(c.getId())).isNotEmpty();

    Client tagged = clients.get(c.getId());
    assertThat(tagged.getRiskRating()).isEqualTo(HIGH);
    assertThat(clientRisk.activeTags(c.getId())).contains(WATCHLIST_REVIEW);
    RiskProfileEntry history = overrides.history(c.getId()).get(0);
    assertThat(history.getSource()).isEqualTo(RiskSource.RULE);
    assertThat(history.getCategoryCode()).isEqualTo("HIGH_SANCTION");
    assertThat(history.getPreviousRating()).isEqualTo("STANDARD");
    assertThat(history.getKycRiskRating()).isEqualTo(HIGH);
    assertThat(history.getRuleId()).isNotNull();
    assertThat(history.getMatchId()).isEqualTo(match.getId());
    assertThat(history.getRunId()).isEqualTo(run.getId());

    // FR-SS-030 R1: the same versions record no second match; nothing changes the second time.
    ScreeningResult again =
        as(INVESTIGATOR, () -> engine.screenClient(c.getId(), ScreeningTrigger.MANUAL, null))
            .orElseThrow();
    assertThat(again.matches()).isEmpty();
    assertThat(again.outcomeOf(c.getId()).orElseThrow().changed()).isFalse();
    assertThat(overrides.history(c.getId())).hasSize(1);
  }

  @Test
  void aListChangeScreensTheClientAndBringsTheKycReviewForward() {
    String last = word();
    String first = word();
    LocalDate birth = someBirthDate();
    Client c = fx.verified(person(first, last, birth));
    LocalDate reviewBefore = c.getKycReviewDue();
    assertThat(reviewBefore).isNotNull();
    assertThat(matchesOf(c.getId())).isEmpty();

    Long entryId = fx.listed(first + " " + last, birth);

    ScreeningMatch match = matchesOf(c.getId()).get(0);
    assertThat(match.getEntryId()).isEqualTo(entryId);
    assertThat(match.getListType()).isEqualTo("INTERNAL");
    assertThat(match.fields()).containsExactly(MatchField.NAME);
    ScreeningRun run = queries.run(match.getRunId());
    assertThat(run.getTrigger()).isEqualTo(ScreeningTrigger.LIST_CHANGE);
    assertThat(run.getReference()).startsWith("CHANGE:");
    Client tagged = clients.get(c.getId());
    assertThat(tagged.getRiskRating()).isEqualTo(HIGH);
    assertThat(tagged.getKycReviewDue()).isBefore(reviewBefore);
    assertThat(overrides.history(c.getId()).get(0).getKycReviewDue())
        .isEqualTo(tagged.getKycReviewDue());
  }

  @Test
  void anIdentityChangeAndAnAccountSubmissionScreenTheClient() {
    Client c = fx.prospect(fx.demoCompany(), person(word(), word(), someBirthDate()));
    assertThat(matchesOf(c.getId())).isEmpty();
    String first = word();
    String last = word();
    fx.listed(first + " " + last, null);
    as("ao", () -> clients.update(c.getId(), person(first, last, c.getBirthDate())));
    ScreeningRun changed = lastRun(c.getCode(), ScreeningTrigger.CLIENT_CHANGED);
    assertThat(changed.getMatches()).isEqualTo(1);

    Long accountId =
        jdbc.queryForObject("select id from acc_account where arn = 'ARN-2026-900001'", Long.class);
    new TransactionTemplate(txManager)
        .executeWithoutResult(
            s ->
                events.publishEvent(
                    new AccountStatusChanged(
                        accountId,
                        "ARN-2026-900001",
                        AccountStatus.DRAFT,
                        AccountStatus.SUBMITTED,
                        "submit",
                        null,
                        null)));
    ScreeningRun submitted = lastRun("ARN-2026-900001", ScreeningTrigger.ACCOUNT_SUBMITTED);
    assertThat(submitted.getClientsScreened()).isEqualTo(1);
    assertThat(submitted.getMatches()).isZero();
  }

  @Test
  void aFalsePositiveNeedsJustificationAndEvidenceAndHoldsUntilTheEntryChanges() {
    String first = word();
    String last = word();
    LocalDate birth = someBirthDate();
    Long entryId = fx.listed(first + " " + last, birth);
    Client c = fx.prospect(fx.demoCompany(), person(first, last, birth));
    ScreeningMatch match = matchesOf(c.getId()).get(0);
    Long matchId = match.getId();

    FalsePositive blank = new FalsePositive(" ", null, null, null, null, null);
    assertThatThrownBy(() -> as(INVESTIGATOR, () -> decisions.markFalsePositive(matchId, blank)))
        .hasMessage("Enter the justification and attach the evidence");
    FalsePositive noEvidence = new FalsePositive("Different person", null, null, null, null, null);
    assertThatThrownBy(
            () -> as(INVESTIGATOR, () -> decisions.markFalsePositive(matchId, noEvidence)))
        .hasMessage("Attach at least one evidence document");

    evidence(matchId);
    MatchDecision decision =
        as(
            INVESTIGATOR,
            () ->
                decisions.markFalsePositive(
                    matchId,
                    new FalsePositive(
                        "Different birth date and nationality on the ID copy",
                        null,
                        "STANDARD",
                        null,
                        Set.of(WATCHLIST_REVIEW),
                        null)));
    assertThat(decision.match().status()).isEqualTo(MatchStatus.FALSE_POSITIVE);
    assertThat(decision.manualEntryId()).isNotNull();
    assertThat(clients.get(c.getId()).getRiskRating()).isEqualTo("STANDARD");
    assertThat(clientRisk.activeTags(c.getId())).doesNotContain(WATCHLIST_REVIEW);
    RiskProfileEntry manual = overrides.history(c.getId()).get(0);
    assertThat(manual.getSource()).isEqualTo(RiskSource.MANUAL);
    assertThat(manual.getEvidence()).isNotBlank();
    assertThatThrownBy(
            () ->
                as(
                    INVESTIGATOR,
                    () ->
                        decisions.markFalsePositive(
                            matchId,
                            new FalsePositive("again", List.of(1L), null, null, null, null))))
        .hasMessage("The match is already a false positive");
    assertThatThrownBy(() -> as(INVESTIGATOR, () -> decisions.openCase(matchId)))
        .hasMessage("Only a potential match not yet in a case can open a case");

    // Suppressed for the same entry version (FR-SS-035 R2)...
    ScreeningResult rerun =
        as(INVESTIGATOR, () -> engine.screenClient(c.getId(), ScreeningTrigger.MANUAL, null))
            .orElseThrow();
    assertThat(rerun.matches()).isEmpty();
    // ... and matched again once the entry changes (FR-SS-031 R3).
    fx.change(entryId, first + " " + last, birth.plusDays(1));
    List<ScreeningMatch> after = matchesOf(c.getId());
    assertThat(after).hasSize(2);
    assertThat(after.get(0).getStatus()).isEqualTo(MatchStatus.POTENTIAL);
    assertThat(after.get(0).getEntryVersion()).isGreaterThan(match.getEntryVersion());
  }

  @Test
  void confirmingAMatchEvaluatesTheRulesAndLinksTheCase() {
    String first = word();
    String last = word();
    fx.listed(first + " " + last, null);
    Client c = fx.prospect(fx.demoCompany(), person(first, last, null));
    ScreeningMatch match = matchesOf(c.getId()).get(0);
    MatchDecision confirmed =
        as(INVESTIGATOR, () -> decisions.confirm(match.getId(), 424242L, "Same person"));
    assertThat(confirmed.match().status()).isEqualTo(MatchStatus.TRUE_MATCH);
    assertThat(confirmed.match().caseId()).isEqualTo(424242L);
    assertThat(confirmed.riskOutcome().categoryCode()).isEqualTo("HIGH_SANCTION");
    assertThat(confirmed.riskOutcome().changed()).isFalse();
    assertThatThrownBy(() -> as(INVESTIGATOR, () -> decisions.confirm(match.getId(), null, null)))
        .hasMessage("The match is already TRUE MATCH");
    as(
        INVESTIGATOR,
        () -> {
          decisions.linkToCase(List.of(match.getId()), 434343L);
          return null;
        });
    assertThat(decisions.get(match.getId()).getCaseId()).isEqualTo(434343L);
  }

  @Test
  void aManualRiskChangeNeedsJustificationEvidenceAndAValidRating() {
    Client c = fx.prospect(fx.demoCompany(), person(word(), word(), someBirthDate()));
    Long file =
        as(
                INVESTIGATOR,
                () ->
                    attachments.upload(
                        new AttachmentTarget("Client", String.valueOf(c.getId())),
                        "edd.pdf",
                        ScreeningMatchingFixtures.PDF,
                        null))
            .getId();
    assertThatThrownBy(
            () ->
                as(
                    INVESTIGATOR,
                    () ->
                        overrides.override(
                            c.getId(),
                            new ManualRiskChange(
                                HIGH, null, null, " ", List.of(file), null, null, null))))
        .hasMessage("Enter the justification of the change");
    assertThatThrownBy(
            () ->
                as(
                    INVESTIGATOR,
                    () ->
                        overrides.override(
                            c.getId(),
                            new ManualRiskChange(
                                HIGH, null, null, "Adverse news", List.of(), null, null, null))))
        .hasMessage("Attach at least one evidence document");
    assertThatThrownBy(
            () ->
                as(
                    INVESTIGATOR,
                    () ->
                        overrides.override(
                            c.getId(),
                            new ManualRiskChange(
                                "VERY_LOW",
                                null,
                                null,
                                "Adverse news",
                                List.of(file),
                                null,
                                null,
                                null))))
        .hasMessage("Select a valid risk rating");
    assertThatThrownBy(
            () ->
                as(
                    INVESTIGATOR,
                    () ->
                        overrides.override(
                            c.getId(),
                            new ManualRiskChange(
                                HIGH,
                                Set.of("NOT_A_TAG"),
                                null,
                                "Adverse news",
                                List.of(file),
                                null,
                                null,
                                null))))
        .hasMessage("Select valid client tags");

    RiskProfileEntry entry =
        as(
            INVESTIGATOR,
            () ->
                overrides.override(
                    c.getId(),
                    new ManualRiskChange(
                        HIGH,
                        Set.of("PEP"),
                        null,
                        "Adverse news on the source of funds",
                        List.of(file),
                        null,
                        null,
                        "EDD-TEST")));
    assertThat(entry.getSource()).isEqualTo(RiskSource.MANUAL);
    assertThat(entry.getTagsAdded()).isEqualTo("PEP");
    assertThat(clients.get(c.getId()).getRiskRating()).isEqualTo(HIGH);
  }

  @Test
  void withoutMatchingCriteriaNoRunStartsAndAnAlertIsRaised() {
    Long company = fx.bareCompany();
    Client c = fx.prospect(company, person(word(), word(), someBirthDate()));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from scr_screening_run where company_id = ?", Long.class, company))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Long.class,
                ScreeningEngine.NO_CONFIG_ALERT + ":" + company))
        .isEqualTo(1);
    assertThat(engine.screenClient(c.getId(), ScreeningTrigger.MANUAL, null)).isEmpty();
  }

  @Test
  void theBatchScreensTheClientsInScopeOnlyAndIsIdempotent() {
    Long company = fx.configuredCompany();
    Client listed = fx.prospect(company, person("Juan", "Dela Cruz", someBirthDate()));
    Client other = fx.prospect(company, person(word(), word(), someBirthDate()));
    Client inactive = fx.prospect(company, person("Juan", "Dela Cruz", someBirthDate()));
    jdbc.update("update crm_client set status = 'INACTIVE' where id = ?", inactive.getId());
    jdbc.update(
        "delete from scr_name_key where subject_kind = 'CLIENT' and subject_id = ?", other.getId());

    ScreeningResult full = batch.periodic(company, LocalDate.of(2026, 10, 1), 77L).orElseThrow();
    assertThat(full.trigger()).isEqualTo(ScreeningTrigger.PERIODIC);
    assertThat(full.clientsScreened()).isEqualTo(2);
    assertThat(full.entriesScreened()).isPositive();
    assertThat(full.matches()).isEmpty();
    ScreeningRun fullRun = queries.run(full.runId());
    assertThat(fullRun.isFullRescreen()).isTrue();
    assertThat(fullRun.getScope()).isEqualTo("PROSPECT,CONFIRMED");
    assertThat(fullRun.getJobRunId()).isEqualTo(77L);
    assertThat(matchesOf(listed.getId())).isNotEmpty();
    assertThat(matchesOf(inactive.getId())).isNotEmpty();

    ScreeningResult delta = batch.periodic(company, LocalDate.of(2026, 10, 2), null).orElseThrow();
    assertThat(queries.run(delta.runId()).isFullRescreen()).isFalse();
    assertThat(delta.clientsScreened()).isEqualTo(2);
    assertThat(delta.matches()).isEmpty();
    assertThat(
            queries
                .runs(company, ScreeningTrigger.PERIODIC, PageRequest.of(0, 5))
                .getTotalElements())
        .isEqualTo(2);

    assertThat(batch.periodic(fx.bareCompany(), LocalDate.of(2026, 10, 2), null)).isEmpty();
    JobOutcome outcome = job.execute(LocalDate.of(2026, 10, 3));
    assertThat(outcome.message()).contains("screening run(s)");
    assertThat(job.name()).isEqualTo(PeriodicScreeningJob.JOB_NAME);
    assertThat(job.description()).contains("SNSRP-602");
    assertThat(job.cron()).isNotBlank();
  }
}
