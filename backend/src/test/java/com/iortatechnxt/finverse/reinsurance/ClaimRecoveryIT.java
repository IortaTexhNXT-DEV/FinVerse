package com.iortatechnxt.finverse.reinsurance;

import static com.iortatechnxt.finverse.reinsurance.RiFixtures.CHECKER;
import static com.iortatechnxt.finverse.reinsurance.RiFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementListener;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.insurance.ClaimReinsuranceView;
import com.iortatechnxt.finverse.reinsurance.api.dto.FacAssignRequest;
import com.iortatechnxt.finverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.reinsurance.service.CessionService;
import com.iortatechnxt.finverse.reinsurance.service.ClaimRecoveryService;
import com.iortatechnxt.finverse.reinsurance.service.FacPlacementService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The claim movement listener, driven with kernel records only: reinsurers' share of reserves,
 * recoveries on payments with reinsurer open items, salvage, idempotency on the reference, the
 * reserve view and the excess of loss recovery with its limit and aggregate.
 */
@IntegrationTest
class ClaimRecoveryIT {

  private static final AtomicLong CLAIM_IDS = new AtomicLong(9_300_000L);
  private static final LocalDate MOVED = LocalDate.of(2026, 5, 5);

  @Autowired private RiFixtures fx;
  @Autowired private ClaimMovementListener listener;
  @Autowired private ClaimReinsuranceView reserveView;
  @Autowired private ClaimRecoveryService recoveries;
  @Autowired private CessionService cessions;
  @Autowired private FacPlacementService placements;
  @Autowired private OpenItemService openItems;
  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private ClaimMovement movement(
      Policy policy, long claimId, String lob, int lossYear, ClaimMovementType type, String amount) {
    BigDecimal value = new BigDecimal(amount);
    return new ClaimMovement(
        fx.companyId(),
        data.branch("HO").getId(),
        claimId,
        "TCL-" + claimId,
        policy.getId(),
        lob,
        LocalDate.of(lossYear, 5, 1),
        MOVED,
        type,
        "PHP",
        value,
        value,
        "TEST:" + claimId + ":" + type + ":" + CLAIM_IDS.incrementAndGet());
  }

  private RiClaimMovement processed(ClaimMovement m) {
    return recoveries.movements(fx.companyId(), MOVED, MOVED).stream()
        .filter(r -> r.getReference().equals(m.reference()))
        .findFirst()
        .orElseThrow();
  }

  private static BigDecimal layer(RiClaimMovement m, RiLayer layer) {
    return m.getShares().stream()
        .filter(s -> s.getLayer() == layer)
        .map(s -> s.getBaseAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void proportionalSharesOfReservesPaymentsAndSalvage() {
    int year = 2033;
    fx.authorized(fx.quotaShare("TQS33", "FIRE", year, "40", null));
    fx.authorized(fx.surplus("TSP33", "FIRE", year, "25000000", 3));
    Policy policy =
        fx.policy(fx.product("FIRE"), year, "PHP", List.of(fx.risk("120000000", "240000")));
    as.run(MAKER, () -> cessions.cedePolicy(policy.getId()));
    placeFacultative(policy);
    long claim = CLAIM_IDS.incrementAndGet();

    ClaimMovement reserve =
        movement(policy, claim, "FIRE", year, ClaimMovementType.RESERVE_CHANGE, "1200000");
    listener.onClaimMovement(reserve);
    listener.onClaimMovement(reserve);
    RiClaimMovement r = processed(reserve);
    assertThat(r.getCessionId()).isNotNull();
    assertThat(layer(r, RiLayer.QUOTA_SHARE)).isEqualByComparingTo("100000");
    assertThat(layer(r, RiLayer.SURPLUS)).isEqualByComparingTo("750000");
    assertThat(layer(r, RiLayer.FAC)).isEqualByComparingTo("140000");
    assertThat(reserveView.reinsuranceShareOfOutstanding(fx.companyId(), MOVED).get(claim))
        .isEqualByComparingTo("990000");
    assertThat(fx.posted("1302", "RI:CLM:" + r.getId())).isEqualByComparingTo("990000.00");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from ri_claim_movement where reference = ?",
                Integer.class,
                reserve.reference()))
        .isEqualTo(1);

    ClaimMovement payment =
        movement(policy, claim, "FIRE", year, ClaimMovementType.PAYMENT, "600000");
    listener.onClaimMovement(payment);
    RiClaimMovement paid = processed(payment);
    assertThat(paid.ceded()).isEqualByComparingTo("495000");
    assertThat(paid.getNetRetained()).isEqualByComparingTo("105000");
    assertThat(fx.posted("1205", "RI:CLM:" + paid.getId())).isEqualByComparingTo("495000.00");
    Long r1 = partyId("R-0001");
    assertThat(
            openItems.partyItems(fx.companyId(), r1).stream()
                .filter(i -> i.getSourceReference().startsWith("RI:CLM:" + paid.getId() + ":"))
                .allMatch(i -> i.getDirection() == ItemDirection.DEBIT))
        .isTrue();

    ClaimMovement salvage =
        movement(policy, claim, "FIRE", year, ClaimMovementType.RECOVERY, "100000");
    listener.onClaimMovement(salvage);
    RiClaimMovement back = processed(salvage);
    assertThat(back.ceded()).isEqualByComparingTo("-82500");
    assertThat(back.getNetRetained()).isEqualByComparingTo("-17500");
    assertThat(
            openItems.partyItems(fx.companyId(), r1).stream()
                .anyMatch(i -> "REINSURANCE_RECOVERY_RETURN".equals(i.getDocumentType())))
        .isTrue();

    listener.onClaimMovement(
        movement(policy, claim, "FIRE", year, ClaimMovementType.RESERVE_CHANGE, "-600000"));
    assertThat(reserveView.reinsuranceShareOfOutstanding(fx.companyId(), MOVED).get(claim))
        .isEqualByComparingTo("495000");
    assertThat(recoveries.catchUp(fx.companyId(), MOVED, MOVED)).isZero();
  }

  private void placeFacultative(Policy policy) {
    Long cessionId = cessions.ofPolicy(policy.getId()).get(0).getId();
    Long id =
        placements.list(fx.companyId(), FacStatus.PROVISIONAL).stream()
            .filter(p -> p.getCession().getId().equals(cessionId))
            .findFirst()
            .orElseThrow()
            .getId();
    as.run(
        MAKER,
        () ->
            placements.assign(
                id,
                new FacAssignRequest(
                    List.of(new FacAssignRequest.Line("R-0004", new BigDecimal("70"), null)),
                    null)));
    as.run(MAKER, () -> placements.submit(id));
    as.run(CHECKER, () -> placements.approve(id, RiFixtures.APPROVAL));
    as.run(MAKER, () -> placements.close(id, RiFixtures.APPROVAL));
  }

  @Test
  void excessOfLossRecoversAbovePriorityWithinLimitAndAggregate() {
    int year = 2032;
    fx.authorized(fx.excessOfLoss("TXL32", "MOTOR", year, "500000", "2000000"));
    Policy motor = fx.policy(fx.product("MOTOR"), year, "PHP", List.of(fx.risk("3000000", "30000")));
    long claim = CLAIM_IDS.incrementAndGet();

    ClaimMovement first = movement(motor, claim, "MOTOR", year, ClaimMovementType.PAYMENT, "900000");
    listener.onClaimMovement(first);
    RiClaimMovement one = processed(first);
    assertThat(one.getCessionId()).isNull();
    assertThat(layer(one, RiLayer.XOL)).isEqualByComparingTo("400000");
    assertThat(one.getShares()).hasSize(2);

    ClaimMovement second =
        movement(motor, claim, "MOTOR", year, ClaimMovementType.PAYMENT, "2000000");
    listener.onClaimMovement(second);
    assertThat(layer(processed(second), RiLayer.XOL)).isEqualByComparingTo("1600000");

    ClaimMovement third = movement(motor, claim, "MOTOR", year, ClaimMovementType.PAYMENT, "100000");
    listener.onClaimMovement(third);
    assertThat(layer(processed(third), RiLayer.XOL)).isZero();

    long other = CLAIM_IDS.incrementAndGet();
    ClaimMovement exhausted =
        movement(motor, other, "MOTOR", year, ClaimMovementType.PAYMENT, "1500000");
    listener.onClaimMovement(exhausted);
    assertThat(layer(processed(exhausted), RiLayer.XOL)).isZero();

    ClaimMovement salvage =
        movement(motor, claim, "MOTOR", year, ClaimMovementType.RECOVERY, "1000000");
    listener.onClaimMovement(salvage);
    assertThat(layer(processed(salvage), RiLayer.XOL)).isEqualByComparingTo("-500000");
  }

  private Long partyId(String code) {
    return jdbc.queryForObject(
        "select id from pty_party where company_id = ? and code = ?",
        Long.class,
        fx.companyId(),
        code);
  }
}
