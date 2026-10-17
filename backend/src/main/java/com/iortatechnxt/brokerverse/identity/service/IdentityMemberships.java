package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The membership events of UIDM-ISC, which orchestrates the membership of the group profiles: the
 * group profiles sent replace those of the user at once, without an approval in the system. The
 * group profiles are checked before anything is applied, so that a change touching several users,
 * or a joiner or mover carrying group profiles, is refused as a whole.
 */
@Service
public class IdentityMemberships {

  private final IdentitySyncService sync;
  private final GroupProfileChecks checks;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param sync intake of the events
   * @param checks checks of the group profiles
   * @param transactions transaction manager
   */
  public IdentityMemberships(
      IdentitySyncService sync,
      GroupProfileChecks checks,
      PlatformTransactionManager transactions) {
    this.sync = sync;
    this.checks = checks;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * Checks the group profiles of a membership event without applying it: a refusal is recorded as a
   * refused membership event and alerted to the System Administrators.
   *
   * @param account the account with the group profiles the user would hold
   * @param source where it comes from
   * @return the refused event, or empty when the group profiles may be given
   */
  public Optional<IdentityEvent> refused(DirectoryAccount account, IdentityEventSource source) {
    try {
      tx.executeWithoutResult(s -> checks.groups(account.groupProfiles()));
      return Optional.empty();
    } catch (IdentityRefused ex) {
      return Optional.of(send(account, source));
    }
  }

  /**
   * Sends a membership event: records it and applies it.
   *
   * @param account the account with the group profiles the user holds after the event
   * @param source where it comes from
   * @return the event with its outcome
   */
  public IdentityEvent send(DirectoryAccount account, IdentityEventSource source) {
    return sync.receive(account, IdentityEventType.MEMBERSHIP, source);
  }
}
