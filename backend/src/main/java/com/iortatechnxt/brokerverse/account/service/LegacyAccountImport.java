package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountClassification;
import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeader;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeaderRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import of in-force legacy policies as accounts by the data migration (object P01; DATA_MIGRATION
 * _DESIGN sections 10, 14.5 and 15): the account is created BOOKED with origin MIGRATED, the
 * premium, policy numbers and sales stamp of legacy, without the NB_ACCOUNT workflow, rating or
 * duplicate checks, and its legacy header (source system, legacy reference, the legacy package kept
 * as given for Renewal sanitation) in {@code acc_account_legacy}. It publishes {@link
 * AccountImported}, not {@link AccountStatusChanged}, so no notification is sent. A rolled-back
 * batch cancels its accounts.
 */
@Service
@Transactional
public class LegacyAccountImport {

  /** Cancellation reason of an account whose migration batch is rolled back. */
  public static final String ROLLBACK_REASON = "Migration batch rolled back";

  private static final String ENTITY = "Account";

  private final AccountRepository accounts;
  private final AccountLegacyHeaderRepository headers;
  private final ProductCatalogService catalog;
  private final ClientService clients;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts accounts
   * @param headers legacy headers
   * @param catalog product catalogue
   * @param clients clients
   * @param numbers document numbers
   * @param audit audit trail
   * @param events event publisher
   * @param currentUser current user (the migration loader)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LegacyAccountImport(
      AccountRepository accounts,
      AccountLegacyHeaderRepository headers,
      ProductCatalogService catalog,
      ClientService clients,
      DocumentNumberService numbers,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.headers = headers;
    this.catalog = catalog;
    this.clients = clients;
    this.numbers = numbers;
    this.audit = audit;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Imports a legacy policy as a booked account.
   *
   * @param request policy, client, cover, premium and legacy header
   * @return the account
   */
  public Account importLegacy(LegacyAccount request) {
    AccountLegacyHeader.LegacyPolicy legacy = request.legacy();
    if (headers
        .findByCompanyIdAndLegacyRefAndRolledBackAtIsNull(request.companyId(), legacy.legacyRef())
        .isPresent()) {
      throw new BusinessRuleException(
          "ACCOUNT_LEGACY_IMPORTED",
          "Legacy policy " + legacy.legacyRef() + " is already imported");
    }
    String arn = numbers.next("ARN-" + BusinessClock.today(clock).getYear());
    Account account =
        Account.create(
            request.companyId(),
            arn,
            Account.Origin.DIRECT,
            data(request),
            request.sales(),
            new AccountClassification(request.businessType(), null, AccountOrigin.MIGRATED));
    account.setPremium(request.premium());
    account.setPaymentArrangement(
        request.cover().arrangement(), currentUser.username(), clock.instant());
    account.recordPolicies(request.policyNos(), request.cover().periodFrom());
    account.recordBooking(legacy.legacyRef(), request.cover().periodFrom(), false);
    account.markStatus(AccountStatus.BOOKED);
    Account saved = accounts.save(account);
    headers.save(new AccountLegacyHeader(saved.getId(), saved.getCompanyId(), legacy));
    audit.record(
        ENTITY,
        arn,
        AuditAction.CREATE,
        "Legacy policy "
            + legacy.policyNo()
            + " ("
            + legacy.sourceSystem()
            + " "
            + legacy.legacyRef()
            + ") imported by batch "
            + legacy.migrationBatch());
    events.publishEvent(
        new AccountImported(saved.getCompanyId(), saved.getId(), arn, legacy.legacyRef()));
    return saved;
  }

  /**
   * Applies a legacy delta to an imported account before the freeze.
   *
   * @param accountId account
   * @param request new data
   * @return the account
   */
  public Account update(Long accountId, LegacyAccount request) {
    Account account = imported(accountId);
    account.apply(data(request));
    account.setPremium(request.premium());
    account.recordPolicies(request.policyNos(), request.cover().periodFrom());
    header(accountId).apply(request.legacy());
    audit.record(
        ENTITY,
        account.getArn(),
        AuditAction.UPDATE,
        "Legacy changes of " + request.legacy().legacyRef() + " applied");
    return account;
  }

  /**
   * Undoes the import of a rolled-back batch: the account is cancelled and its header released.
   *
   * @param accountId account
   * @param batchNo rolled-back batch
   */
  public void rollback(Long accountId, String batchNo) {
    Account account = imported(accountId);
    if (account.getStatus() != AccountStatus.CANCELLED) {
      account.recordCancellation(BusinessClock.today(clock), ROLLBACK_REASON);
      account.markStatus(AccountStatus.CANCELLED);
    }
    header(accountId).rolledBack(clock.instant());
    audit.record(
        ENTITY,
        account.getArn(),
        AuditAction.REVERSE,
        "Migration batch " + batchNo + " rolled back");
  }

  /**
   * The legacy header of an account, if imported.
   *
   * @param accountId account
   * @return header
   */
  @Transactional(readOnly = true)
  public Optional<AccountLegacyHeader> headerOf(Long accountId) {
    return headers.findByAccountId(accountId);
  }

  /**
   * Legacy headers of accounts (list screens).
   *
   * @param accountIds accounts
   * @return headers
   */
  @Transactional(readOnly = true)
  public List<AccountLegacyHeader> headersOf(List<Long> accountIds) {
    return accountIds.isEmpty() ? List.of() : headers.findByAccountIdIn(accountIds);
  }

  private Account imported(Long accountId) {
    Account account =
        accounts
            .findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, accountId));
    if (account.getClassification().origin() != AccountOrigin.MIGRATED) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_MIGRATED", "Account " + account.getArn() + " was not imported from legacy");
    }
    return account;
  }

  private AccountLegacyHeader header(Long accountId) {
    return headers
        .findByAccountId(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("AccountLegacyHeader", accountId));
  }

  private AccountData data(LegacyAccount r) {
    Client client = clients.requireByCode(r.companyId(), r.clientCode());
    RiskProduct product = catalog.requireProduct(r.productCode());
    AccountData.ProductRef ref =
        new AccountData.ProductRef(
            product.getCode(),
            product.getLineCode(),
            product.getCoverTypeCode(),
            catalog.requireLine(product.getLineCode()).getRiskItemKind());
    LegacyAccount.Cover c = r.cover();
    return new AccountData(
        new AccountData.ClientRef(client.getId(), client.getCode(), client.getDisplayName()),
        ref,
        c.marketSegment(),
        c.sourceChannel(),
        c.insurerCode(),
        c.insurerBranch(),
        c.periodFrom(),
        c.periodTo(),
        false,
        1,
        c.currency(),
        c.arrangement(),
        c.mortgage(),
        AccountContact.NONE,
        List.of(RiskItemData.generic(r.riskDescription(), r.sumInsured(), null)));
  }
}
