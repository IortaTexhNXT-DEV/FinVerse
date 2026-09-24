package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionTerms;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionReviewJob;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService.DrillDown;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService.RuleStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class RetentionIT {

  @Autowired private RetentionService retention;
  @Autowired private RetentionReviewJob job;
  @Autowired private AsUser as;

  private RuleStatus rule(String recordType, String statuses) {
    return retention.rules().stream()
        .filter(s -> s.rule().getRecordType().equals(recordType))
        .filter(s -> s.rule().getStatuses().equals(statuses))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void reviewCountsEligibleRecordsPerRuleWithoutDeletingAnything() {
    RuleStatus inactiveClients = rule("CLIENT", "INACTIVE");
    assertThat(inactiveClients.providerAvailable()).isTrue();
    assertThat(inactiveClients.rule().getYearsOnline()).isEqualTo(5);
    assertThat(inactiveClients.rule().getYearsArchive()).isEqualTo(15);
    assertThat(rule("QUOTATION", "NOT_PROCEEDED,VOIDED").rule().getAction())
        .isEqualTo(RetentionAction.ARCHIVE);

    JobOutcome outcome = job.execute(LocalDate.now(ZoneOffset.UTC));
    assertThat(outcome.itemsProcessed()).isGreaterThanOrEqualTo(5);
    RuleStatus after = rule("CLIENT", "INACTIVE");
    assertThat(after.latestRun()).isNotNull();
    assertThat(after.latestRun().getEligibleCount()).isPositive();
    assertThat(after.latestRun().getRunBy()).isEqualTo("SYSTEM");

    DrillDown drill = retention.eligible(after.rule().getId(), 50);
    assertThat(drill.providerAvailable()).isTrue();
    assertThat(drill.records())
        .anySatisfy(r -> assertThat(r.reference()).isEqualTo("PR-2026-000012"));
    assertThat(retention.eligible(rule("CLIENT", "PROSPECT").rule().getId(), 50).records())
        .anySatisfy(r -> assertThat(r.reference()).isEqualTo("PR-2026-000011"));
  }

  @Test
  void rulesWithoutAProviderAreCountedLater() {
    RuleStatus accounts = rule("ACCOUNT", "VOIDED,CANCELLED");
    if (!accounts.providerAvailable()) {
      DrillDown drill = retention.eligible(accounts.rule().getId(), 10);
      assertThat(drill.records()).isEmpty();
      as.run("badmin", () -> retention.review(LocalDate.now(ZoneOffset.UTC)));
      assertThat(rule("ACCOUNT", "VOIDED,CANCELLED").latestRun().isProviderAvailable()).isFalse();
    }
    assertThat(accounts.rule().cutoff(LocalDate.of(2026, 9, 1)))
        .isEqualTo(LocalDate.of(2021, 9, 1));
  }

  @Test
  void rulesAreMaintainedWithValidation() {
    RetentionRule prospects = rule("CLIENT", "PROSPECT").rule();
    RetentionTerms original =
        new RetentionTerms(
            prospects.getStatuses(),
            prospects.getYearsOnline(),
            prospects.getYearsArchive(),
            prospects.getAction(),
            prospects.isActive(),
            prospects.getDescription());
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        retention.update(
                            prospects.getId(),
                            new RetentionTerms(" , ", 5, 15, RetentionAction.REVIEW, true, "x"))))
        .extracting("code")
        .isEqualTo("RETENTION_STATUSES");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        retention.update(
                            prospects.getId(),
                            new RetentionTerms(
                                "PROSPECT", 0, 15, RetentionAction.REVIEW, true, "x"))))
        .extracting("code")
        .isEqualTo("RETENTION_YEARS");
    RetentionRule changed =
        as.run(
            "badmin",
            () ->
                retention.update(
                    prospects.getId(),
                    new RetentionTerms(
                        "prospect", 6, 10, RetentionAction.REVIEW, false, "Dormant prospects")));
    assertThat(changed.getStatuses()).isEqualTo("PROSPECT");
    assertThat(changed.getYearsOnline()).isEqualTo(6);
    assertThat(changed.isActive()).isFalse();
    as.run("badmin", () -> retention.update(prospects.getId(), original));
  }
}
