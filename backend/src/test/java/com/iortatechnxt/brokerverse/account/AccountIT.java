package com.iortatechnxt.brokerverse.account;

import static com.iortatechnxt.brokerverse.account.AccountFixtures.draft;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.token;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountCheck;
import com.iortatechnxt.brokerverse.account.service.AccountClientRecords;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.account.service.AccountTaggingService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@IntegrationTest
@RecordApplicationEvents
class AccountIT {

  @Autowired private AccountService accounts;
  @Autowired private AccountQueryService queries;
  @Autowired private AccountTaggingService tagging;
  @Autowired private AccountClientRecords clientRecords;
  @Autowired private WorkflowService workflow;
  @Autowired private WorkflowViewService workflowView;
  @Autowired private AccountFixtures fx;
  @Autowired private AsUser as;
  @Autowired private ApplicationEvents events;

  private Account create(String user, AccountDraft draft) {
    return as.run(user, () -> accounts.createDraft(NewAccount.direct(fx.company(), draft)));
  }

  private Account motor(Client client) {
    return create(
        "ao", draft(client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "1000000"))));
  }

  @Test
  void createdAccountGetsArnPremiumSalesUnitContactAndWorkCase() {
    Client client = fx.confirmed("CL-2026-900001");
    Account account = motor(client);
    assertThat(account.getArn()).matches("ARN-\\d{4}-\\d{6}");
    assertThat(account.getStatus()).isEqualTo(AccountStatus.DRAFT);
    assertThat(account.getClientCode()).isEqualTo("CL-2026-900001");
    assertThat(account.getLineCode()).isEqualTo("MOTOR");
    assertThat(account.getTotalSumInsured()).isEqualByComparingTo("1000000");
    assertThat(account.getPremium().netPremium()).isEqualByComparingTo("12425.00");
    assertThat(account.getPremium().grossPremium()).isEqualByComparingTo("15562.69");
    assertThat(account.getPremium().commissionRate()).isEqualByComparingTo("17.5");
    assertThat(account.getSales().team()).isEqualTo("T-CBG1");
    assertThat(account.getSales().costCenter()).isEqualTo("NB-CBG-M");
    assertThat(account.getSales().accountOfficer()).isEqualTo("ao");
    assertThat(account.getContact().email()).isEqualTo(client.getEmail());
    Account loaded = queries.get(account.getId());
    assertThat(loaded.getItems())
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getPlateNo()).startsWith("P");
              assertThat(i.getPremium()).isEqualByComparingTo("12425.00");
              assertThat(i.getRate()).isEqualByComparingTo("1.3");
            });
    assertThat(queries.requireByArn(account.getArn()).getId()).isEqualTo(account.getId());
    var view = workflowView.view("Account", String.valueOf(account.getId())).orElseThrow();
    assertThat(view.workCase().getReference()).isEqualTo(account.getArn());
    assertThat(view.workCase().getStageCode()).isEqualTo("DRAFT");
    assertThat(clientRecords.recordsOf(client.getId()))
        .extracting("reference")
        .contains(account.getArn());
  }

  @Test
  void arnFromAQuotationIsKeptAndPremiumTaken() {
    Client client = fx.confirmed("CL-2026-900002");
    String arn = "ARN-2026-7" + String.format("%05d", Math.abs(token().hashCode()) % 100000);
    Account account =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    new NewAccount(
                        fx.company(),
                        arn,
                        new Account.Origin("QT-2026-000001", null),
                        draft(client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "1000000"))),
                        null,
                        "ao2")));
    assertThat(account.getArn()).isEqualTo(arn);
    assertThat(account.getQuotationRef()).isEqualTo("QT-2026-000001");
    assertThat(account.getSales().team()).isEqualTo("T-CORP1");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        accounts.createDraft(
                            new NewAccount(
                                fx.company(),
                                arn,
                                Account.Origin.DIRECT,
                                draft(
                                    client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "1"))),
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("ARN_IN_USE");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        accounts.createDraft(
                            new NewAccount(
                                fx.company(),
                                "ARN-X",
                                Account.Origin.DIRECT,
                                draft(client.getId(), "MTR10", "CBG", List.of()),
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("ARN_INVALID");
  }

  @Test
  void submitNeedsMinimumFieldsDocumentsAndPremiumThenProcessingValidates() {
    Client client = fx.confirmed("CL-2026-900001");
    String id = token();
    RiskItemData noEngine =
        new RiskItemData(
            null,
            new BigDecimal("900000"),
            null,
            null,
            null,
            new RiskItemData.Vehicle(
                "P" + id, null, null, "C" + id, "Ford", "Ranger", 2025, null, null, null),
            null,
            null);
    Account account = create("ao", draft(client.getId(), "MTR10", "CBG", List.of(noEngine)));
    AccountCheck check = queries.check(account.getId());
    assertThat(check.fieldErrors()).containsKey("items[0].engineNo");
    assertThat(check.missingDocuments()).containsExactly("Insurance Declaration Form (IDF)");
    assertThat(check.readyToSubmit()).isFalse();
    assertThatThrownBy(() -> as.run("ao", () -> accounts.submit(account.getId(), null)))
        .isInstanceOf(FieldValidationException.class)
        .satisfies(
            e ->
                assertThat(((FieldValidationException) e).getFieldErrors())
                    .containsKey("items[0].engineNo"));

    as.run(
        "ao",
        () ->
            accounts.update(
                account.getId(),
                draft(client.getId(), "MTR10", "CBG", List.of(vehicle(id, "900000")))));
    assertThatThrownBy(() -> as.run("ao", () -> accounts.submit(account.getId(), "go")))
        .extracting("code")
        .isEqualTo("MISSING_DOCUMENTS");
    fx.attach(account.getId(), "IDF", "ao");
    assertThat(queries.check(account.getId()).readyToSubmit()).isTrue();
    as.run("ao", () -> accounts.submit(account.getId(), "please process"));
    assertThat(queries.get(account.getId()).getStatus()).isEqualTo(AccountStatus.SUBMITTED);
    assertThat(events.stream(AccountStatusChanged.class))
        .anyMatch(e -> e.arn().equals(account.getArn()) && e.to() == AccountStatus.SUBMITTED);
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        accounts.update(
                            account.getId(),
                            draft(client.getId(), "MTR10", "CBG", List.of(vehicle(id, "1"))))))
        .extracting("code")
        .isEqualTo("ACCOUNT_NOT_EDITABLE");
    as.run(
        "proc",
        () ->
            accounts.update(
                account.getId(),
                draft(client.getId(), "MTR10", "CBG", List.of(vehicle(id, "950000")))));

    as.run("proc", () -> accounts.validate(account.getId(), "ok"));
    Account validated = queries.get(account.getId());
    assertThat(validated.getStatus()).isEqualTo(AccountStatus.AWAITING_PAYMENT);
    assertThat(validated.getTotalSumInsured()).isEqualByComparingTo("950000");
  }

  @Test
  void processingReturnsAndMarketingResubmits() {
    Client client = fx.confirmed("CL-2026-900003");
    Account account =
        create(
            "ao2",
            draft(
                client.getId(),
                "CGL01",
                "CORBANK",
                List.of(
                    RiskItemData.generic(
                        "Warehouse operations " + token(), new BigDecimal("5000000"), null))));
    as.run("ao2", () -> accounts.submit(account.getId(), null));
    Long caseId =
        workflowView
            .view("Account", String.valueOf(account.getId()))
            .orElseThrow()
            .workCase()
            .getId();
    as.run(
        "proc",
        () ->
            workflow.genericTransition(
                caseId,
                "return",
                new TransitionNote("INCOMPLETE_DETAILS", "Add the site address")));
    assertThat(queries.get(account.getId()).getStatus())
        .isEqualTo(AccountStatus.RETURNED_TO_MARKETING);
    as.run(
        "ao2",
        () ->
            accounts.update(
                account.getId(),
                draft(
                    client.getId(),
                    "CGL01",
                    "CORBANK",
                    List.of(
                        RiskItemData.generic(
                            "Warehouse operations, Pier 4 " + token(),
                            new BigDecimal("5000000"),
                            null)))));
    as.run("ao2", () -> accounts.resubmit(account.getId(), "address added"));
    assertThat(queries.get(account.getId()).getStatus()).isEqualTo(AccountStatus.SUBMITTED);
  }

  @Test
  void validationNeedsAConfirmedClientAndTsuClearance() {
    Client prospect = fx.prospect();
    Account forProspect =
        create(
            "ao",
            draft(
                prospect.getId(),
                "CGL01",
                "CBG",
                List.of(RiskItemData.generic("Shop " + token(), new BigDecimal("1000000"), null))));
    as.run("ao", () -> accounts.submit(forProspect.getId(), null));
    assertThatThrownBy(() -> as.run("proc", () -> accounts.validate(forProspect.getId(), null)))
        .extracting("code")
        .isEqualTo("CLIENT_NOT_CONFIRMED");

    Client client = fx.confirmed("CL-2026-900003");
    Account big =
        create(
            "ao2",
            draft(
                client.getId(),
                "CAR11",
                "CORBANK",
                List.of(
                    RiskItemData.generic(
                        "Tower project " + token(), new BigDecimal("60000000"), null))));
    as.run("ao2", () -> accounts.submit(big.getId(), null));
    AccountCheck check = queries.check(big.getId());
    assertThat(check.tsuRequired()).isTrue();
    assertThat(check.tsuRule()).isEqualTo("TSI_50M");
    assertThatThrownBy(() -> as.run("proc", () -> accounts.validate(big.getId(), null)))
        .extracting("code")
        .isEqualTo("TSU_CLEARANCE_REQUIRED");
    as.run("tsu", () -> accounts.clearTsu(big.getId(), "terms agreed with insurer"));
    as.run("proc", () -> accounts.validate(big.getId(), null));
    Account validated = queries.get(big.getId());
    assertThat(validated.getStatus()).isEqualTo(AccountStatus.AWAITING_PAYMENT);
    assertThat(validated.getTsu().clearedBy()).isEqualTo("tsu");
  }

  @Test
  void directPaymentAccountsSkipThePaymentGate() {
    Client client = fx.confirmed("CL-2026-900003");
    AccountDraft base =
        draft(
            client.getId(),
            "INL01",
            "CORBANK",
            List.of(RiskItemData.generic("Transit " + token(), new BigDecimal("2000000"), null)));
    AccountDraft direct =
        new AccountDraft(
            base.clientId(),
            base.productCode(),
            base.marketSegment(),
            base.sourceChannel(),
            base.insurerCode(),
            base.insurerBranch(),
            base.periodFrom(),
            base.periodTo(),
            false,
            1,
            "PHP",
            PaymentArrangement.DIRECT_TO_INSURER,
            null,
            null,
            base.items(),
            null,
            null,
            null);
    Account account = create("ao2", direct);
    assertThat(account.getDirectPaymentTaggedBy()).isEqualTo("ao2");
    as.run("ao2", () -> accounts.submit(account.getId(), null));
    as.run("proc", () -> accounts.validate(account.getId(), null));
    Account ready = queries.get(account.getId());
    assertThat(ready.getStatus()).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(ready.getLifecycle().getPaymentStatus()).isEqualTo(PaymentStatus.DIRECT);
    var history =
        workflowView.view("Account", String.valueOf(account.getId())).orElseThrow().history();
    assertThat(history).anyMatch(h -> h.getAction().equals("payment_confirmed") && h.isAutomatic());

    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        accounts.createDraft(
                            NewAccount.direct(
                                fx.company(),
                                new AccountDraft(
                                    client.getId(),
                                    "PAR01",
                                    "CBG",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    1,
                                    null,
                                    PaymentArrangement.DIRECT_TO_INSURER,
                                    null,
                                    null,
                                    List.of(),
                                    null,
                                    null,
                                    null)))))
        .extracting("code")
        .isEqualTo("DIRECT_PAYMENT_NOT_ELIGIBLE");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        tagging.setPaymentArrangement(
                            account.getId(), PaymentArrangement.VIA_BDOI)))
        .extracting("code")
        .isEqualTo("PAYMENT_ARRANGEMENT_LOCKED");
    var search =
        queries.search(
            new AccountSearch(
                fx.company(),
                null,
                null,
                null,
                null,
                "INL01",
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "ao2",
                false),
            PageRequest.of(0, 50));
    assertThat(search.getContent()).extracting(Account::getArn).contains(account.getArn());
  }

  @Test
  void preBookedAccountsAreFoundByArnPolicyOrPromissoryNote() {
    Client client = fx.confirmed("CL-2026-900002");
    String pn = "PN-" + token();
    AccountDraft base = draft(client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "700000")));
    AccountDraft withPn =
        new AccountDraft(
            base.clientId(),
            base.productCode(),
            base.marketSegment(),
            base.sourceChannel(),
            base.insurerCode(),
            base.insurerBranch(),
            base.periodFrom(),
            base.periodTo(),
            false,
            1,
            "PHP",
            PaymentArrangement.DIRECT_TO_INSURER,
            new com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage(
                "BDO_AUTO_LOANS", null, List.of(pn)),
            null,
            base.items(),
            null,
            null,
            null);
    Account account = create("ao", withPn);
    assertThat(account.isDirectPayment()).isTrue();
    assertThat(queries.preBooked(fx.company(), account.getArn()))
        .extracting(Account::getArn)
        .containsExactly(account.getArn());
    assertThat(queries.preBooked(fx.company(), " " + pn + " "))
        .singleElement()
        .satisfies(a -> assertThat(a.getItems()).hasSize(1));
    assertThat(queries.preBooked(fx.company(), "NO-SUCH-REF")).isEmpty();
    assertThat(queries.preBooked(fx.company(), " ")).isEmpty();
  }

  @Test
  void directBookingNeedsTheIssuedPolicy() {
    Client client = fx.confirmed("CL-2026-900002");
    Account account = motor(client);
    fx.attach(account.getId(), "IDF", "ao");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    assertThatThrownBy(() -> as.run("proc", () -> accounts.directBooking(account.getId(), null)))
        .extracting("code")
        .isEqualTo("POLICY_DOCUMENT_REQUIRED");
    fx.attach(account.getId(), "POLICY_COPY", "proc");
    as.run("proc", () -> accounts.directBooking(account.getId(), "issued by the dealer's insurer"));
    Account booked = queries.get(account.getId());
    assertThat(booked.getStatus()).isEqualTo(AccountStatus.POLICY_ISSUED);
    assertThat(booked.isDirectBooking()).isTrue();
  }
}
