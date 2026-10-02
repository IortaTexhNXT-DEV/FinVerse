package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleOutcome;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.placement.service.EbPlacementService;
import com.iortatechnxt.brokerverse.eb.placement.service.LinePlacement;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Accounts of a confirmed cycle with the shared business type (FR-EB-021 R2, FR-EB-046; BT0): one
 * account per line, RENEWAL accounts referring to the expiring ARN, NEW_BUSINESS accounts of origin
 * EMPLOYEE_BENEFITS, the contract tracked item, and PLACED once every account is booked.
 */
@IntegrationTest
class EbPlacementIT {

  private static final String[] TO_CONFIRMED_NB = {
    "start",
    "remarket",
    "release_tor",
    "build_comparative",
    "submit",
    "approve",
    "present",
    "confirm"
  };

  private static final String[] TO_CONFIRMED_RENEWAL = {
    "send_ra",
    "record_feedback",
    "stay_with_incumbent",
    "build_comparative",
    "submit",
    "approve",
    "present",
    "confirm"
  };

  @Autowired private EbFixtures fx;
  @Autowired private EbPlacementService placement;
  @Autowired private EbCycleRepository cycles;
  @Autowired private EbProgrammeRepository programmes;
  @Autowired private EbTrackedItemRepository items;
  @Autowired private AccountRepository accounts;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;

  @Test
  void aRenewalCycleCreatesRenewalAccountsThatReferToTheExpiringArn() {
    String expiring = "ARN-2025-" + EbFixtures.token();
    EbProgramme p = fx.programme(true, List.of(hmo(LocalDate.of(2026, 10, 1), expiring)));
    EbCycle cycle = fx.cycle(p, BusinessType.RENEWAL, 2026);
    assertThatThrownBy(() -> trigger(cycle)).extracting("code").isEqualTo("EB_CYCLE_NOT_CONFIRMED");
    fx.move(cycle, TO_CONFIRMED_RENEWAL);

    List<Account> created = trigger(cycle);
    assertThat(created).hasSize(1);
    Account account = accounts.findByArn(created.get(0).getArn()).orElseThrow();
    assertThat(account.getBusinessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(account.getClassification().renewalOfRef()).isEqualTo(expiring);
    assertThat(account.getClassification().origin()).isEqualTo(AccountOrigin.EMPLOYEE_BENEFITS);
    assertThat(account.getClientId()).isEqualTo(fx.client().getId());
    assertThat(account.getSales().accountOfficer()).isEqualTo(AO);

    EbCycle placed = cycles.findById(cycle.getId()).orElseThrow();
    assertThat(placed.getStage()).isEqualTo(EbCycleStage.IN_PLACEMENT);
    List<String> arns =
        tx.execute(s -> cycles.findById(cycle.getId()).orElseThrow().getAccountArns());
    assertThat(arns).containsExactly(account.getArn());
    List<EbTrackedItem> contract =
        items.findByProgrammeIdOrderByIdDesc(p.getId()).stream()
            .filter(i -> account.getArn().equals(i.getAccountArn()))
            .toList();
    assertThat(contract).hasSize(1);
    assertThat(contract.get(0).getItemType()).isEqualTo("CONTRACT");
    assertThat(contract.get(0).getPartyCode()).isEqualTo("INS-MGIC");
    assertThat(placement.accountsOfProgramme(fx.company(), p.getId()))
        .extracting(a -> a.account().getArn())
        .containsExactly(account.getArn());

    book(account);
    EbCycle done = cycles.findById(cycle.getId()).orElseThrow();
    assertThat(done.getStage()).isEqualTo(EbCycleStage.PLACED);
    assertThat(done.getOutcome()).isEqualTo(EbCycleOutcome.RENEWED_INCUMBENT);
    EbProgrammeLine line = tx.execute(s -> programmes.findById(p.getId()).orElseThrow().line(1));
    assertThat(line.getCurrentArn()).isEqualTo(account.getArn());
    assertThat(line.getPeriodTo()).isEqualTo(account.getPeriodTo());
  }

  @Test
  void aNewBusinessCycleCreatesNewBusinessAccountsAndWaitsForEveryBooking() {
    EbProgramme p =
        fx.programme(
            false,
            List.of(
                new EbProgrammeLine.Data("HMO", null, null, null, null, null, null, 50),
                new EbProgrammeLine.Data("GPA", null, null, null, null, null, null, 50)));
    EbCycle cycle = fx.cycle(p, BusinessType.NEW_BUSINESS, 2027);
    fx.move(cycle, TO_CONFIRMED_NB);
    Long clientId = fx.client().getId();
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        placement.trigger(
                            fx.company(),
                            cycle.getId(),
                            List.of(
                                new LinePlacement(1, EbFixtures.motorDraft(clientId), null),
                                new LinePlacement(1, EbFixtures.motorDraft(clientId), null)))))
        .extracting("code")
        .isEqualTo("EB_LINE_TWICE");
    List<Account> created =
        as.run(
            AO,
            () ->
                placement.trigger(
                    fx.company(),
                    cycle.getId(),
                    List.of(
                        new LinePlacement(1, EbFixtures.motorDraft(clientId), null),
                        new LinePlacement(2, EbFixtures.motorDraft(clientId), null))));
    assertThat(created)
        .extracting(Account::getBusinessType)
        .containsOnly(BusinessType.NEW_BUSINESS);
    assertThat(created)
        .extracting(a -> a.getClassification().origin())
        .containsOnly(AccountOrigin.EMPLOYEE_BENEFITS);

    book(accounts.findByArn(created.get(0).getArn()).orElseThrow());
    assertThat(cycles.findById(cycle.getId()).orElseThrow().getStage())
        .isEqualTo(EbCycleStage.IN_PLACEMENT);
    book(accounts.findByArn(created.get(1).getArn()).orElseThrow());
    EbCycle done = cycles.findById(cycle.getId()).orElseThrow();
    assertThat(done.getStage()).isEqualTo(EbCycleStage.PLACED);
    assertThat(done.getOutcome()).isEqualTo(EbCycleOutcome.NEW_PLACED);
    assertThat(programmes.findById(p.getId()).orElseThrow().getStatus())
        .isEqualTo(EbProgrammeStatus.ACTIVE);
  }

  private List<Account> trigger(EbCycle cycle) {
    Long clientId = fx.client().getId();
    return as.run(
        AO,
        () ->
            placement.trigger(
                fx.company(),
                cycle.getId(),
                List.of(new LinePlacement(1, EbFixtures.motorDraft(clientId), null))));
  }

  /** Books an account as the booking module does: status BOOKED and its status event. */
  private void book(Account account) {
    tx.executeWithoutResult(
        s -> {
          Account loaded = accounts.findById(account.getId()).orElseThrow();
          AccountStatus from = loaded.getStatus();
          loaded.markStatus(AccountStatus.BOOKED);
          events.publishEvent(
              new AccountStatusChanged(
                  loaded.getId(), loaded.getArn(), from, AccountStatus.BOOKED, "book", null, null));
        });
  }
}
