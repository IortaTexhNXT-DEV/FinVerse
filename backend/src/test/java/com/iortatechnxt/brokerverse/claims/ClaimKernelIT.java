package com.iortatechnxt.brokerverse.claims;

import static com.iortatechnxt.brokerverse.claims.ClaimFixtures.CHECKER;
import static com.iortatechnxt.brokerverse.claims.ClaimFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.EstimateLine;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.Recovery;
import com.iortatechnxt.brokerverse.claims.domain.RecoveryType;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChange;
import com.iortatechnxt.brokerverse.claims.domain.Settlement;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.service.RecoveryCommand;
import com.iortatechnxt.brokerverse.claims.service.RecoveryService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.brokerverse.insurance.OutstandingClaim;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyClaimsView;
import com.iortatechnxt.brokerverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Insurance kernel integration: listener notifications and the two read views. */
@IntegrationTest
class ClaimKernelIT {

  @Autowired private ClaimFixtures fx;
  @Autowired private RecordingClaimListener listener;
  @Autowired private ReserveService reserves;
  @Autowired private SettlementService settlements;
  @Autowired private RecoveryService recoveries;
  @Autowired private ClaimsExperienceView experience;
  @Autowired private PolicyClaimsView policyClaims;
  @Autowired private UnderwritingPorts ports;
  @Autowired private AsUser as;

  @Test
  void everyPostedMovementIsNotifiedOnceWithAStableReference() {
    Policy policy = fx.policy("FIRE");
    Claim claim = fx.openClaim(policy, "100000", "5000");
    Settlement s =
        fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "30000", null);
    as.run(
        MAKER,
        () ->
            reserves.request(
                claim.getId(),
                new EstimateLine(EstimateSide.RECOVERY, CostType.LOSS, new BigDecimal("8000")),
                "Salvage"));
    fx.approvePending(claim.getId());
    Recovery r =
        as.run(
            MAKER,
            () ->
                recoveries.create(
                    claim.getId(),
                    new RecoveryCommand(
                        RecoveryType.SUBROGATION,
                        null,
                        "1111",
                        new BigDecimal("5000"),
                        "TP insurer")));
    as.run(CHECKER, () -> recoveries.approve(r.getId(), ClaimFixtures.APPROVED));

    List<ClaimMovement> received = listener.of(claim.getId());
    assertThat(received)
        .extracting(ClaimMovement::type)
        .containsExactly(
            ClaimMovementType.RESERVE_CHANGE,
            ClaimMovementType.RESERVE_CHANGE,
            ClaimMovementType.PAYMENT,
            ClaimMovementType.RESERVE_CHANGE,
            ClaimMovementType.RECOVERY);
    String stl = "CLAIM:" + claim.getId() + ":STL:" + s.getId();
    assertThat(received)
        .extracting(ClaimMovement::reference)
        .contains(stl, stl + ":RSV", "CLAIM:" + claim.getId() + ":REC:" + r.getId());
    received.forEach(m -> assertThat(listener.deliveries(m.reference())).isEqualTo(1));
    BigDecimal reserve =
        received.stream()
            .filter(m -> m.type() == ClaimMovementType.RESERVE_CHANGE)
            .map(ClaimMovement::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(reserve).isEqualByComparingTo("75000.00");
    assertThat(received.get(0).lineOfBusiness()).isEqualTo("FIRE");
    assertThat(received.get(0).lossDate()).isEqualTo(ClaimFixtures.LOSS);

    assertThatThrownBy(() -> as.run(CHECKER, () -> settlements.approve(s.getId(), null)))
        .hasMessageContaining("APPROVED");
    assertThat(listener.deliveries(stl)).isEqualTo(1);

    List<ClaimMovement> view =
        experience.movements(
            fx.companyId(), ClaimFixtures.APPROVED, ClaimFixtures.APPROVED.plusDays(30));
    assertThat(view.stream().filter(m -> m.claimId().equals(claim.getId())).toList())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrderElementsOf(received);
  }

  @Test
  void outstandingAndPolicyFiguresFollowTheLedger() {
    Policy policy = fx.policy("FIRE");
    Claim claim = fx.openClaim(policy, "60000", "4000");
    ReserveChange up = fx.requestReserve(claim.getId(), CostType.LOSS, "70000");
    as.run(CHECKER, () -> reserves.approve(up.getId(), LocalDate.of(2026, 5, 20)));
    fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "20000", null);

    OutstandingClaim before = outstanding(claim.getId(), LocalDate.of(2026, 5, 1));
    assertThat(before.outstanding()).isEqualByComparingTo("44000.00");
    assertThat(before.baseOutstanding()).isEqualByComparingTo("44000.00");
    assertThat(before.reportedDate()).isEqualTo(ClaimFixtures.REPORTED);
    assertThat(outstanding(claim.getId(), LocalDate.of(2026, 12, 31)).outstanding())
        .isEqualByComparingTo("54000.00");
    assertThat(experience.outstanding(fx.companyId(), LocalDate.of(2026, 4, 1)))
        .noneMatch(o -> o.claimId().equals(claim.getId()));

    Map<Long, ClaimsFigures> figures = policyClaims.figures(List.of(policy.getId()));
    ClaimsFigures f = figures.get(policy.getId());
    assertThat(f.claimCount()).isEqualTo(1);
    assertThat(f.latestClaimNo()).isEqualTo(claim.getClaimNo());
    assertThat(f.latestLossDate()).isEqualTo(ClaimFixtures.LOSS);
    assertThat(f.reserve()).isEqualByComparingTo("74000.00");
    assertThat(f.paid()).isEqualByComparingTo("20000.00");
    assertThat(f.outstanding()).isEqualByComparingTo("54000.00");
    assertThat(f.netClaims()).isEqualByComparingTo("74000.00");
    assertThat(ports.claims(List.of(policy.getId()))).containsKey(policy.getId());
    assertThat(policyClaims.figures(List.of())).isEmpty();
  }

  private OutstandingClaim outstanding(Long claimId, LocalDate asOf) {
    return experience.outstanding(fx.companyId(), asOf).stream()
        .filter(o -> o.claimId().equals(claimId))
        .findFirst()
        .orElseThrow();
  }
}
