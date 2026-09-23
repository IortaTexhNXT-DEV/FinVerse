package com.iortatechnxt.finverse.reinsurance;

import static com.iortatechnxt.finverse.reinsurance.RiFixtures.CHECKER;
import static com.iortatechnxt.finverse.reinsurance.RiFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementListener;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.reinsurance.api.dto.SettlementRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.SoaRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.finverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.finverse.reinsurance.domain.Soa;
import com.iortatechnxt.finverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.finverse.reinsurance.domain.SoaStatus;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.service.CessionService;
import com.iortatechnxt.finverse.reinsurance.service.FacPlacementService;
import com.iortatechnxt.finverse.reinsurance.service.ReinsuranceApprovalSource;
import com.iortatechnxt.finverse.reinsurance.service.SoaLayout;
import com.iortatechnxt.finverse.reinsurance.service.SoaService;
import com.iortatechnxt.finverse.reinsurance.service.SoaSettlement;
import com.iortatechnxt.finverse.reinsurance.service.TreatyService;
import com.iortatechnxt.finverse.reinsurance.service.UnplacedFacAlertCheck;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Statements of account: quarterly figures, maker-checker approval with the statement adjustments,
 * settlement matching the reinsurer's open items; the approval inbox and the unplaced facultative
 * alert.
 */
@IntegrationTest
class StatementOfAccountIT {

  private static final int YEAR = 2034;

  @Autowired private RiFixtures fx;
  @Autowired private CessionService cessions;
  @Autowired private SoaService statements;
  @Autowired private SoaSettlement settlement;
  @Autowired private TreatyService treaties;
  @Autowired private FacPlacementService placements;
  @Autowired private ClaimMovementListener listener;
  @Autowired private ReinsuranceApprovalSource inbox;
  @Autowired private UnplacedFacAlertCheck unplaced;
  @Autowired private TestData data;
  @Autowired private AsUser as;

  @Test
  void quarterlyStatementIsApprovedAndSettledAgainstOpenItems() {
    RiFixtures.Reinsurers ri = fx.reinsurers();
    Treaty qs = fx.authorized(fx.quotaShare(ri, "TQS34", "FIRE", YEAR, "40", "50000000"));
    Policy policy =
        fx.policy(fx.product("FIRE"), YEAR, "PHP", List.of(fx.risk("120000000", "240000")));
    as.run(MAKER, () -> cessions.cedePolicy(policy.getId()));
    listener.onClaimMovement(claim(policy, ClaimMovementType.RESERVE_CHANGE, "300000"));
    listener.onClaimMovement(claim(policy, ClaimMovementType.PAYMENT, "120000"));

    SoaRequest q1 = new SoaRequest(fx.companyId(), "TQS34", ri.follow(), 2026, 1, null);
    Soa soa = as.run(MAKER, () -> statements.generate(q1)).get(0);
    SoaFigures f = soa.figures();
    assertThat(soa.getStatementDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    assertThat(f.premium()).isEqualByComparingTo("16000.00");
    assertThat(f.commission()).isEqualByComparingTo("4000.00");
    assertThat(f.levy()).isEqualByComparingTo("160.00");
    assertThat(f.premiumReserveRetained()).isEqualByComparingTo("3200.00");
    assertThat(f.lossesPaid()).isZero();
    assertThat(soa.getBalance()).isEqualByComparingTo("8640.00");

    Soa again = as.run(MAKER, () -> statements.generate(q1)).get(0);
    assertThat(again.getId()).isEqualTo(soa.getId());
    assertThatThrownBy(() -> as.run(MAKER, () -> statements.approve(soa.getId())))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(pendingReferences()).contains(soa.getSoaNo());

    Soa approved = as.run(CHECKER, () -> statements.approve(soa.getId()));
    assertThat(approved.getStatus()).isEqualTo(SoaStatus.APPROVED);
    assertThat(fx.posted("2507", "RI:SOA:" + soa.getSoaNo())).isEqualByComparingTo("-160.00");
    assertThat(fx.posted("2202", "RI:SOA:" + soa.getSoaNo())).isEqualByComparingTo("-3200.00");
    assertThat(settlement.outstandingItems(approved)).hasSize(2);
    assertThatThrownBy(() -> as.run(MAKER, () -> statements.generate(q1)))
        .hasMessageContaining("already");

    Soa settled =
        as.run(
            CHECKER,
            () ->
                statements.settle(
                    soa.getId(), new SettlementRequest(LocalDate.of(2026, 4, 20), "1111")));
    assertThat(settled.getStatus()).isEqualTo(SoaStatus.SETTLED);
    assertThat(settlement.outstandingItems(settled)).isEmpty();
    assertThat(fx.posted("2201", "RI:SOA:" + soa.getSoaNo() + ":SETTLE"))
        .isEqualByComparingTo("8640.00");
    assertThatThrownBy(
            () ->
                as.run(
                    CHECKER,
                    () ->
                        statements.settle(
                            soa.getId(), new SettlementRequest(LocalDate.of(2026, 4, 20), "1111"))))
        .isInstanceOf(BusinessRuleException.class);

    secondQuarter(ri, qs);
  }

  private void secondQuarter(RiFixtures.Reinsurers ri, Treaty qs) {
    List<Soa> q2 =
        as.run(
            MAKER,
            () ->
                statements.generate(
                    new SoaRequest(
                        fx.companyId(), qs.getCode(), null, 2026, 2, LocalDate.of(2026, 7, 10))));
    assertThat(q2).hasSize(2);
    Soa r3 =
        q2.stream()
            .filter(s -> ri.follow().equals(s.getParty().getCode()))
            .findFirst()
            .orElseThrow();
    SoaFigures f = r3.figures();
    assertThat(f.premium()).isZero();
    assertThat(f.lossesPaid()).isEqualByComparingTo("8000.00");
    assertThat(f.interest()).isEqualByComparingTo("32.00");
    assertThat(f.lossReserveRetained()).isEqualByComparingTo("10000.00");
    SoaLayout layout = statements.layout(r3);
    assertThat(layout.balanceOnIncome()).isTrue();
    assertThat(layout.balanceLabel()).isEqualTo("Balance due from reinsurer");
    assertThat(layout.total())
        .isEqualByComparingTo(layout.incomeSubtotal().max(layout.outgoSubtotal()));
    assertThat(statements.list(fx.companyId())).extracting(Soa::getSoaNo).contains(r3.getSoaNo());
    Soa approved = as.run(CHECKER, () -> statements.approve(r3.getId()));
    Soa settled =
        as.run(
            CHECKER,
            () ->
                statements.settle(
                    approved.getId(), new SettlementRequest(LocalDate.of(2026, 7, 20), "1111")));
    assertThat(settlement.outstandingItems(settled)).isEmpty();
  }

  private ClaimMovement claim(Policy policy, ClaimMovementType type, String amount) {
    BigDecimal value = new BigDecimal(amount);
    return new ClaimMovement(
        fx.companyId(),
        data.branch("HO").getId(),
        9_340_001L,
        "TCL-9340001",
        policy.getId(),
        "FIRE",
        LocalDate.of(YEAR, 3, 1),
        LocalDate.of(2026, 5, 10),
        type,
        "PHP",
        value,
        value,
        "TEST:SOA:" + type);
  }

  private Set<String> pendingReferences() {
    List<PendingApproval> items =
        inbox.pendingFor(ApprovalViewer.user(CHECKER, Set.of("REINSURANCE_AUTHORIZE")));
    return Set.copyOf(items.stream().map(PendingApproval::reference).toList());
  }

  @Test
  void pendingTreatiesAndSlipsReachTheInboxAndOldSlipsRaiseAnAlert() {
    TreatyRequest request = fx.surplus(fx.reinsurers(), "TSP35", "ENGG", 2035, "10000000", 2);
    Treaty draft = as.run(MAKER, () -> treaties.create(request));
    assertThat(pendingReferences()).contains(draft.getCode());
    assertThat(inbox.pendingFor(ApprovalViewer.user(MAKER, Set.of("REINSURANCE_AUTHORIZE"))))
        .extracting(PendingApproval::reference)
        .doesNotContain(draft.getCode());
    assertThat(inbox.pendingFor(ApprovalViewer.user(CHECKER, Set.of()))).isEmpty();
    as.run(CHECKER, () -> treaties.authorize(draft.getId()));
    assertThatThrownBy(
            () -> fx.authorized(fx.surplus(fx.reinsurers(), "TSP35B", "ENGG", 2035, "10000000", 2)))
        .hasMessageContaining("already exists");

    Policy engineering =
        fx.policy(fx.product("ENGG"), 2035, "PHP", List.of(fx.risk("50000000", "100000")));
    as.run(MAKER, () -> cessions.cedePolicy(engineering.getId()));
    assertThat(placements.list(fx.companyId(), FacStatus.PROVISIONAL)).isNotEmpty();
    assertThat(unplaced.evaluate(LocalDate.of(2026, 12, 31)))
        .anyMatch(s -> s.facts().message().contains(engineering.getPolicyNo()));
    assertThat(ApprovalViewer.system().can("X")).isTrue();
  }
}
