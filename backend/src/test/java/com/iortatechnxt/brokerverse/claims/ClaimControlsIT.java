package com.iortatechnxt.brokerverse.claims;

import static com.iortatechnxt.brokerverse.claims.ClaimFixtures.CHECKER;
import static com.iortatechnxt.brokerverse.claims.ClaimFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.alert.service.AlertService.AlertSearch;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimStatus;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.DocumentStatus;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChange;
import com.iortatechnxt.brokerverse.claims.domain.Settlement;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.service.ClaimAlerts;
import com.iortatechnxt.brokerverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.brokerverse.claims.service.ClaimService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/** Maker-checker, authorization limits, approval inbox, alerts and lifecycle rules. */
@IntegrationTest
class ClaimControlsIT {

  private static final Instant FAR_FUTURE = Instant.parse("2100-01-01T00:00:00Z");

  @Autowired private ClaimFixtures fx;
  @Autowired private ClaimService claims;
  @Autowired private ReserveService reserves;
  @Autowired private SettlementService settlements;
  @Autowired private ClaimLifecycleService lifecycle;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private AlertService alerts;
  @Autowired private AsUser as;

  private static List<String> references(List<PendingApproval> items) {
    return items.stream().map(PendingApproval::reference).toList();
  }

  private boolean alerted(String code, Claim claim) {
    return alerts
        .search(
            new AlertSearch(null, null, code, fx.companyId(), Instant.EPOCH, FAR_FUTURE),
            Pageable.unpaged())
        .stream()
        .anyMatch(a -> claim.getClaimNo().equals(a.getEntityId()));
  }

  @Test
  void reserveNeedsAnotherUserWithinTheAuthorizationLimit() {
    Policy policy = fx.policy("FIRE");
    Claim claim = as.run(MAKER, () -> claims.register(fx.request(policy, "6000000", null)));
    ReserveChange rc = reserves.forClaim(claim.getId()).get(0);
    String reference = claim.getClaimNo() + "/" + rc.getChangeNo();

    assertThat(references(as.run(MAKER, () -> inbox.inbox(fx.companyId()))))
        .doesNotContain(reference);
    assertThat(references(as.run("checker", () -> inbox.inbox(fx.companyId()))))
        .doesNotContain(reference);
    assertThat(references(as.run(CHECKER, () -> inbox.inbox(fx.companyId())))).contains(reference);

    assertThatThrownBy(() -> as.run(MAKER, () -> reserves.approve(rc.getId(), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be approved by the user who entered it");
    assertThatThrownBy(() -> as.run("checker", () -> reserves.approve(rc.getId(), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("authorization limit");

    ReserveChange approved =
        as.run(CHECKER, () -> reserves.approve(rc.getId(), ClaimFixtures.APPROVED));
    assertThat(approved.getApproval().getStatus()).isEqualTo(DocumentStatus.APPROVED);
    assertThat(approved.getApproval().getAuthorityAmount()).isEqualByComparingTo("6000000.00");
    assertThat(approved.getOurChange()).isEqualByComparingTo("6000000.00");
    assertThat(alerted(ClaimAlerts.LARGE_RESERVE, claim)).isTrue();

    ReserveChange decrease = fx.requestReserve(claim.getId(), CostType.LOSS, "4000000");
    assertThat(decrease.getApproval().getAuthorityAmount()).isEqualByComparingTo("0");
    ReserveChange approvedDecrease =
        as.run("checker", () -> reserves.approve(decrease.getId(), ClaimFixtures.APPROVED));
    assertThat(approvedDecrease.getChangeAmount()).isEqualByComparingTo("-2000000.00");
  }

  @Test
  void rejectedDocumentsLeaveTheClaimUnchanged() {
    Policy policy = fx.policy("FIRE");
    Claim claim = as.run(MAKER, () -> claims.register(fx.request(policy, "100000", null)));
    ReserveChange rc = reserves.forClaim(claim.getId()).get(0);
    assertThatThrownBy(() -> fx.requestReserve(claim.getId(), CostType.LOSS, "120000"))
        .hasMessageContaining("already pending");

    ReserveChange rejected = as.run(CHECKER, () -> reserves.reject(rc.getId(), "Too high"));
    assertThat(rejected.getApproval().getStatus()).isEqualTo(DocumentStatus.REJECTED);
    assertThat(rejected.getApproval().getRejectionReason()).isEqualTo("Too high");
    assertThat(as.run(CHECKER, () -> claims.get(claim.getId())).getStatus())
        .isEqualTo(ClaimStatus.REGISTERED);
    assertThatThrownBy(() -> as.run(CHECKER, () -> reserves.approve(rc.getId(), null)))
        .hasMessageContaining("REJECTED");

    fx.approvePending(fx.requestReserve(claim.getId(), CostType.LOSS, "80000").getClaim().getId());
    Settlement s =
        fx.enterSettlement(
            claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "5000", null);
    Settlement rejectedSettlement =
        as.run(CHECKER, () -> settlements.reject(s.getId(), "Duplicate"));
    assertThat(rejectedSettlement.getApproval().getStatus()).isEqualTo(DocumentStatus.REJECTED);
    assertThat(as.run(CHECKER, () -> claims.get(claim.getId())).getTotals().getPaidLoss())
        .isEqualByComparingTo("0");
  }

  @Test
  void repudiationWithdrawalAndPendingDocumentGuards() {
    Policy policy = fx.policy("FIRE");
    Claim open = fx.openClaim(policy, "250000", null);
    assertThatThrownBy(
            () ->
                as.run(
                    MAKER, () -> lifecycle.decline(open.getId(), ClaimStatus.REJECTED, "x", null)))
        .hasMessageContaining("registered it");

    fx.requestReserve(open.getId(), CostType.LOSS, "300000");
    assertThatThrownBy(
            () ->
                as.run(
                    CHECKER,
                    () -> lifecycle.decline(open.getId(), ClaimStatus.REJECTED, "x", null)))
        .hasMessageContaining("waiting for approval");
    fx.approvePending(open.getId());
    Claim rejected =
        as.run(
            CHECKER,
            () ->
                lifecycle.decline(
                    open.getId(), ClaimStatus.REJECTED, "Excluded peril", ClaimFixtures.APPROVED));
    assertThat(rejected.getStatus()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(rejected.ourShare().outstanding()).isEqualByComparingTo("0");
    assertThatThrownBy(() -> fx.requestReserve(open.getId(), CostType.LOSS, "1000"))
        .hasMessageContaining("REJECTED");

    Claim registered = as.run(MAKER, () -> claims.register(fx.request(policy, null, null)));
    Claim withdrawn =
        as.run(
            CHECKER,
            () ->
                lifecycle.decline(
                    registered.getId(), ClaimStatus.WITHDRAWN, "Claimant withdrew", null));
    assertThat(withdrawn.getStatus()).isEqualTo(ClaimStatus.WITHDRAWN);

    Claim paid = fx.openClaim(policy, "90000", null);
    fx.settle(paid.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "1000", null);
    assertThatThrownBy(
            () ->
                as.run(
                    CHECKER,
                    () -> lifecycle.decline(paid.getId(), ClaimStatus.WITHDRAWN, "x", null)))
        .hasMessageContaining("Cannot decline");
    assertThatThrownBy(() -> as.run(CHECKER, () -> lifecycle.reopen(paid.getId(), "x")))
        .hasMessageContaining("reopen");
  }

  @Test
  void notificationIsValidatedAndLateReportsRaiseAnAlert() {
    Policy policy = fx.policy("FIRE");
    ClaimRequest base = fx.request(policy, null, null);
    ClaimRequest beforeCover = withDates(base, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2));
    assertThatThrownBy(() -> as.run(MAKER, () -> claims.register(beforeCover)))
        .hasMessageContaining("not approved and in force");
    ClaimRequest usd = withCurrency(base, "USD");
    assertThatThrownBy(() -> as.run(MAKER, () -> claims.register(usd)))
        .hasMessageContaining("policy currency");
    ClaimRequest future = withDates(base, LocalDate.of(2026, 4, 1), LocalDate.of(2099, 1, 1));
    assertThatThrownBy(() -> as.run(MAKER, () -> claims.register(future)))
        .hasMessageContaining("future");

    ClaimRequest late = withDates(base, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 5, 1));
    Claim claim = as.run(MAKER, () -> claims.register(late));
    assertThat(alerted(ClaimAlerts.LATE_NOTIFICATION, claim)).isTrue();

    ReserveChange rc = fx.requestReserve(claim.getId(), CostType.LOSS, "10000");
    assertThatThrownBy(
            () -> as.run(CHECKER, () -> reserves.approve(rc.getId(), ClaimFixtures.APPROVED)))
        .hasMessageContaining("before the claim was reported");
    as.run(CHECKER, () -> reserves.approve(rc.getId(), LocalDate.of(2026, 5, 3)));
    assertThatThrownBy(
            () ->
                fx.enterSettlement(
                    claim.getId(),
                    "C-000201",
                    CostType.LOSS,
                    SettlementType.PARTIAL,
                    "20000",
                    null))
        .hasMessageContaining("exceeds the outstanding");
    assertThatThrownBy(
            () ->
                fx.enterSettlement(
                    claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "100", "100"))
        .hasMessageContaining("absorb");
  }

  private static ClaimRequest withDates(ClaimRequest r, LocalDate loss, LocalDate reported) {
    return new ClaimRequest(
        r.companyId(),
        r.policyNo(),
        r.riskLineNo(),
        loss,
        reported,
        r.natureOfLoss(),
        r.causeOfLoss(),
        r.lossLocation(),
        r.description(),
        r.currency(),
        r.claimantCode(),
        r.parties(),
        r.initialLossReserve(),
        r.initialExpenseReserve());
  }

  private static ClaimRequest withCurrency(ClaimRequest r, String currency) {
    return new ClaimRequest(
        r.companyId(),
        r.policyNo(),
        r.riskLineNo(),
        r.lossDate(),
        r.reportedDate(),
        r.natureOfLoss(),
        r.causeOfLoss(),
        r.lossLocation(),
        r.description(),
        currency,
        r.claimantCode(),
        r.parties(),
        r.initialLossReserve(),
        r.initialExpenseReserve());
  }
}
