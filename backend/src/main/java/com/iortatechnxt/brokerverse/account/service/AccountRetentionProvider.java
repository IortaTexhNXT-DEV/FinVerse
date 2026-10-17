package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (BRNB.106): accounts voided, cancelled
 * after issuance or with the placement cancelled whose last change is on or before the cutoff. Read
 * only.
 */
@Component
@Transactional(readOnly = true)
public class AccountRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "ACCOUNT";

  private final AccountRepository accounts;

  /**
   * Creates the provider.
   *
   * @param accounts accounts
   */
  public AccountRetentionProvider(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(accounts, criteria, AccountStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(accounts, criteria, AccountStatus.class, limit).stream()
        .map(AccountRetentionProvider::candidate)
        .toList();
  }

  private static RetentionCandidate candidate(Account a) {
    return new RetentionCandidate(
        a.getArn(),
        a.getClientName() + " - " + a.getProductCode(),
        a.getStatus().name(),
        RetentionQueries.lastActivity(a),
        "/accounts/" + a.getId());
  }
}
