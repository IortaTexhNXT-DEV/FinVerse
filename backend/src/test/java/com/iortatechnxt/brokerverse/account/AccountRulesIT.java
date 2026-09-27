package com.iortatechnxt.brokerverse.account;

import static com.iortatechnxt.brokerverse.account.AccountFixtures.draft;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.location;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.token;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountClientRecords;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.AccountTaggingService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@IntegrationTest
@RecordApplicationEvents
class AccountRulesIT {

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
  void motorDuplicatesAreRejectedExceptCtplAgainstComprehensive() {
    Client client = fx.confirmed("CL-2026-900002");
    String id = token();
    Account first =
        create("ao", draft(client.getId(), "MTR10", "CBG", List.of(vehicle(id, "800000"))));
    RiskItemData sameEngine =
        new RiskItemData(
            null,
            new BigDecimal("800000"),
            null,
            null,
            null,
            new RiskItemData.Vehicle(
                "X" + token(),
                null,
                "e-" + id.toLowerCase(),
                null,
                null,
                null,
                null,
                null,
                null,
                null),
            null,
            null);
    assertThatThrownBy(
            () -> create("ao", draft(client.getId(), "MTR15", "CBG", List.of(sameEngine))))
        .hasMessageContaining(first.getArn())
        .hasMessageContaining("engine number")
        .extracting("code")
        .isEqualTo("DUPLICATE_ACCOUNT");
    AccountDraft ctpl =
        new AccountDraft(
            client.getId(),
            "CTP01",
            "CBG",
            null,
            null,
            null,
            AccountFixtures.FROM,
            AccountFixtures.TO,
            false,
            1,
            "PHP",
            PaymentArrangement.VIA_BDOI,
            null,
            null,
            List.of(vehicle(id, "0")),
            null,
            null,
            null);
    Account compulsory = create("ao", ctpl);
    assertThat(compulsory.getPremium().isRated()).isFalse();

    Long caseId =
        workflowView
            .view("Account", String.valueOf(first.getId()))
            .orElseThrow()
            .workCase()
            .getId();
    as.run(
        "ao",
        () -> workflow.genericTransition(caseId, "void", new TransitionNote("DUPLICATE", null)));
    assertThat(queries.get(first.getId()).getStatus()).isEqualTo(AccountStatus.VOIDED);
    Account again = create("ao", draft(client.getId(), "MTR15", "CBG", List.of(sameEngine)));
    assertThat(again.getStatus()).isEqualTo(AccountStatus.DRAFT);
    var page =
        queries.search(
            new AccountSearch(
                fx.company(),
                first.getArn(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false),
            PageRequest.of(0, 10));
    assertThat(page.getContent()).isEmpty();
    var withVoided =
        queries.search(
            new AccountSearch(
                fx.company(),
                first.getArn(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true),
            PageRequest.of(0, 10));
    assertThat(withVoided.getContent()).extracting(Account::getArn).containsExactly(first.getArn());
  }

  @Test
  void fireDuplicatesNeedTheSameClientLocationAndItem() {
    Client client = fx.confirmed("CL-2026-900001");
    String street = token() + " Mabini Street";
    Account first =
        create(
            "ao",
            draft(
                client.getId(), "PAR01", "CBG", List.of(location(street, "Building", "3000000"))));
    assertThat(first.getPremium().netPremium()).isEqualByComparingTo("7500.00");
    assertThatThrownBy(
            () ->
                create(
                    "ao",
                    draft(
                        client.getId(),
                        "PAR08",
                        "CBG",
                        List.of(location(street.toUpperCase() + ",", "building", "3000000")))))
        .extracting("code")
        .isEqualTo("DUPLICATE_ACCOUNT");
    Account contents =
        create(
            "ao",
            draft(client.getId(), "PAR08", "CBG", List.of(location(street, "Contents", "500000"))));
    assertThat(contents.getStatus()).isEqualTo(AccountStatus.DRAFT);
    Client other = fx.confirmed("CL-2026-900002");
    Account otherClient =
        create(
            "ao",
            draft(other.getId(), "PAR01", "CBG", List.of(location(street, "Building", "3000000"))));
    assertThat(otherClient.getId()).isNotNull();
    var byLocation =
        queries.search(
            new AccountSearch(
                fx.company(),
                null,
                null,
                null,
                street.toLowerCase(),
                null,
                "PROPERTY",
                null,
                List.of(AccountStatus.DRAFT),
                null,
                null,
                null,
                null,
                null,
                false),
            PageRequest.of(0, 10));
    assertThat(byLocation.getContent()).hasSize(3);
  }

  @Test
  void freeFirstYearIsTaggedAndCancelledWithAudit() {
    Client client = fx.confirmed("CL-2026-900001");
    Account account = motor(client);
    as.run("ao", () -> tagging.tagFreeFirstYear(account.getId(), LocalDate.of(2026, 10, 1)));
    Account tagged = queries.get(account.getId());
    assertThat(tagged.getFreeFirstYear().active()).isTrue();
    assertThat(tagged.getFreeFirstYear().end()).isEqualTo(LocalDate.of(2027, 9, 30));
    var ffyPage =
        queries.search(
            new AccountSearch(
                fx.company(),
                tagged.getArn(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                false),
            PageRequest.of(0, 10));
    assertThat(ffyPage.getContent()).hasSize(1);
    assertThatThrownBy(
            () -> as.run("ao", () -> tagging.cancelFreeFirstYear(account.getId(), "NOPE", null)))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    as.run(
        "ao",
        () -> tagging.cancelFreeFirstYear(account.getId(), "LOAN_CANCELLED", "loan paid off"));
    Account cancelled = queries.get(account.getId());
    assertThat(cancelled.getFreeFirstYear().active()).isFalse();
    assertThat(cancelled.getFreeFirstYear().start()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(cancelled.getFreeFirstYear().cancelReason()).contains("loan paid off");
    assertThatThrownBy(
            () -> as.run("ao", () -> tagging.cancelFreeFirstYear(account.getId(), "OTHERS", null)))
        .extracting("code")
        .isEqualTo("FFY_NOT_TAGGED");

    Account fire =
        create(
            "ao",
            draft(
                client.getId(),
                "PAR01",
                "CBG",
                List.of(location(token() + " Rizal Ave", "Building", "1000000"))));
    assertThatThrownBy(
            () ->
                as.run(
                    "ao", () -> tagging.tagFreeFirstYear(fire.getId(), LocalDate.of(2026, 10, 1))))
        .extracting("code")
        .isEqualTo("FFY_NOT_ELIGIBLE");
    assertThat(tagging.byVehicle(fx.company(), tagged.getItems().get(0).getChassisNo()))
        .extracting(Account::getArn)
        .containsExactly(tagged.getArn());
  }

  @Test
  void draftRulesComeFromTheProduct() {
    Client client = fx.confirmed("CL-2026-900003");
    assertThatThrownBy(
            () ->
                create(
                    "ao2",
                    draft(
                        client.getId(),
                        "PAR01",
                        "CORBANK",
                        List.of(location(token(), "Building", "1")))))
        .extracting("code")
        .isEqualTo("SEGMENT_NOT_ALLOWED");
    AccountDraft multi =
        new AccountDraft(
            client.getId(),
            "CGL01",
            "CORBANK",
            null,
            null,
            null,
            null,
            null,
            true,
            3,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null,
            null);
    assertThatThrownBy(() -> create("ao2", multi))
        .extracting("code")
        .isEqualTo("MULTI_YEAR_NOT_ALLOWED");
    AccountDraft ffy =
        new AccountDraft(
            client.getId(),
            "CGL01",
            "CORBANK",
            null,
            null,
            null,
            null,
            null,
            false,
            1,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null,
            LocalDate.of(2026, 1, 1));
    assertThatThrownBy(() -> create("ao2", ffy)).extracting("code").isEqualTo("FFY_NOT_ELIGIBLE");
    AccountDraft period =
        new AccountDraft(
            client.getId(),
            "CGL01",
            "CORBANK",
            null,
            null,
            null,
            AccountFixtures.TO,
            AccountFixtures.FROM,
            false,
            1,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null,
            null);
    assertThatThrownBy(() -> create("ao2", period))
        .extracting("code")
        .isEqualTo("ACCOUNT_PERIOD_INVALID");
    AccountDraft noClient =
        new AccountDraft(
            null, "CGL01", null, null, null, null, null, null, false, 1, null, null, null, null,
            List.of(), null, null, null);
    assertThatThrownBy(() -> create("ao2", noClient))
        .extracting("code")
        .isEqualTo("ACCOUNT_CLIENT_REQUIRED");

    AccountDraft proRata =
        new AccountDraft(
            client.getId(),
            "CGL01",
            "CORBANK",
            null,
            null,
            null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 7, 2),
            false,
            1,
            null,
            null,
            null,
            null,
            List.of(
                RiskItemData.generic(
                    "Office " + token(), new BigDecimal("2000000"), new BigDecimal("0.5"))),
            com.iortatechnxt.brokerverse.catalog.service.PeriodBasis.PRO_RATA,
            new BigDecimal("12"),
            null);
    Account prorated = create("ao2", proRata);
    // 10,000 x 182/365 = 4,986.30 (no minimum premium on pro-rata)
    assertThat(prorated.getPremium().netPremium()).isEqualByComparingTo("4986.30");
    assertThat(prorated.getPremium().commission()).isEqualByComparingTo("598.36");
    assertThat(prorated.getPremium().ratingBasis()).isEqualTo("PRO_RATA");
    assertThat(prorated.getContact().name()).isEqualTo("Pacific Harbor Logistics Inc.");
  }
}
