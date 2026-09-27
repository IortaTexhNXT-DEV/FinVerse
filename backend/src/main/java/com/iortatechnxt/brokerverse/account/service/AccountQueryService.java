package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeader;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeaderRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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
  private final AccountLegacyHeaderRepository legacyHeaders;
  private final AccountChecks checks;
  private final ProductCatalogService catalog;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param legacyHeaders legacy references of migrated accounts
   * @param checks completeness checks
   * @param catalog product lines (item kind of a saved account)
   */
  public AccountQueryService(
      AccountRepository accounts,
      AccountLegacyHeaderRepository legacyHeaders,
      AccountChecks checks,
      ProductCatalogService catalog) {
    this.accounts = accounts;
    this.legacyHeaders = legacyHeaders;
    this.checks = checks;
    this.catalog = catalog;
  }

  /**
   * Accounts of any status found by an account number (contract of the Customer Servicing Facility,
   * BRCSF-003): the ARN with or without its suffix, a policy number, or the legacy reference or
   * policy number of a migrated account.
   *
   * @param companyId company
   * @param reference account number
   * @return accounts, each once
   */
  public List<Account> byAccountNumber(Long companyId, String reference) {
    if (reference == null || reference.isBlank()) {
      return List.of();
    }
    String ref = reference.strip();
    List<Long> legacyIds =
        Stream.concat(
                legacyHeaders
                    .findByCompanyIdAndLegacyRefAndRolledBackAtIsNull(companyId, ref)
                    .stream(),
                legacyHeaders
                    .findByCompanyIdAndPolicyNoOrCompanyIdAndCoverNo(companyId, ref, companyId, ref)
                    .stream())
            .map(AccountLegacyHeader::getAccountId)
            .toList();
    Map<Long, Account> found = new LinkedHashMap<>();
    accounts
        .findByAccountNumber(companyId, ref.toUpperCase(Locale.ROOT))
        .forEach(a -> found.put(a.getId(), a));
    accounts.findAllById(legacyIds).forEach(a -> found.putIfAbsent(a.getId(), a));
    return List.copyOf(found.values());
  }

  /**
   * Accounts whose loan application number is the given one (contract of the Customer Servicing
   * Facility, BRCSF-003).
   *
   * @param companyId company
   * @param loanApplicationNo loan application number
   * @return accounts
   */
  public List<Account> byLoanApplication(Long companyId, String loanApplicationNo) {
    if (loanApplicationNo == null || loanApplicationNo.isBlank()) {
      return List.of();
    }
    return accounts.findByLoanApplication(
        companyId, loanApplicationNo.strip().toUpperCase(Locale.ROOT));
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
   * The data of a saved account as a draft (Renewal, design section 13): the renewal account of the
   * next term is built from the expiring account as of its last posted endorsement (BRRN.032), with
   * the same client, product, items, insurer, mortgage and PN numbers, and contact.
   *
   * @param arn Account Reference Number of the saved account
   * @return draft carrying its data
   */
  public AccountDraft draftOf(String arn) {
    Account account = requireByArn(arn);
    return AccountFields.draftOf(
        account, catalog.requireLine(account.getLineCode()).getRiskItemKind());
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
