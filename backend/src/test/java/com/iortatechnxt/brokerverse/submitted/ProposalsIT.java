package com.iortatechnxt.brokerverse.submitted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNominatedRate;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.submitted.proposal.service.NominatedRateService;
import com.iortatechnxt.brokerverse.submitted.proposal.service.ProposalDocument;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Line;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Proposed;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.StoredDownloads;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Renewal proposals with nominated rates and the preferred insurer (FR-SP-066, 067). */
@IntegrationTest
class ProposalsIT {

  private static final String TL = "sbmtl";
  private static final String MAKER = "sanitation";

  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private MasterlistService masterlist;
  @Autowired private SbmProcessingService processing;
  @Autowired private NominatedRateService rates;
  @Autowired private SbmProposalService proposals;
  @Autowired private ProposalDocument documents;
  @Autowired private StoredDownloads downloads;

  private Long company() {
    return data.company().getId();
  }

  private SbmPolicy forRenewal() {
    String pn = "PN-P-" + System.nanoTime();
    LocalDate expiry = LocalDate.now().plusDays(60);
    SbmPolicyData d =
        new SbmPolicyData(
            "NONCBG_RETAIL",
            SbmBusinessType.NB,
            new SbmLoan(pn, null, "CIF-1", null, null, null, null, "Borrower"),
            new SbmAssured("Assured " + pn, "Makati City", null, null, "a@example.ph", null),
            new SbmTerms(
                "INS-LAC",
                "POL-" + pn,
                expiry.minusYears(1),
                expiry,
                null,
                new BigDecimal("900000"),
                new BigDecimal("15000.00"),
                "PHP"),
            new SbmRisk(
                null, null, null, null, null, null, null, "Makati City", "Residential", null),
            SbmMarks.NONE);
    SbmPolicy p =
        as.run(
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
    as.run(
        MAKER,
        () ->
            processing.run(
                new RunRequest(company(), SbmRun.Trigger.MANUAL, "IT", List.of(p.getId()))));
    SbmPolicy processed = as.run(TL, () -> masterlist.get(p.getId()));
    assertThat(processed.getStatus()).isEqualTo(SbmPolicyStatus.FOR_RENEWAL);
    return processed;
  }

  @Test
  void proposalsAreGeneratedWithTheNominatedRateReleasedAndReassigned() {
    SbmNominatedRate rate =
        as.run(
            TL,
            () ->
                rates.create(
                    company(),
                    new SbmNominatedRate.Row(
                        "NONCBG_RETAIL",
                        null,
                        "INS-MGIC",
                        new BigDecimal("0.25"),
                        LocalDate.of(2026, 1, 1),
                        null)));
    as.run(MAKER, () -> rates.authorize(company(), rate.getId()));
    SbmPolicy p = forRenewal();

    Proposed proposed =
        as.run(MAKER, () -> proposals.preview(company(), List.of(p.getId()))).get(0);
    assertThat(proposed.insurerCode()).isEqualTo("INS-MGIC");
    assertThat(proposed.nominatedRate()).isEqualByComparingTo("0.25");
    assertThat(proposed.premium()).isEqualByComparingTo("2250.00");

    assertThatThrownBy(
            () ->
                as.run(
                    MAKER,
                    () ->
                        proposals.generate(
                            company(),
                            List.of(new Line(p.getId(), null, new BigDecimal("0.30"), null)))))
        .extracting("code")
        .isEqualTo("SBM_PROPOSAL_RATE_REASON");
    SbmProposalBatch batch =
        as.run(
            MAKER,
            () -> proposals.generate(company(), List.of(new Line(p.getId(), null, null, null))));
    assertThat(batch.getBatchNo()).startsWith("SBP-");
    SbmProposal first = proposals.ofBatch(batch.getId()).get(0);
    assertThat(first.getStatus()).isEqualTo(SbmProposal.Status.FOR_REVIEW);
    assertThat(first.getVersionNo()).isEqualTo(1);
    assertThat(downloads.bytes(as.run(TL, () -> documents.pdf(first.getId()))))
        .startsWith((byte) '%');

    assertThatThrownBy(
            () -> as.run(MAKER, () -> proposals.assignInsurer(List.of(first.getId()), "INS-LAC")))
        .extracting("code")
        .isEqualTo("SBM_PROPOSAL_NO_RATE");
    assertThatThrownBy(() -> as.run(MAKER, () -> proposals.release(first.getId())))
        .extracting("code")
        .isEqualTo("SBM_PROPOSAL_MAKER");
    as.run(TL, () -> proposals.release(first.getId()));
    assertThatThrownBy(
            () -> as.run(MAKER, () -> proposals.assignInsurer(List.of(first.getId()), "INS-MGIC")))
        .extracting("code")
        .isEqualTo("SBM_PROPOSAL_RELEASED");
    as.run(TL, () -> proposals.returnProposal(first.getId(), "Use the bank rate"));

    SbmProposalBatch second =
        as.run(
            MAKER,
            () ->
                proposals.generate(
                    company(),
                    List.of(new Line(p.getId(), null, new BigDecimal("0.30"), "Bank rate"))));
    SbmProposal v2 = proposals.ofBatch(second.getId()).get(0);
    assertThat(v2.getVersionNo()).isEqualTo(2);
    assertThat(v2.getAppliedRate()).isEqualByComparingTo("0.30");
    assertThat(v2.getRateReason()).isEqualTo("Bank rate");
    assertThat(proposals.proposal(first.getId()).getStatus())
        .isEqualTo(SbmProposal.Status.SUPERSEDED);
    as.run(TL, () -> rates.deactivate(company(), rate.getId()));
  }
}
