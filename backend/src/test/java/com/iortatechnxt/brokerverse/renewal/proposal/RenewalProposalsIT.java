package com.iortatechnxt.brokerverse.renewal.proposal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.approval.service.ClientDecisions;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalProposal;
import com.iortatechnxt.brokerverse.renewal.domain.TsuRequest;
import com.iortatechnxt.brokerverse.renewal.kyc.service.KycMonitoring;
import com.iortatechnxt.brokerverse.renewal.proposal.service.RenewalProposals;
import com.iortatechnxt.brokerverse.renewal.proposal.service.TsuRequests;
import com.iortatechnxt.brokerverse.renewal.setup.service.RiskCodeMaintenance;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proposals, TSU requests, KYC monitoring and risk code maintenance of BDOI's FRS against the real
 * database (FRRN.017 to FRRN.019, FRRN.038, FRRN.039).
 */
@IntegrationTest
class RenewalProposalsIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private RenewalProposals proposals;
  @Autowired private TsuRequests tsu;
  @Autowired private ClientDecisions decisions;
  @Autowired private KycMonitoring kyc;
  @Autowired private com.iortatechnxt.brokerverse.renewal.check.service.CheckEngine engine;

  @Autowired
  private com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository candidates;

  @Autowired private org.springframework.transaction.support.TransactionTemplate tx;
  @Autowired private RiskCodeMaintenance riskCodes;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private AsUser as;

  private RenewalCandidate disposed(
      RenewalDisposition disposition, BigDecimal tsi, boolean packaged) {
    RenewalCandidate c = fx.unassignedRetail();
    jdbc.update(
        "update rnw_candidate set stage = 'FOR_DISPOSITION', disposition = ?,"
            + " total_sum_insured = ?, packaged = ?, assigned_ao = 'ao' where id = ?",
        disposition.name(),
        tsi,
        packaged,
        c.getId());
    return fx.reload(c);
  }

  @Test
  void aProposalIsGeneratedOnlyForProposalAndAFullOneAboveTheThresholdNeedsThreeSignatories() {
    RenewalCandidate renewal = disposed(RenewalDisposition.FOR_RENEWAL, BigDecimal.TEN, true);
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () -> proposals.generate(fx.company(), renewal.getRenewalRef(), "QUICK", null)))
        .isInstanceOf(BusinessRuleException.class);
    RenewalCandidate big =
        disposed(RenewalDisposition.FOR_PROPOSAL, new BigDecimal("600000000"), false);
    assertThat(proposals.requiredSignatories(big)).isEqualTo(3);
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        proposals.generate(
                            fx.company(), big.getRenewalRef(), "FULL", List.of(AO, TL))))
        .hasMessage(RenewalProposals.SIGNATORIES_REQUIRED);
    RenewalProposal full =
        as.run(
            AO,
            () ->
                proposals.generate(
                    fx.company(), big.getRenewalRef(), "FULL", List.of(AO, TL, ADMIN)));
    assertThat(full.getFileName()).startsWith("Renewal_FullProposal_" + big.getRenewalRef() + "_");
    RenewalProposal quick =
        as.run(AO, () -> proposals.generate(fx.company(), big.getRenewalRef(), "QUICK", null));
    assertThat(quick.getFileName()).startsWith("Renewal_QuickProposal_").endsWith(".pdf");
    assertThat(quick.getAttachmentId()).isNotNull();
  }

  @Test
  void aProposalIsSentThroughCcmAnInvalidAddressIsRefusedAndARejectedProposalIsLostBusiness() {
    RenewalCandidate c = disposed(RenewalDisposition.FOR_PROPOSAL, BigDecimal.TEN, true);
    RenewalProposal p =
        as.run(AO, () -> proposals.generate(fx.company(), c.getRenewalRef(), "QUICK", null));
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        proposals.send(
                            fx.company(),
                            c.getRenewalRef(),
                            p.getProposalNo(),
                            List.of("nope"),
                            List.of())))
        .hasMessage(RenewalProposals.INVALID);
    assertThat(
            as.run(
                AO,
                () ->
                    proposals.send(
                        fx.company(),
                        c.getRenewalRef(),
                        p.getProposalNo(),
                        List.of("client@client.example.ph"),
                        List.of())))
        .isEqualTo(RenewalProposals.SENT);
    assertThat(fx.reload(c).getPlacement().getDecision().getClientStatus())
        .isEqualTo(CandidateDecision.PENDING);
    as.run(
        AO,
        () -> {
          decisions.record(
              fx.company(), c.getRenewalRef(), CandidateDecision.REJECTED, "Too costly");
          return null;
        });
    RenewalCandidate lost = fx.reload(c);
    assertThat(lost.getDisposition().code()).isEqualTo(RenewalDisposition.NOT_FOR_RENEWAL);
    assertThat(lost.getDisposition().reasonCode()).isEqualTo("LOST_BUSINESS");
  }

  @Test
  void aTsuRequestIsApprovedByTheTeamLeadAssignedAndASecondRequestIsRefused() {
    RenewalCandidate c = disposed(RenewalDisposition.FOR_QUOTATION, BigDecimal.TEN, false);
    TsuRequest created =
        as.run(AO, () -> tsu.create(fx.company(), c.getRenewalRef(), "Large fleet"));
    as.run(AO, () -> tsu.submit(fx.company(), created.getRequestNo()));
    assertThatThrownBy(() -> as.run(AO, () -> tsu.create(fx.company(), c.getRenewalRef(), null)))
        .hasMessageContaining("already exists");
    assertThat(as.run(AO, () -> tsu.of(fx.company(), c.getRenewalRef())).get(0).status())
        .isEqualTo(TsuRequest.PENDING);
    as.run(TL, () -> tsu.decide(fx.company(), created.getRequestNo(), TsuRequest.APPROVED, null));
    as.run(TL, () -> tsu.assign(fx.company(), created.getRequestNo(), "tsu"));
    as.run(
        TL,
        () -> {
          tsu.quote(
              fx.company(),
              created.getRequestNo(),
              new TsuRequests.Quote(
                  c.getSnapshot().insurerCode(), new BigDecimal("1500.00"), "Standard"));
          return null;
        });
    TsuRequests.View view = as.run(AO, () -> tsu.of(fx.company(), c.getRenewalRef())).get(0);
    assertThat(view.status()).isEqualTo(TsuRequest.PROCESSING);
    assertThat(view.quotes()).hasSize(1);
  }

  @Test
  void theKycStatusIsRecordedWithItsHistory() {
    RenewalCandidate c = fx.unassignedRetail();
    jdbc.update("update rnw_candidate set assigned_ao = ? where id = ?", AO, c.getId());
    as.run(
        AO,
        () -> {
          kyc.record(
              fx.company(), c.getRenewalRef(), KycMonitoring.FOLLOW_UP, "Called the client", null);
          kyc.record(
              fx.company(), c.getRenewalRef(), KycMonitoring.COMPLETED, "Documents received", null);
          return null;
        });
    KycMonitoring.Monitoring m = as.run(AO, () -> kyc.of(fx.company(), c.getRenewalRef()));
    assertThat(m.status()).isEqualTo(KycMonitoring.COMPLETED);
    assertThat(m.history()).hasSizeGreaterThanOrEqualTo(2);
    Map<String, Object> counts = as.run(AO, () -> kyc.dashboard(fx.company()));
    assertThat(((Number) counts.get("completed")).intValue()).isPositive();
    assertThat(as.run(AO, () -> kyc.accounts(fx.company(), KycMonitoring.COMPLETED)))
        .anyMatch(r -> c.getRenewalRef().equals(r.get("renewalRef")));
  }

  @Test
  void theKycCountsMatchTheKycDueTagsOfTheChecks() {
    RenewalCandidate c = fx.unassignedRetail();
    Long clientId = c.getSnapshot().client().clientId();
    jdbc.update(
        "update crm_client set kyc_review_due = ? where id = ?",
        java.sql.Date.valueOf(BusinessClock.today(clock)),
        clientId);
    boolean tagged =
        tx.execute(
            s -> {
              RenewalCandidate fresh = candidates.findById(c.getId()).orElseThrow();
              engine.run(
                  fresh, com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger.INITIATION);
              return fresh.getFlags().isKycDue();
            });
    assertThat(tagged).isTrue();
    Map<String, Object> counts = as.run(AO, () -> kyc.dashboard(fx.company()));
    assertThat(((Number) counts.get("due")).intValue()).isPositive();
    assertThat(as.run(AO, () -> kyc.accounts(fx.company(), KycMonitoring.DUE)))
        .anyMatch(r -> c.getRenewalRef().equals(r.get("renewalRef")));
  }

  @Test
  void aRiskCodeIsUniqueAndCannotBeRetroactive() {
    LocalDate today = BusinessClock.today(clock);
    String code = "RC" + System.nanoTime() % 100000;
    assertThatThrownBy(
            () ->
                as.run(
                    ADMIN,
                    () ->
                        riskCodes.create(
                            fx.company(),
                            new RiskCodeMaintenance.Entry(
                                code, "Old vessels", null, false, today.minusDays(1), null))))
        .hasMessageContaining("retroactive");
    NonRenewableRiskCode saved =
        as.run(
            ADMIN,
            () ->
                riskCodes.create(
                    fx.company(),
                    new RiskCodeMaintenance.Entry(code, "Old vessels", null, false, today, null)));
    assertThat(saved.isRenewable()).isFalse();
    assertThatThrownBy(
            () ->
                as.run(
                    ADMIN,
                    () ->
                        riskCodes.create(
                            fx.company(),
                            new RiskCodeMaintenance.Entry(code, "Again", null, true, today, null))))
        .hasMessage(RiskCodeMaintenance.DUPLICATE);
    NonRenewableRiskCode updated =
        as.run(
            ADMIN,
            () ->
                riskCodes.update(
                    fx.company(),
                    saved.getId(),
                    new RiskCodeMaintenance.Entry(
                        code, "Old vessels", null, true, today, "Reviewed")));
    assertThat(updated.isRenewable()).isTrue();
  }
}
