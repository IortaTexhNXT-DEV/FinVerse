package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the NB_ACCOUNT work case stage on the account (BRNB.022/115) for every transition,
 * including the generic ones run from the workflow panel (return, void), and publishes {@link
 * AccountStatusChanged}.
 */
@Component
public class AccountStatusListener {

  private final AccountRepository accounts;
  private final ApplicationEventPublisher events;

  /**
   * Creates the listener.
   *
   * @param accounts accounts
   * @param events event publisher
   */
  public AccountStatusListener(AccountRepository accounts, ApplicationEventPublisher events) {
    this.accounts = accounts;
    this.events = events;
  }

  /**
   * Mirrors a stage change of an account's work case.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!AccountService.ENTITY.equals(event.entityType())) {
      return;
    }
    Account account =
        accounts
            .findById(Long.valueOf(event.entityId()))
            .orElseThrow(() -> new IllegalStateException("No account " + event.entityId()));
    AccountStatus from = account.getStatus();
    AccountStatus to = AccountStatus.valueOf(event.toStage());
    account.markStatus(to);
    events.publishEvent(
        new AccountStatusChanged(
            account.getId(),
            account.getArn(),
            from,
            to,
            event.action(),
            event.reasonCode(),
            event.comment()));
  }
}
