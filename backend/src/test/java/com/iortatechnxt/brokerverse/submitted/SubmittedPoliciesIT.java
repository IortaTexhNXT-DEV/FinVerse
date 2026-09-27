package com.iortatechnxt.brokerverse.submitted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReview;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResultRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import com.iortatechnxt.brokerverse.submitted.fee.service.HandlingFeeService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.submitted.renewal.service.HandOffService;
import com.iortatechnxt.brokerverse.submitted.review.service.IaafService;
import com.iortatechnxt.brokerverse.submitted.review.service.ReviewSlaCheck;
import com.iortatechnxt.brokerverse.submitted.review.service.TorService;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedRetentionProvider;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import com.iortatechnxt.brokerverse.submitted.setup.service.SetupService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Submitted Policies (BRD-12): intake into the masterlist, a processing run with its step results
 * and fallout, the handler's exclusion, the review with the IAAF approved and sent, a TOR of a
 * policy above the insurer limits, the renewal hand-off taken by Renewal (wave R3), a handling fee
 * billed, the setup maker and checker, the reports, the retention and the review alerts.
 */
@IntegrationTest
class SubmittedPoliciesIT {

  private static final String TL = "sbmtl";

  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private MasterlistService masterlist;
  @Autowired private SbmProcessingService processing;
  @Autowired private SbmRunResultRepository results;
  @Autowired private IaafService iaafs;
  @Autowired private TorService tors;
  @Autowired private HandOffService handOff;
  @Autowired private RenewalHandOff port;
  @Autowired private RenewalCandidateRepository candidates;
  @Autowired private HandlingFeeService fees;
  @Autowired private SetupService setup;
  @Autowired private ReportService reports;
  @Autowired private SubmittedRetentionProvider retention;
  @Autowired private ReviewSlaCheck sla;

  private Long company() {
    return data.company().getId();
  }

  private SbmPolicy intake(String segment, String pn, String insurer, String sumInsured) {
    LocalDate expiry = LocalDate.now().plusDays(60);
    SbmPolicyData d =
        new SbmPolicyData(
            segment,
            SbmBusinessType.NB,
            new SbmLoan(pn, null, "CIF-1", null, null, null, null, "Seed Borrower"),
            new SbmAssured(
                "Assured " + pn, "Makati City", null, null, "a@example.ph", "bank@example.ph"),
            new SbmTerms(
                insurer,
                "POL-" + pn,
                expiry.minusYears(1),
                expiry,
                null,
                new BigDecimal(sumInsured),
                new BigDecimal("15000.00"),
                "PHP"),
            new SbmRisk(
                null, null, null, null, null, null, null, "Makati City", "Residential", null),
            SbmMarks.NONE);
    return as.run(
        TL,
        () ->
            masterlist
                .upsert(
                    company(),
                    d,
                    UpsertContext.of(
                        "SPI",
                        null,
                        LocalDate.now(),
                        SbmPolicyStatus.RECEIVED,
                        SbmHistorySource.INTAKE,
                        "IT"))
                .policy());
  }

  private SbmPolicy processed(SbmPolicy p) {
    as.run(
        "sanitation",
        () ->
            processing.run(
                new RunRequest(company(), SbmRun.Trigger.MANUAL, "IT", List.of(p.getId()))));
    return as.run(TL, () -> masterlist.get(p.getId()));
  }

  private static String pn(String prefix) {
    return prefix + "-" + System.nanoTime();
  }

  @Test
  void aRetailPolicyIsProcessedForRenewalAndHandedToRenewal() {
    SbmPolicy p = processed(intake("NONCBG_RETAIL", pn("PN-R"), "INS-LAC", "900000"));
    assertThat(p.getStatus()).isEqualTo(SbmPolicyStatus.FOR_RENEWAL);
    assertThat(p.getBucket()).isEqualTo("FOR_RENEWAL");
    assertThat(p.getClassification()).isNotNull();
    assertThat(results.findByPolicyIdOrderByIdDesc(p.getId())).hasSizeGreaterThanOrEqualTo(4);

    SbmRenewal r = as.run(TL, () -> handOff.renewWithBdoi(p.getId()));
    assertThat(r.getInsurerAssigned()).isEqualTo("INS-MGIC");
    assertThat(r.getHandoffStatus()).isEqualTo(SbmRenewal.HANDED_OFF);
    RenewalCandidate c =
        candidates
            .findByCompanyIdAndSourceAndSourceRef(
                company(), CandidateSource.SUBMITTED_POLICY, p.getSbmNo())
            .orElseThrow();
    assertThat(c.getRenewalRef()).isEqualTo(r.getRenewalRef());
    assertThat(c.getSnapshot().insurerCode()).isEqualTo("INS-MGIC");
    assertThat(as.run(TL, () -> masterlist.get(p.getId())).getStatus())
        .isEqualTo(SbmPolicyStatus.RENEWAL_IN_PROGRESS);
    assertThat(port.status(company(), p.getSbmNo()))
        .get()
        .extracting(RenewalHandOff.HandOffStatus::state)
        .isEqualTo(RenewalHandOff.State.OPEN);
    assertThatThrownBy(() -> as.run(TL, () -> handOff.renewWithBdoi(p.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void aCbgPolicyWithoutLoanFallsOutAndIsExcludedByTheHandler() {
    SbmPolicy p = processed(intake("CBG_MOTOR", pn("PN-X"), "INS-MGIC", "800000"));
    assertThat(p.isFallout()).isTrue();
    assertThat(p.getFalloutReason()).isEqualTo("NO_LAMD_RECORD");
    SbmPolicy excluded =
        as.run("sanitation", () -> masterlist.act(p.getId(), "exclude", "INCORRECT_DATA", "IT"));
    assertThat(excluded.getStatus()).isEqualTo(SbmPolicyStatus.EXCLUDED);
    assertThat(excluded.getRenewalTag()).isEqualTo("NON_RENEWABLE");
    assertThat(as.run(TL, () -> masterlist.get(p.getId()))).isNotNull();
  }

  @Test
  void aReviewedPolicyGetsItsIaafApprovedAndSent() {
    SbmPolicy p = intake("NONCBG_CORPORATE", pn("PN-C"), "INS-MPI", "12000000");
    as.run(
        "polreview",
        () ->
            iaafs.review(
                p.getId(),
                new SbmIaafReview.Content(
                    LocalDate.now(),
                    SbmIaafReview.WITH_FINDINGS,
                    List.of("MORTGAGEE_CLAUSE_MISSING"),
                    "")));
    assertThatThrownBy(() -> as.run("polreview", () -> iaafs.generate(p.getId(), Map.of())))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        "polreview",
        () ->
            iaafs.review(
                p.getId(),
                new SbmIaafReview.Content(
                    LocalDate.now(), SbmIaafReview.ADEQUATE, List.of(), "OK")));
    SbmIaaf i = as.run("polreview", () -> iaafs.generate(p.getId(), Map.of()));
    as.run("polreview", () -> iaafs.submit(i.getId()));
    assertThatThrownBy(() -> as.run("polreview", () -> iaafs.approve(i.getId())))
        .isInstanceOf(RuntimeException.class);
    SbmIaaf approved = as.run("sbmchecker", () -> iaafs.approve(i.getId()));
    assertThat(approved.getStatus()).isEqualTo(SbmDocStatus.APPROVED);
    SbmIaaf sent = as.run("polreview", () -> iaafs.issue(i.getId()));
    assertThat(sent.getStatus()).isEqualTo(SbmDocStatus.ISSUED);
    assertThat(sent.getSentTo()).isEqualTo("bank@example.ph");
  }

  @Test
  void aPolicyAboveTheInsurerLimitGetsATorApprovedByTsu() {
    SbmPolicy p = processed(intake("NONCBG_CORPORATE", pn("PN-T"), "INS-MPI", "150000000"));
    assertThat(p.isInsurerApprovalRequired()).isTrue();
    assertThat(as.run(TL, () -> tors.breaches(p.getId()))).isNotBlank();
    SbmTor t =
        as.run(TL, () -> tors.generate(p.getId(), "Co-insurance with a second insurer", "ao"));
    as.run(TL, () -> tors.submit(t.getId()));
    SbmTor approved = as.run("tsu", () -> tors.approve(t.getId()));
    assertThat(approved.getStatus()).isEqualTo(SbmDocStatus.APPROVED);
  }

  @Test
  void aHandlingFeeIsBilledAndListed() {
    SbmPolicy p = intake("NONCBG_RETAIL", pn("PN-F"), "INS-LAC", "500000");
    SbmHandlingFee f =
        as.run(
            "sbmfee",
            () ->
                fees.bill(
                    company(),
                    new SbmHandlingFee.Bill(
                        p.getId(),
                        p.getLoan().pnNo(),
                        null,
                        new BigDecimal("1120.00"),
                        "PHP",
                        LocalDate.now()),
                    null));
    assertThat(f.getStatus()).isEqualTo(SbmHandlingFee.BILLED);
    assertThat(
            as.run(
                    "sbmfee",
                    () ->
                        fees.list(
                            company(), List.of(SbmHandlingFee.BILLED), PageRequest.of(0, 500)))
                .getContent())
        .extracting(SbmHandlingFee::getFeeNo)
        .contains(f.getFeeNo());
    SbmHandlingFee cancelled = as.run("sbmfee", () -> fees.cancel(f.getId(), "Billed twice"));
    assertThat(cancelled.getStatus()).isEqualTo(SbmHandlingFee.CANCELLED);
  }

  @Test
  void aLimitRuleWaitsForItsChecker() {
    SbmLimitRule r =
        as.run(
            TL,
            () ->
                setup.saveLimitRule(
                    company(),
                    null,
                    new SbmLimitRule.Limits(
                        "INS-VMI",
                        "NONCBG_RETAIL",
                        null,
                        new BigDecimal("2000000"),
                        null,
                        null,
                        null,
                        "IT")));
    assertThat(r.getRecordStatus()).isNotEqualTo(RecordStatus.ACTIVE);
    assertThatThrownBy(() -> as.run(TL, () -> setup.authorize("LIMIT", r.getId())))
        .isInstanceOf(RuntimeException.class);
    assertThat(as.run("mkttl", () -> setup.authorize("LIMIT", r.getId())).getRecordStatus())
        .isEqualTo(RecordStatus.ACTIVE);
  }

  @Test
  void everyReportRunsAndTheRetentionAndAlertsAnswer() {
    intake("NONCBG_RETAIL", pn("PN-P"), "INS-LAC", "700000");
    Map<String, String> params = new HashMap<>();
    params.put("companyId", company().toString());
    for (String code :
        List.of(
            "SBM-MASTERLIST",
            "SBM-DOC-FALLOUT",
            "SBM-PROCESS-FALLOUT",
            "SBM-NON-RENEWAL",
            "SBM-MIGRATION-ERRORS",
            "SBM-SANITATION",
            "SBM-DISPOSITION",
            "SBM-CLASSIFICATION",
            "SBM-RENEWABLE",
            "SBM-IAAF",
            "SBM-TOR",
            "SBM-HANDLING-FEE",
            "SBM-CONVERSION",
            "SBM-NO-TOUCH",
            "SBM-PR-CONVERSION",
            "SBM-PR-MONITORING",
            "SBM-PERSISTENCY",
            "SBM-PENETRATION",
            "SBM-HOLD-COVER-GAP",
            "SBM-LETTERS")) {
      ReportResult result = as.run(TL, () -> reports.run(code, params));
      assertThat(result.code()).isEqualTo(code);
    }
    assertThat(as.run(TL, () -> reports.run("SBM-MASTERLIST", params).rows())).isNotEmpty();
    assertThat(
            retention.countEligible(
                new RetentionCriteria(Set.of("EXCLUDED", "CLOSED"), LocalDate.of(2099, 1, 1))))
        .isGreaterThanOrEqualTo(0);
    assertThat(sla.evaluate(LocalDate.now().plusDays(60))).isNotNull();
  }
}
