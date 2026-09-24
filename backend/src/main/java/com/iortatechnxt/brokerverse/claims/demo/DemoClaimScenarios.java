package com.iortatechnxt.brokerverse.claims.demo;

import com.iortatechnxt.brokerverse.claims.api.dto.ClaimPartyRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.brokerverse.claims.demo.DemoClaimPlan.PlannedClaim;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.brokerverse.claims.domain.ClaimStatus;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.DocumentStatus;
import com.iortatechnxt.brokerverse.claims.domain.EstimateLine;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.LpoCover;
import com.iortatechnxt.brokerverse.claims.domain.Recovery;
import com.iortatechnxt.brokerverse.claims.domain.RecoveryType;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChange;
import com.iortatechnxt.brokerverse.claims.domain.Settlement;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.brokerverse.claims.service.ClaimService;
import com.iortatechnxt.brokerverse.claims.service.LpoCommand;
import com.iortatechnxt.brokerverse.claims.service.LpoService;
import com.iortatechnxt.brokerverse.claims.service.RecoveryCommand;
import com.iortatechnxt.brokerverse.claims.service.RecoveryService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementCommand;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.underwriting.demo.DemoUserContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs one planned demo claim through its lifecycle with the claims services: the claims officer
 * "claims" enters documents, the finance manager "fmanager" approves them and takes the lifecycle
 * decisions. Every step is dated after the notification and never later than {@link #LAST_DATE}.
 */
final class DemoClaimScenarios {

  static final String MAKER = "claims";
  static final String CHECKER = "fmanager";

  /** Last accounting date used by the demo. */
  static final LocalDate LAST_DATE = LocalDate.of(2026, 9, 20);

  private static final String MOTOR = "MOTOR";
  private static final String GARAGE_HO = "G-0001";
  private static final String GARAGE_CEB = "G-0002";
  private static final String SURVEYOR = "SV-0001";
  private static final String THIRD_PARTY = "TP-0001";
  private static final String SALVAGE_BUYER = "S-0010";
  private static final int APPROVE_AFTER = 2;
  private static final int CHANGE_AFTER = 15;
  private static final int SETTLE_AFTER = 25;
  private static final int FINAL_AFTER = 40;
  private static final int RECOVER_AFTER = 50;
  private static final int REOPEN_AFTER = 55;
  private static final BigDecimal EXPENSE_SHARE = new BigDecimal("0.10");
  private static final BigDecimal INCREASE = new BigDecimal("1.20");
  private static final BigDecimal REDUCTION = new BigDecimal("0.70");
  private static final BigDecimal PARTIAL_SHARE = new BigDecimal("0.40");
  private static final BigDecimal DEDUCTIBLE_SHARE = new BigDecimal("0.05");
  private static final BigDecimal SALVAGE_ESTIMATE = new BigDecimal("0.15");
  private static final BigDecimal SALVAGE_RECEIVED = new BigDecimal("0.12");
  private static final BigDecimal REOPEN_SHARE = new BigDecimal("0.20");
  private static final BigDecimal LPO_DISCOUNT = new BigDecimal("0.05");

  private final DemoUserContext users;
  private final ClaimService claims;
  private final ReserveService reserves;
  private final SettlementService settlements;
  private final RecoveryService recoveries;
  private final LpoService lpos;
  private final ClaimLifecycleService lifecycle;
  private final Map<DemoClaimPlan.Scenario, Consumer<Steps>> scenarios =
      new EnumMap<>(DemoClaimPlan.Scenario.class);

  DemoClaimScenarios(
      DemoUserContext users,
      ClaimService claims,
      ReserveService reserves,
      SettlementService settlements,
      RecoveryService recoveries,
      LpoService lpos,
      ClaimLifecycleService lifecycle) {
    this.users = users;
    this.claims = claims;
    this.reserves = reserves;
    this.settlements = settlements;
    this.recoveries = recoveries;
    this.lpos = lpos;
    this.lifecycle = lifecycle;
    scenarios.put(DemoClaimPlan.Scenario.OPEN, s -> s.approvePending(APPROVE_AFTER));
    scenarios.put(DemoClaimPlan.Scenario.PARTIAL, this::partial);
    scenarios.put(DemoClaimPlan.Scenario.FINAL, s -> finalSettlement(s, s.pc.lossReserve()));
    scenarios.put(DemoClaimPlan.Scenario.REDUCED_FINAL, this::reducedFinal);
    scenarios.put(DemoClaimPlan.Scenario.REPUDIATED, this::repudiate);
    scenarios.put(DemoClaimPlan.Scenario.PENDING_INCREASE, this::pendingIncrease);
    scenarios.put(DemoClaimPlan.Scenario.REOPENED, this::reopened);
    scenarios.put(DemoClaimPlan.Scenario.WITHDRAWN, this::withdraw);
    scenarios.put(DemoClaimPlan.Scenario.PENDING_SETTLEMENT, this::pendingSettlement);
    scenarios.put(DemoClaimPlan.Scenario.SALVAGE, this::salvage);
  }

  /**
   * Creates one claim and runs its scenario.
   *
   * @param companyId company
   * @param pc planned claim
   * @return claim after its last step
   */
  Claim run(Long companyId, PlannedClaim pc) {
    boolean withdrawn = pc.scenario() == DemoClaimPlan.Scenario.WITHDRAWN;
    Claim claim = users.runAs(MAKER, () -> claims.register(request(companyId, pc, !withdrawn)));
    Steps s = new Steps(claim.getId(), pc);
    if (s.motor() && !withdrawn) {
      s.lpo(scale(pc.lossReserve(), PARTIAL_SHARE));
    }
    scenarios.get(pc.scenario()).accept(s);
    return users.runAs(CHECKER, () -> claims.get(claim.getId()));
  }

  private static ClaimRequest request(Long companyId, PlannedClaim pc, boolean reserve) {
    boolean motor = MOTOR.equals(pc.policy().businessLine());
    List<ClaimPartyRequest> parties =
        motor
            ? List.of(new ClaimPartyRequest(ClaimPartyRole.THIRD_PARTY, THIRD_PARTY))
            : List.of(new ClaimPartyRequest(ClaimPartyRole.SURVEYOR, SURVEYOR));
    DemoLoss loss = DemoLoss.of(pc.policy().businessLine());
    return new ClaimRequest(
        companyId,
        pc.policy().policyNo(),
        1,
        pc.lossDate(),
        pc.reportedDate(),
        loss.nature(),
        loss.cause(),
        loss.location(),
        loss.description() + " (" + pc.policy().insuredName() + ")",
        pc.policy().currency(),
        null,
        parties,
        reserve ? pc.lossReserve() : null,
        reserve ? expense(pc.lossReserve()) : null);
  }

  private void partial(Steps s) {
    s.approvePending(APPROVE_AFTER);
    BigDecimal increased = scale(s.pc.lossReserve(), INCREASE);
    s.reserve(CostType.LOSS, increased, "Surveyor report: repair cost higher", CHANGE_AFTER);
    BigDecimal amount = scale(increased, PARTIAL_SHARE);
    String payee = s.motor() ? s.garage() : s.pc.policy().customerCode();
    s.settle(payee, CostType.LOSS, SettlementType.PARTIAL, amount, BigDecimal.ZERO, SETTLE_AFTER);
  }

  private void finalSettlement(Steps s, BigDecimal loss) {
    s.approvePending(APPROVE_AFTER);
    s.settle(
        SURVEYOR,
        CostType.EXPENSE,
        SettlementType.PARTIAL,
        expense(s.pc.lossReserve()),
        BigDecimal.ZERO,
        SETTLE_AFTER);
    String payee = s.motor() ? s.garage() : s.pc.policy().customerCode();
    s.settle(
        payee,
        CostType.LOSS,
        SettlementType.FINAL,
        loss,
        scale(loss, DEDUCTIBLE_SHARE),
        FINAL_AFTER);
  }

  private void reducedFinal(Steps s) {
    s.approvePending(APPROVE_AFTER);
    BigDecimal reduced = scale(s.pc.lossReserve(), REDUCTION);
    s.reserve(CostType.LOSS, reduced, "Loss adjusted downwards after inspection", CHANGE_AFTER);
    s.settle(
        s.pc.policy().customerCode(),
        CostType.LOSS,
        SettlementType.FINAL,
        reduced,
        BigDecimal.ZERO,
        FINAL_AFTER);
  }

  private void repudiate(Steps s) {
    s.approvePending(APPROVE_AFTER);
    users.runAs(
        CHECKER,
        () ->
            lifecycle.decline(
                s.claimId,
                ClaimStatus.REJECTED,
                "Loss caused by an excluded peril (wear and tear)",
                s.date(CHANGE_AFTER)));
  }

  private void pendingIncrease(Steps s) {
    s.approvePending(APPROVE_AFTER);
    users.runAs(
        MAKER,
        () ->
            reserves.request(
                s.claimId,
                new EstimateLine(
                    EstimateSide.PAYMENT, CostType.LOSS, scale(s.pc.lossReserve(), INCREASE)),
                "Additional damage found"));
  }

  private void reopened(Steps s) {
    finalSettlement(s, s.pc.lossReserve());
    users.runAs(
        CHECKER, () -> lifecycle.reopen(s.claimId, "Supplementary claim for hidden damage"));
    s.reserve(
        CostType.LOSS,
        s.pc.lossReserve().add(scale(s.pc.lossReserve(), REOPEN_SHARE)),
        "Reserve for the supplementary claim",
        REOPEN_AFTER);
  }

  private void withdraw(Steps s) {
    users.runAs(
        CHECKER,
        () ->
            lifecycle.decline(
                s.claimId,
                ClaimStatus.WITHDRAWN,
                "Claimant settled with the third party directly",
                s.date(APPROVE_AFTER)));
  }

  private void pendingSettlement(Steps s) {
    s.approvePending(APPROVE_AFTER);
    BigDecimal amount = scale(s.pc.lossReserve(), PARTIAL_SHARE);
    String payee = s.pc.policy().customerCode();
    s.settle(payee, CostType.LOSS, SettlementType.PARTIAL, amount, BigDecimal.ZERO, SETTLE_AFTER);
    users.runAs(
        MAKER,
        () ->
            settlements.create(
                s.claimId,
                new SettlementCommand(
                    payee,
                    CostType.LOSS,
                    SettlementType.PARTIAL,
                    scale(s.pc.lossReserve(), PARTIAL_SHARE),
                    null,
                    null,
                    "Second interim payment")));
  }

  private void salvage(Steps s) {
    s.approvePending(APPROVE_AFTER);
    BigDecimal loss = s.pc.lossReserve();
    users.runAs(
        MAKER,
        () ->
            reserves.request(
                s.claimId,
                new EstimateLine(
                    EstimateSide.RECOVERY, CostType.LOSS, scale(loss, SALVAGE_ESTIMATE)),
                "Salvage expected from the damaged stock"));
    s.approvePending(CHANGE_AFTER);
    s.settle(
        s.pc.policy().customerCode(),
        CostType.LOSS,
        SettlementType.FINAL,
        loss,
        BigDecimal.ZERO,
        FINAL_AFTER);
    String bank = "USD".equals(s.pc.policy().currency()) ? "1113" : "1111";
    Recovery recovery =
        users.runAs(
            MAKER,
            () ->
                recoveries.create(
                    s.claimId,
                    new RecoveryCommand(
                        RecoveryType.SALVAGE,
                        SALVAGE_BUYER,
                        bank,
                        scale(loss, SALVAGE_RECEIVED),
                        "Sale of salvaged stock")));
    users.runAs(CHECKER, () -> recoveries.approve(recovery.getId(), s.date(RECOVER_AFTER)));
  }

  private static BigDecimal expense(BigDecimal loss) {
    return scale(loss, EXPENSE_SHARE);
  }

  private static BigDecimal scale(BigDecimal amount, BigDecimal factor) {
    return amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN);
  }

  /** Steps of one claim: maker entries approved by the checker at dated offsets. */
  private final class Steps {

    private final Long claimId;
    private final PlannedClaim pc;

    Steps(Long claimId, PlannedClaim pc) {
      this.claimId = claimId;
      this.pc = pc;
    }

    boolean motor() {
      return MOTOR.equals(pc.policy().businessLine());
    }

    LocalDate date(int daysAfterReport) {
      LocalDate date = pc.reportedDate().plusDays(daysAfterReport);
      return date.isAfter(LAST_DATE) ? LAST_DATE : date;
    }

    void approvePending(int daysAfterReport) {
      for (ReserveChange rc : reserves.forClaim(claimId)) {
        if (rc.getApproval().getStatus() == DocumentStatus.PENDING_APPROVAL) {
          users.runAs(CHECKER, () -> reserves.approve(rc.getId(), date(daysAfterReport)));
        }
      }
    }

    void reserve(CostType cost, BigDecimal amount, String reason, int daysAfterReport) {
      ReserveChange rc =
          users.runAs(
              MAKER,
              () ->
                  reserves.request(
                      claimId, new EstimateLine(EstimateSide.PAYMENT, cost, amount), reason));
      users.runAs(CHECKER, () -> reserves.approve(rc.getId(), date(daysAfterReport)));
    }

    void settle(
        String payee,
        CostType cost,
        SettlementType type,
        BigDecimal assessed,
        BigDecimal deductible,
        int daysAfterReport) {
      Settlement s =
          users.runAs(
              MAKER,
              () ->
                  settlements.create(
                      claimId,
                      new SettlementCommand(
                          payee,
                          cost,
                          type,
                          assessed.add(deductible),
                          deductible,
                          null,
                          type + " settlement of " + cost.name().toLowerCase(Locale.ROOT))));
      users.runAs(CHECKER, () -> settlements.approve(s.getId(), date(daysAfterReport)));
    }

    /** Garage repairing the vehicle of a motor claim. */
    String garage() {
      return pc.policy().branchId() % 2 == 0 ? GARAGE_CEB : GARAGE_HO;
    }

    /** Issues an LPO for the motor repair to the claim's garage. */
    void lpo(BigDecimal net) {
      String garage = garage();
      BigDecimal discount = scale(net, LPO_DISCOUNT);
      users.runAs(
          MAKER,
          () ->
              lpos.issue(
                  claimId,
                  new LpoCommand(
                      garage,
                      LpoCover.OD,
                      date(APPROVE_AFTER),
                      net.add(discount),
                      discount,
                      "Body and paint repair, parts replacement")));
    }
  }
}
