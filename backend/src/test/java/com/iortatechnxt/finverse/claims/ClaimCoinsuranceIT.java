package com.iortatechnxt.finverse.claims;

import static com.iortatechnxt.finverse.claims.ClaimFixtures.CHECKER;
import static com.iortatechnxt.finverse.claims.ClaimFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateLine;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.Lpo;
import com.iortatechnxt.finverse.claims.domain.LpoCover;
import com.iortatechnxt.finverse.claims.domain.LpoStatus;
import com.iortatechnxt.finverse.claims.domain.MovementLine;
import com.iortatechnxt.finverse.claims.domain.Recovery;
import com.iortatechnxt.finverse.claims.domain.RecoveryType;
import com.iortatechnxt.finverse.claims.domain.Settlement;
import com.iortatechnxt.finverse.claims.domain.SettlementType;
import com.iortatechnxt.finverse.claims.service.ClaimPostingService;
import com.iortatechnxt.finverse.claims.service.ClaimQueryService;
import com.iortatechnxt.finverse.claims.service.ClaimService;
import com.iortatechnxt.finverse.claims.service.LpoCommand;
import com.iortatechnxt.finverse.claims.service.LpoService;
import com.iortatechnxt.finverse.claims.service.RecoveryCommand;
import com.iortatechnxt.finverse.claims.service.RecoveryService;
import com.iortatechnxt.finverse.claims.service.ReserveService;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Coinsurance, foreign currency, salvage recoveries and motor LPOs. */
@IntegrationTest
class ClaimCoinsuranceIT {

  @Autowired private ClaimFixtures fx;
  @Autowired private ClaimService claims;
  @Autowired private ReserveService reserves;
  @Autowired private RecoveryService recoveries;
  @Autowired private LpoService lpos;
  @Autowired private ClaimQueryService query;
  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private AsUser as;

  private List<OpenItem> items(String partyCode, String documentNo) {
    Long partyId = parties.getByCode(fx.companyId(), partyCode).getId();
    return openItems.partyItems(fx.companyId(), partyId).stream()
        .filter(i -> documentNo.equals(i.getDocumentNo()))
        .toList();
  }

  private Recovery recover(Long claimId, String amount) {
    as.run(
        MAKER,
        () ->
            reserves.request(
                claimId,
                new EstimateLine(EstimateSide.RECOVERY, CostType.LOSS, new BigDecimal("50000")),
                "Salvage expected"));
    fx.approvePending(claimId);
    Recovery r =
        as.run(
            MAKER,
            () ->
                recoveries.create(
                    claimId,
                    new RecoveryCommand(
                        RecoveryType.SALVAGE,
                        "S-0010",
                        "1111",
                        new BigDecimal(amount),
                        "Salvage sold")));
    return as.run(
        CHECKER, () -> recoveries.approve(r.getId(), ClaimFixtures.APPROVED.plusDays(20)));
  }

  @Test
  void leaderPaysAndCollectsOneHundredPercentAndBooksTheCoinsurerShare() {
    Policy policy = fx.coinsuredPolicy(true);
    Claim claim = fx.openClaim(policy, "150000", null);
    assertThat(claim.ourEstimate()).isEqualByComparingTo("90000.00");

    Settlement s =
        fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "100000", null);
    assertThat(s.getOurAmount()).isEqualByComparingTo("60000.00");
    assertThat(s.getPayableAmount()).isEqualByComparingTo("100000.00");
    assertThat(s.getCoinsurerAmount()).isEqualByComparingTo("40000.00");
    assertThat(fx.posted(s.getJournalBatchNo(), "5100")).isEqualByComparingTo("60000.00");
    assertThat(fx.posted(s.getCoinsuranceBatchNo(), "1206")).isEqualByComparingTo("40000.00");
    assertThat(fx.posted(s.getCoinsuranceBatchNo(), "2204")).isEqualByComparingTo("-40000.00");
    assertThat(items("C-000201", s.getSettlementNo()))
        .singleElement()
        .satisfies(i -> assertThat(i.getAmount()).isEqualByComparingTo("100000.00"));
    assertThat(items("CO-0001", s.getSettlementNo()))
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.DEBIT);
              assertThat(i.getDocumentType())
                  .isEqualTo(ClaimPostingService.COINSURER_SHARE_DOCUMENT);
              assertThat(i.getAmount()).isEqualByComparingTo("40000.00");
            });

    Recovery r = recover(claim.getId(), "10000");
    assertThat(r.getOurAmount()).isEqualByComparingTo("6000.00");
    assertThat(fx.posted(r.getJournalBatchNo(), "1111")).isEqualByComparingTo("6000.00");
    assertThat(fx.posted(r.getJournalBatchNo(), "5100")).isEqualByComparingTo("-6000.00");
    assertThat(fx.posted(r.getCoinsuranceBatchNo(), "1111")).isEqualByComparingTo("4000.00");
    assertThat(items("CO-0001", r.getRecoveryNo()))
        .singleElement()
        .satisfies(i -> assertThat(i.getDirection()).isEqualTo(ItemDirection.CREDIT));
    assertThat(as.run(CHECKER, () -> claims.get(claim.getId())).ourRecovered())
        .isEqualByComparingTo("6000.00");
  }

  @Test
  void followerPaysOnlyItsShare() {
    Policy policy = fx.coinsuredPolicy(false);
    Claim claim = fx.openClaim(policy, "100000", null);
    Settlement s =
        fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.FINAL, "33333.33", null);
    assertThat(s.getOurAmount()).isEqualByComparingTo("20000.00");
    assertThat(s.getPayableAmount()).isEqualByComparingTo("20000.00");
    assertThat(s.getCoinsurerAmount()).isEqualByComparingTo("0");
    assertThat(s.getCoinsuranceBatchNo()).isNull();
    assertThat(items("CO-0001", s.getSettlementNo())).isEmpty();
    BigDecimal lines =
        query.movements(claim.getId()).stream()
            .filter(l -> l.getSide() == EstimateSide.PAYMENT)
            .map(MovementLine::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(lines).isEqualByComparingTo("40000.00");
  }

  @Test
  void foreignCurrencyClaimCarriesBaseAmounts() {
    Policy policy = fx.usdPolicy();
    Claim claim = fx.openClaim(policy, "10000", null);
    MovementLine line = query.movements(claim.getId()).get(0);
    assertThat(line.getCurrency()).isEqualTo("USD");
    assertThat(line.getAmount()).isEqualByComparingTo("10000.00");
    assertThat(line.getBaseAmount()).isGreaterThan(new BigDecimal("100000"));
    Settlement s =
        fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "2000", null);
    assertThat(s.getExchangeRate()).isGreaterThan(BigDecimal.ONE);
    assertThatThrownBy(() -> recover(claim.getId(), "999999"))
        .hasMessageContaining("exceeds the outstanding recovery");
  }

  @Test
  void motorRepairsUseLposForGarages() {
    Policy motor = fx.policy("MOTOR");
    Claim claim = fx.openClaim(motor, "80000", null);
    Lpo lpo =
        as.run(
            MAKER,
            () ->
                lpos.issue(
                    claim.getId(),
                    new LpoCommand(
                        "G-0001",
                        LpoCover.OD,
                        LocalDate.of(2026, 4, 10),
                        new BigDecimal("52000"),
                        new BigDecimal("2000"),
                        "Bumper and door repair")));
    assertThat(lpo.getLpoNo()).startsWith("LPO-HO-2026-");
    assertThat(lpo.getNetAmount()).isEqualByComparingTo("50000.00");
    assertThat(as.run(CHECKER, () -> claims.get(claim.getId())).getParties())
        .anyMatch(p -> p.getRole() == ClaimPartyRole.GARAGE);
    assertThat(lpos.forClaim(claim.getId())).hasSize(1);
    assertThat(lpos.forCompany(fx.companyId())).anyMatch(l -> l.getId().equals(lpo.getId()));

    Settlement garage =
        fx.settle(claim.getId(), "G-0001", CostType.LOSS, SettlementType.PARTIAL, "50000", null);
    assertThat(items("G-0001", garage.getSettlementNo())).hasSize(1);

    Lpo cancelled = as.run(MAKER, () -> lpos.cancel(lpo.getId(), "Wrong garage"));
    assertThat(cancelled.getStatus()).isEqualTo(LpoStatus.CANCELLED);
    assertThatThrownBy(() -> as.run(MAKER, () -> lpos.cancel(lpo.getId(), "again")))
        .hasMessageContaining("CANCELLED");

    Claim fire = fx.openClaim(fx.policy("FIRE"), "10000", null);
    LpoCommand command =
        new LpoCommand("G-0001", LpoCover.TP, null, new BigDecimal("100"), null, "Repair");
    assertThatThrownBy(() -> as.run(MAKER, () -> lpos.issue(fire.getId(), command)))
        .hasMessageContaining("motor claims only");
  }
}
