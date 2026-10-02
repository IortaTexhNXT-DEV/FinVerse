package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ActivePolicyQuery} (FR-SS-034 R3, until SQ10): a client has an active policy when
 * one of its accounts is in POLICY_ISSUED or BOOKED. Policy periods and renewals in progress are
 * not considered until BDOI defines "active policy".
 */
@Component
@Transactional(readOnly = true)
public class AccountActivePolicyQuery implements ActivePolicyQuery {

  private static final Set<AccountStatus> ACTIVE =
      Set.of(AccountStatus.POLICY_ISSUED, AccountStatus.BOOKED);

  private final AccountQueryService accounts;

  /**
   * Creates the adapter.
   *
   * @param accounts account reads
   */
  public AccountActivePolicyQuery(AccountQueryService accounts) {
    this.accounts = accounts;
  }

  @Override
  public boolean hasActivePolicy(Long clientId, LocalDate asOf) {
    return accounts.byClient(clientId).stream().anyMatch(a -> ACTIVE.contains(a.getStatus()));
  }
}
