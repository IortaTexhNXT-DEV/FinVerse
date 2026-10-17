package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Codes;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSet;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.PackageChoice;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyParties;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyPolicy;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageChoiceService;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageMapService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RenewalSetupService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService.Header;
import com.iortatechnxt.brokerverse.renewal.setup.service.RuleVersionService.Step;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Renewal wave R1-A: extraction with the checks, initiation and routing, the lists in scope,
 * Renewal Setup with maker and checker, and the package remapping of migrated policies.
 */
@IntegrationTest
class RenewalFoundationIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private ExtractionService extraction;
  @Autowired private InitiationService initiation;
  @Autowired private CandidateQueryService queries;
  @Autowired private RenewalSetupService setup;
  @Autowired private RuleVersionService versions;
  @Autowired private PackageMapService map;
  @Autowired private PackageChoiceService choices;
  @Autowired private AsUser as;

  private CandidateFilter search(String text) {
    return new CandidateFilter(fx.company(), Tab.ALL, text, null, null, Codes.NONE, Flags.NONE);
  }

  @Test
  void anExpiringInvoiceIsExtractedOnceWithItsChecksAndBucket() {
    RenewalCandidate c = fx.extractedMotor();
    assertThat(c.getStage()).isEqualTo(RenewalStage.EXTRACTED);
    assertThat(c.getSource()).isEqualTo(CandidateSource.BIBS_INVOICE);
    assertThat(c.getRenewalRef()).startsWith("RNW-");
    assertThat(c.getBucket()).isNotNull();
    assertThat(c.getSnapshot().clientName()).isNotBlank();
    List<CheckResult> results = as.run(PROC_TL, () -> queries.latestResults(c));
    assertThat(results).extracting(CheckResult::getCheckCode).contains("REFERENCE_MATCH", "CLAIMS");
    assertThat(results)
        .filteredOn(r -> r.getCheckCode().equals("CLAIMS"))
        .extracting(CheckResult::getOutcome)
        .containsExactly(CheckOutcome.PASS);

    var again =
        as.run(
            TL, () -> extraction.extractRange(fx.company(), c.getExpiryDate(), c.getExpiryDate()));
    assertThat(again.getNewCount()).isZero();
    assertThat(again.getExistingCount()).isPositive();
  }

  @Test
  void initiationRoutesTheRenewalAndTheListsShowIt() {
    RenewalCandidate c = fx.extractedMotor();
    var outcome = as.run(TL, () -> initiation.initiate(fx.company(), List.of(c.getRenewalRef())));
    assertThat(outcome.refused()).isEmpty();
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isNotIn(RenewalStage.EXTRACTED, RenewalStage.EVALUATING);
    assertThat(after.getInitiatedBy()).isEqualTo(TL);

    var page = as.run(PROC_TL, () -> queries.list(search(c.getRenewalRef()), 0, 50));
    assertThat(page.getContent())
        .extracting(RenewalCandidate::getRenewalRef)
        .containsExactly(c.getRenewalRef());
    var initiatedAgain =
        as.run(TL, () -> initiation.initiate(fx.company(), List.of(c.getRenewalRef())));
    assertThat(initiatedAgain.refused()).containsKey(c.getRenewalRef());
  }

  @Test
  void aNonRenewableRiskCodeNeedsAnotherUserToAuthorize() {
    NonRenewableRiskCode code =
        as.run(
            ADMIN,
            () ->
                setup.createRiskCode(
                    fx.company(),
                    new NonRenewableRiskCode.Data(
                        "ZZ" + System.nanoTime() % 1000,
                        null,
                        "Withdrawn cover",
                        LocalDate.of(2026, 1, 1),
                        null)));
    assertThatThrownBy(
            () -> as.run(ADMIN, () -> setup.authorizeRiskCode(fx.company(), code.getId())))
        .isInstanceOf(BusinessRuleException.class);
    NonRenewableRiskCode authorized =
        as.run("approver", () -> setup.authorizeRiskCode(fx.company(), code.getId()));
    assertThat(authorized.isActive()).isTrue();
  }

  @Test
  void aBucketRuleVersionIsActivatedByAnotherUserAndRetiresThePreviousOne() {
    List<BucketRule.Data> rules =
        List.of(
            new BucketRule.Data(
                1, null, CheckSeverity.FAIL_EXCEPTION, CheckOutcome.FAIL, Bucket.EXCEPTION),
            new BucketRule.Data(2, null, null, CheckOutcome.FAIL, Bucket.REVIEW));
    assertThatThrownBy(
            () ->
                as.run(
                    ADMIN,
                    () ->
                        versions.saveBucketRules(
                            fx.company(),
                            null,
                            new Header(null, "Invalid"),
                            List.of(
                                new BucketRule.Data(
                                    1, null, null, CheckOutcome.FAIL, Bucket.CLEAN)))))
        .isInstanceOf(BusinessRuleException.class);
    BucketRuleSet first = activate(rules);
    BucketRuleSet second = activate(rules);
    assertThat(second.getVersionNo()).isGreaterThan(first.getVersionNo());
    assertThat(as.run(ADMIN, () -> versions.bucketRuleSets(fx.company())))
        .filteredOn(s -> s.getId().equals(first.getId()))
        .extracting(BucketRuleSet::getStatus)
        .containsExactly(RuleSetStatus.RETIRED);
  }

  private BucketRuleSet activate(List<BucketRule.Data> rules) {
    BucketRuleSet draft =
        as.run(
            ADMIN,
            () -> versions.saveBucketRules(fx.company(), null, new Header(null, "Rules"), rules));
    as.run(ADMIN, () -> versions.decideBucketRules(fx.company(), draft.getId(), Step.SUBMIT, null));
    assertThatThrownBy(
            () ->
                as.run(
                    ADMIN,
                    () ->
                        versions.decideBucketRules(
                            fx.company(), draft.getId(), Step.ACTIVATE, null)))
        .isInstanceOf(BusinessRuleException.class);
    return as.run(
        "approver",
        () -> versions.decideBucketRules(fx.company(), draft.getId(), Step.ACTIVATE, null));
  }

  @Test
  void anUnmappedLegacyPackageGoesToTheExceptionBucketUntilAChoiceIsApproved() {
    String pkg = "QPS-T" + System.nanoTime() % 100000;
    RenewalCandidate c = legacy(pkg);
    assertThat(c.getBucket()).isEqualTo(Bucket.EXCEPTION);
    assertThat(failed(c)).contains("PACKAGE_REMAP");

    PackageChoice choice =
        as.run(
            PO, () -> choices.propose(fx.company(), c.getRenewalRef(), "MTR12", 1, "Same cover"));
    assertThatThrownBy(() -> as.run(PO, () -> choices.decide(choice.getId(), true, null)))
        .isInstanceOf(BusinessRuleException.class);
    as.run(PROC_TL, () -> choices.decide(choice.getId(), true, null));
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getResolvedProductCode()).isEqualTo("MTR12");
    assertThat(failed(after)).doesNotContain("PACKAGE_REMAP");
  }

  @Test
  void anAuthorizedMapEntryResolvesTheLegacyPackageAtExtraction() {
    String pkg = "QPS-M" + System.nanoTime() % 100000;
    PackageMapEntry entry =
        as.run(
            PO,
            () ->
                map.create(
                    fx.company(),
                    new PackageMapEntry.Data(pkg, null, null, null, null, null, "MTR12", 1, null),
                    PackageMapService.SOURCE_SETUP));
    as.run(PROC_TL, () -> map.authorize(fx.company(), entry.getId()));
    assertThatThrownBy(
            () ->
                as.run(
                    PO,
                    () ->
                        map.create(
                            fx.company(),
                            new PackageMapEntry.Data(
                                pkg, null, null, null, null, null, "MTR12", 2, null),
                            PackageMapService.SOURCE_SETUP)))
        .isInstanceOf(BusinessRuleException.class);
    RenewalCandidate c = legacy(pkg);
    assertThat(c.getResolvedProductCode()).isEqualTo("MTR12");
    assertThat(c.getResolvedVersionNo()).isEqualTo(1);
    assertThat(failed(c)).doesNotContain("PACKAGE_REMAP");
  }

  private List<String> failed(RenewalCandidate c) {
    return as.run(PROC_TL, () -> queries.latestResults(fx.reload(c))).stream()
        .filter(r -> r.getOutcome() == CheckOutcome.FAIL)
        .map(CheckResult::getCheckCode)
        .toList();
  }

  private RenewalCandidate legacy(String pkg) {
    String ref = "QPS-" + pkg;
    return as.run(
        PROC_TL,
        () ->
            extraction.createLegacy(
                fx.company(),
                new LegacyHeader(
                    ref,
                    "QPS",
                    null,
                    new LegacyPolicy(
                        "FI-" + ref,
                        null,
                        "MTR12",
                        "MOTOR",
                        pkg,
                        null,
                        LocalDate.of(2027, 2, 1),
                        LocalDate.of(2028, 2, 1),
                        new BigDecimal("900000.00"),
                        new BigDecimal("15000.00"),
                        "PHP",
                        "PN-" + ref),
                    new LegacyParties(
                        "CL-2026-000001",
                        "Seed Client",
                        null,
                        "INS-MGIC",
                        "ao",
                        "T-CBG1",
                        "CBG",
                        null),
                    false,
                    null),
                null,
                false));
  }
}
