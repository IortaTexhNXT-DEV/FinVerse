package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.crm.service.ClientIdentityChanged;
import com.iortatechnxt.brokerverse.crm.service.ClientRegistered;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistEntriesChanged;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The event triggers of screening (SNSRP-602, 303, 204; FR-SS-030): a client registered, a client's
 * identity changed, an account submitted, watchlist entries changed. Each listens after the
 * business transaction committed and screens in a new transaction, so the business transaction
 * never waits for or fails because of screening (FR-SS-030 R4); a screening failure is logged and
 * the next batch run catches up.
 */
@Component
public class ScreeningTriggers {

  private static final Logger LOG = LoggerFactory.getLogger(ScreeningTriggers.class);

  private final ScreeningEngine engine;
  private final BatchScreening batch;
  private final AccountService accounts;
  private final TransactionTemplate tx;

  /**
   * Creates the triggers.
   *
   * @param engine engine
   * @param batch delta screening
   * @param accounts accounts (read)
   * @param txManager transactions
   */
  public ScreeningTriggers(
      ScreeningEngine engine,
      BatchScreening batch,
      AccountService accounts,
      PlatformTransactionManager txManager) {
    this.engine = engine;
    this.batch = batch;
    this.accounts = accounts;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Screens a new client (trigger CLIENT_REGISTERED).
   *
   * @param event the registration
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRegistered(ClientRegistered event) {
    safely(
        "client " + event.code(),
        () ->
            engine.screenClient(
                event.clientId(), ScreeningTrigger.CLIENT_REGISTERED, event.code()));
  }

  /**
   * Screens a client whose identity changed (trigger CLIENT_CHANGED).
   *
   * @param event the change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onIdentityChanged(ClientIdentityChanged event) {
    safely(
        "client " + event.clientId(),
        () -> engine.screenClient(event.clientId(), ScreeningTrigger.CLIENT_CHANGED, null));
  }

  /**
   * Screens the client of a submitted account (trigger ACCOUNT_SUBMITTED, SNSRP-303).
   *
   * @param event the account status change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAccountStatus(AccountStatusChanged event) {
    if (event.to() != AccountStatus.SUBMITTED) {
      return;
    }
    safely(
        "account " + event.arn(),
        () -> {
          Account account = accounts.get(event.accountId());
          return engine.screenClient(
              account.getClientId(), ScreeningTrigger.ACCOUNT_SUBMITTED, event.arn());
        });
  }

  /**
   * Rebuilds the keys of changed entries and screens the in-scope clients sharing a key with them
   * (trigger LIST_CHANGE).
   *
   * @param event the entries changed
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEntriesChanged(WatchlistEntriesChanged event) {
    String what = "list " + event.cause();
    List<ListedEntry> active = safely(what, () -> batch.rekeyEntries(event.entryIds()));
    Map<Long, List<Long>> candidates =
        active == null || active.isEmpty()
            ? Map.of()
            : safely(what, () -> batch.candidatesByCompany(active));
    if (candidates != null) {
      candidates.forEach(
          (companyId, clientIds) ->
              safely(what, () -> batch.screenChange(companyId, clientIds, active, event.cause())));
    }
  }

  private <T> T safely(String what, Supplier<T> work) {
    try {
      return tx.execute(s -> work.get());
    } catch (RuntimeException ex) {
      LOG.warn("Screening of {} not completed: {}", what, ex.getMessage());
      return null;
    }
  }
}
