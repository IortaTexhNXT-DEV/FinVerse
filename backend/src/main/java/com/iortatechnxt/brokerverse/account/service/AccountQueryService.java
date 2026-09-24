package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account reads for screens and later modules (BRNB.050): one account with its items and numbers
 * loaded, look-up by ARN, multi-criteria search and the readiness check.
 */
@Service
@Transactional(readOnly = true)
public class AccountQueryService {

  /** Statuses of accounts that are live and not yet booked. */
  public static final Set<AccountStatus> PRE_BOOKED =
      EnumSet.complementOf(
          EnumSet.of(AccountStatus.BOOKED, AccountStatus.CANCELLED, AccountStatus.VOIDED));

  private final AccountRepository accounts;
  private final AccountChecks checks;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param checks completeness checks
   */
  public AccountQueryService(AccountRepository accounts, AccountChecks checks) {
    this.accounts = accounts;
    this.checks = checks;
  }

  /**
   * One account with its items, PN and policy numbers loaded.
   *
   * @param id id
   * @return account
   */
  public Account get(Long id) {
    return loaded(
        accounts
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(AccountService.ENTITY, id)));
  }

  /**
   * An account by ARN, loaded.
   *
   * @param arn Account Reference Number
   * @return account
   */
  public Account requireByArn(String arn) {
    return loaded(
        accounts
            .findByArn(arn)
            .orElseThrow(() -> new ResourceNotFoundException(AccountService.ENTITY, arn)));
  }

  /**
   * Accounts matching the criteria (list columns only; items are not loaded).
   *
   * @param search criteria
   * @param pageable page and sort
   * @return page of accounts
   */
  public Page<Account> search(AccountSearch search, Pageable pageable) {
    return accounts.findAll(AccountSpecifications.of(search), pageable);
  }

  /**
   * Accounts not yet booked (nor voided or cancelled) found by ARN, policy number or promissory
   * note number: contract for cashiering and production reconciliation (Operations BRD-2, CSHID.020
   * / PRCID.023), which match collections to accounts before booking.
   *
   * @param companyId company
   * @param reference ARN, policy number or PN number
   * @return matching pre-booked accounts, loaded
   */
  public List<Account> preBooked(Long companyId, String reference) {
    if (reference == null || reference.isBlank()) {
      return List.of();
    }
    return accounts.findByReference(companyId, PRE_BOOKED, reference.strip()).stream()
        .map(AccountQueryService::loaded)
        .toList();
  }

  /**
   * Accounts of a client, newest first.
   *
   * @param clientId client
   * @return accounts
   */
  public List<Account> byClient(Long clientId) {
    return accounts.findByClientIdOrderByCreatedAtDesc(clientId);
  }

  /**
   * Readiness of an account: missing fields and documents, duplicates, premium, TSU.
   *
   * @param id account
   * @return check
   */
  public AccountCheck check(Long id) {
    return checks.check(get(id));
  }

  private static Account loaded(Account account) {
    Hibernate.initialize(account.getPnNumbers());
    Hibernate.initialize(account.getPolicyNumbers());
    for (RiskItem item : account.getItems()) {
      Hibernate.initialize(item.getInsuredItems());
    }
    return account;
  }
}
