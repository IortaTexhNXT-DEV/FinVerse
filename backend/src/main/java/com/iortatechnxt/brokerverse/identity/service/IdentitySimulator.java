package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.identity.domain.SimulatedAccount;
import com.iortatechnxt.brokerverse.identity.domain.SimulatedAccountRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Enterprise SSO simulator of SIT and UAT: its accounts (the directory) and the events UIDM-ISC
 * would send for them (joiner, mover, leaver, rehire, status), applied through the same intake as
 * the provisioning interface, so the flows run end to end before the real platform is connected.
 */
@Service
@ConditionalOnProperty(name = "brokerverse.identity.simulator", havingValue = "true")
public class IdentitySimulator {

  private final SimulatedAccountRepository accounts;
  private final IdentitySyncService sync;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the simulator.
   *
   * @param accounts simulator accounts
   * @param sync intake of the events
   * @param transactions transaction manager
   * @param clock clock
   */
  public IdentitySimulator(
      SimulatedAccountRepository accounts,
      IdentitySyncService sync,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.accounts = accounts;
    this.sync = sync;
    this.tx = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  /**
   * Every account of the simulator.
   *
   * @return accounts by Windows ID
   */
  public List<DirectoryAccount> accounts() {
    return tx.execute(
        s ->
            accounts.findAllByOrderByWindowsIdAsc().stream()
                .map(SimulatedAccount::toAccount)
                .toList());
  }

  /**
   * Adds an account (a new joiner in the Enterprise SSO platform).
   *
   * @param account details
   * @return the account
   */
  public DirectoryAccount add(DirectoryAccount account) {
    return tx.execute(
        s -> {
          if (accounts.findByWindowsIdIgnoreCase(account.windowsId()).isPresent()) {
            throw new DuplicateResourceException("Enterprise SSO account", account.windowsId());
          }
          return accounts.save(new SimulatedAccount(account, clock.instant())).toAccount();
        });
  }

  /**
   * Changes an account (details or status).
   *
   * @param windowsId Windows ID
   * @param account details
   * @return the account
   */
  public DirectoryAccount change(String windowsId, DirectoryAccount account) {
    return tx.execute(
        s -> {
          SimulatedAccount found = find(windowsId);
          found.change(account, clock.instant());
          return found.toAccount();
        });
  }

  /**
   * Sends the event of an account to the system, as UIDM-ISC would.
   *
   * @param windowsId Windows ID
   * @param type event
   * @return the event with its outcome
   */
  public IdentityEvent send(String windowsId, IdentityEventType type) {
    DirectoryAccount account = tx.execute(s -> find(windowsId).toAccount());
    DirectoryAccount sent =
        switch (type) {
          case LEAVER -> account.withStatus(DirectoryStatus.DEACTIVATED);
          case REHIRE -> account.withStatus(DirectoryStatus.ACTIVE);
          default -> account;
        };
    if (type == IdentityEventType.LEAVER || type == IdentityEventType.REHIRE) {
      change(windowsId, sent);
    }
    IdentityEventSource source =
        type == IdentityEventType.STATUS
            ? IdentityEventSource.ENTERPRISE_SSO
            : IdentityEventSource.UIDM_ISC;
    return sync.receive(sent, type, source);
  }

  private SimulatedAccount find(String windowsId) {
    return accounts
        .findByWindowsIdIgnoreCase(windowsId)
        .orElseThrow(() -> new ResourceNotFoundException("Enterprise SSO account", windowsId));
  }
}
