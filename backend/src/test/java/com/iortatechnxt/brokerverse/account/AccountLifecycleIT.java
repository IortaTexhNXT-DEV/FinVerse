package com.iortatechnxt.brokerverse.account;

import static com.iortatechnxt.brokerverse.account.AccountFixtures.draft;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.location;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.token;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The lifecycle contract used by placement, issuance and booking. */
@IntegrationTest
class AccountLifecycleIT {

  @Autowired private AccountService accounts;
  @Autowired private AccountLifecycleService lifecycle;
  @Autowired private AccountQueryService queries;
  @Autowired private WorkflowViewService workflowView;
  @Autowired private AccountFixtures fx;
  @Autowired private AsUser as;

  private Account awaitingPayment(AccountDraft draft) {
    Account account =
        as.run("ao", () -> accounts.createDraft(NewAccount.direct(fx.company(), draft)));
    fx.attach(account.getId(), "IDF", "ao");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    as.run("proc", () -> accounts.validate(account.getId(), null));
    return queries.get(account.getId());
  }

  private String arnOf(Account account) {
    return account.getArn();
  }

  @Test
  void accountGoesFromPaymentToBookingAndCancellation() {
    Client client = fx.confirmed("CL-DEMO-A001");
    String arn =
        arnOf(
            awaitingPayment(
                draft(client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "750000")))));
    as.run("proc", () -> lifecycle.markPaymentConfirmed(arn, "CLPC payment report 2026-10"));
    Account paid = queries.requireByArn(arn);
    assertThat(paid.getStatus()).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(paid.getLifecycle().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

    assertThatThrownBy(
            () -> as.run("proc", () -> lifecycle.recordPlacement(arn, "PL-1", "INS-LAC", "QC")))
        .extracting("code")
        .isEqualTo("PLACEMENT_INSURER_MISMATCH");
    as.run("proc", () -> lifecycle.recordPlacement(arn, "PL-2026-000001", "INS-MGIC", "MKT"));
    as.run(
        "proc",
        () ->
            lifecycle.recordHoldCover(
                arn, HoldCoverStatus.CONFIRMED, "HC-778", LocalDate.of(2026, 10, 1)));
    Account placed = queries.requireByArn(arn);
    assertThat(placed.getStatus()).isEqualTo(AccountStatus.PLACED);
    assertThat(placed.getLifecycle().getPlacementSlipRef()).isEqualTo("PL-2026-000001");
    assertThat(placed.getLifecycle().getHoldCoverStatus()).isEqualTo(HoldCoverStatus.CONFIRMED);
    assertThat(placed.getLifecycle().getInsurerRef()).isEqualTo("HC-778");

    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        lifecycle.recordPolicy(
                            arn, List.of("P1", "P2"), LocalDate.of(2026, 10, 2))))
        .extracting("code")
        .isEqualTo("POLICY_NUMBERS_MISMATCH");
    as.run(
        "proc",
        () -> lifecycle.recordPolicy(arn, List.of("MGIC-MC-2026-0001"), LocalDate.of(2026, 10, 2)));
    Account issued = queries.requireByArn(arn);
    assertThat(issued.getStatus()).isEqualTo(AccountStatus.POLICY_ISSUED);
    assertThat(issued.getPolicyNumbers()).containsExactly("MGIC-MC-2026-0001");
    assertThat(issued.getLifecycle().isEpolicyReceived()).isTrue();

    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        lifecycle.recordBooking(
                            arn, "BK-1", LocalDate.of(2026, 10, 3), true, "NOPE")))
        .extracting("code")
        .isEqualTo("INVALID_DIMENSION");
    as.run(
        "proc",
        () ->
            lifecycle.recordBooking(arn, "BK-2026-000001", LocalDate.of(2026, 10, 3), true, "MKT"));
    Account booked = queries.requireByArn(arn);
    assertThat(booked.getStatus()).isEqualTo(AccountStatus.BOOKED);
    assertThat(booked.getSales().costCenter()).isEqualTo("MKT");
    assertThat(booked.getLifecycle().isIncentiveFlag()).isTrue();

    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        lifecycle.recordCancellation(
                            arn, "CLIENT_REQUEST", LocalDate.of(2026, 12, 1))))
        .extracting("code")
        .isEqualTo("WORKFLOW_ACTION_NOT_PERMITTED");
    as.run(
        "adjust",
        () -> lifecycle.recordCancellation(arn, "CLIENT_REQUEST", LocalDate.of(2026, 12, 1)));
    Account cancelled = queries.requireByArn(arn);
    assertThat(cancelled.getStatus()).isEqualTo(AccountStatus.CANCELLED);
    assertThat(cancelled.getLifecycle().getCancelledAt()).isEqualTo(LocalDate.of(2026, 12, 1));
  }

  @Test
  void insurerReturnsCancelledPlacementsAndSystemSteps() {
    Client client = fx.confirmed("CL-DEMO-A002");
    AccountDraft base =
        draft(
            client.getId(),
            "PAR01",
            "CBG",
            List.of(location(token() + " Lapu-Lapu St", "Building", "2000000")));
    AccountDraft multiYear =
        new AccountDraft(
            base.clientId(),
            base.productCode(),
            base.marketSegment(),
            base.sourceChannel(),
            base.insurerCode(),
            base.insurerBranch(),
            base.periodFrom(),
            base.periodFrom().plusYears(3),
            true,
            3,
            "PHP",
            null,
            new Mortgage("BDO_HOME_LOANS", "HL-" + token(), List.of("PN-" + token())),
            null,
            base.items(),
            null,
            null,
            null);
    String arn = arnOf(awaitingPayment(multiYear));
    // a system step (no signed-in user): CLPC matching job
    lifecycle.markPaymentConfirmed(arn, "CLPC billing match");
    Account ready = queries.requireByArn(arn);
    assertThat(ready.getLifecycle().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(workflowView.view("Account", String.valueOf(ready.getId())).orElseThrow().history())
        .anyMatch(h -> h.getAction().equals("payment_confirmed") && h.isAutomatic());
    assertThat(ready.getPremium().netPremium()).isEqualByComparingTo("5000.00");
    assertThat(ready.getTermYears()).isEqualTo(3);

    as.run("proc", () -> lifecycle.recordPlacement(arn, "PL-X", "INS-MGIC", "MKT"));
    as.run("proc", () -> lifecycle.recordInsurerReturn(arn, "INSURER_REQUIREMENTS", "Need photos"));
    assertThat(queries.requireByArn(arn).getStatus()).isEqualTo(AccountStatus.RETURNED_BY_INSURER);
    as.run("proc", () -> lifecycle.resubmitPlacement(arn, "photos attached"));
    assertThat(queries.requireByArn(arn).getStatus()).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThatThrownBy(() -> as.run("proc", () -> lifecycle.resubmitPlacement(arn, null)))
        .extracting("code")
        .isEqualTo("ACCOUNT_STATUS_INVALID");
    as.run("proc", () -> lifecycle.cancelPlacement(arn, "CLIENT_REQUEST", "client asked to hold"));
    assertThat(queries.requireByArn(arn).getStatus()).isEqualTo(AccountStatus.PLACEMENT_CANCELLED);
    as.run("proc", () -> lifecycle.reactivate(arn, "client confirmed"));
    as.run("proc", () -> lifecycle.recordPlacement(arn, "PL-Y", "INS-MGIC", "MKT"));
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> lifecycle.recordPolicy(arn, List.of("Y1"), LocalDate.of(2026, 10, 5))))
        .extracting("code")
        .isEqualTo("POLICY_NUMBERS_MISMATCH");
    as.run(
        "proc",
        () -> lifecycle.recordPolicy(arn, List.of("Y1", "Y2", " Y3 "), LocalDate.of(2026, 10, 5)));
    assertThat(queries.requireByArn(arn).getPolicyNumbers()).containsExactly("Y1", "Y2", "Y3");
  }

  @Test
  void directlyBookedAccountsOnlyRecordThePolicyNumbers() {
    Client client = fx.confirmed("CL-DEMO-A001");
    Account account =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    NewAccount.direct(
                        fx.company(),
                        draft(
                            client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "600000"))))));
    fx.attach(account.getId(), "IDF", "ao");
    fx.attach(account.getId(), "EPOLICY", "ao");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    as.run("proc", () -> accounts.directBooking(account.getId(), null));
    as.run(
        "proc",
        () -> lifecycle.recordPolicy(account.getArn(), List.of("D-1"), LocalDate.of(2026, 9, 30)));
    Account issued = queries.get(account.getId());
    assertThat(issued.getStatus()).isEqualTo(AccountStatus.POLICY_ISSUED);
    assertThat(issued.getPolicyNumbers()).containsExactly("D-1");
    assertThatThrownBy(() -> lifecycle.recordPolicy("ARN-1900-000000", List.of("x"), null))
        .hasMessageContaining("ARN-1900-000000");
  }
}
