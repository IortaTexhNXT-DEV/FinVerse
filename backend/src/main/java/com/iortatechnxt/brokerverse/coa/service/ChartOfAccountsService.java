package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.api.dto.GlAccountRequest;
import com.iortatechnxt.brokerverse.coa.api.dto.GlCategoryRequest;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.coa.domain.GlCategory;
import com.iortatechnxt.brokerverse.coa.domain.GlCategoryRepository;
import com.iortatechnxt.brokerverse.coa.domain.NegativeBalancePolicy;
import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the multi-tier chart of accounts and GL categories.
 *
 * <p>Rules enforced:
 *
 * <ul>
 *   <li>Accounts are never deleted; they can be frozen or closed.
 *   <li>A child must be exactly one tier below its parent and share its class.
 *   <li>Adding a child turns the parent into a heading (non postable); this is refused when the
 *       parent already has postings.
 *   <li>Every change requires authorization by a different user.
 * </ul>
 */
@Service
@Transactional
public class ChartOfAccountsService {

  private static final String ACCOUNT = "GL account";
  private static final String ENTITY = "GlAccount";

  private final GlAccountRepository accounts;
  private final GlCategoryRepository categories;
  private final AccountUsageChecker usageChecker;
  private final CoaNumberingService numbering;
  private final ShortCodes shortCodes;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts account repository
   * @param categories category repository
   * @param usageChecker ledger usage port
   * @param numbering system-generated account numbers
   * @param shortCodes short codes
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ChartOfAccountsService(
      GlAccountRepository accounts,
      GlCategoryRepository categories,
      AccountUsageChecker usageChecker,
      CoaNumberingService numbering,
      ShortCodes shortCodes,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.categories = categories;
    this.usageChecker = usageChecker;
    this.numbering = numbering;
    this.shortCodes = shortCodes;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the chart of accounts of a company.
   *
   * @param companyId company
   * @return accounts ordered by code
   */
  @Transactional(readOnly = true)
  public List<GlAccount> list(Long companyId) {
    return accounts.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Searches accounts by code prefix, name or short code (FRBS 2.3.3).
   *
   * @param companyId company
   * @param term search term
   * @return matching accounts
   */
  @Transactional(readOnly = true)
  public List<GlAccount> search(Long companyId, String term) {
    return accounts.search(companyId, term.trim());
  }

  /**
   * Gets an account by id.
   *
   * @param id id
   * @return account
   */
  @Transactional(readOnly = true)
  public GlAccount get(Long id) {
    return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ACCOUNT, id));
  }

  /**
   * Gets an account by company and code.
   *
   * @param companyId company
   * @param code account code
   * @return account
   */
  @Transactional(readOnly = true)
  public GlAccount getByCode(Long companyId, String code) {
    return accounts
        .findByCompanyIdAndCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT, code));
  }

  /**
   * Finds an account by its code or, failing that, its short code (FRBS 2.3.3, 2.8.1).
   *
   * @param companyId company
   * @param key account code or short code
   * @return account
   */
  @Transactional(readOnly = true)
  public GlAccount lookup(Long companyId, String key) {
    return shortCodes.lookup(companyId, key);
  }

  /**
   * Creates an account (pending authorization). A blank code is generated from the numbering scheme
   * of the parent (FRBS 2.3.2).
   *
   * @param request request
   * @return created account
   */
  public GlAccount create(GlAccountRequest request) {
    String code =
        request.code() == null || request.code().isBlank()
            ? numbering.proposedCode(request.companyId(), request.parentCode())
            : request.code().trim();
    if (accounts.existsByCompanyIdAndCode(request.companyId(), code)) {
      throw new DuplicateResourceException(ACCOUNT, code);
    }
    GlAccount account =
        new GlAccount(
            request.companyId(),
            code,
            request.name(),
            request.accountClass(),
            request.level(),
            request.openedOn());
    attachParent(account, request.parentCode());
    apply(account, request);
    GlAccount saved = accounts.save(account);
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created account " + saved.getName());
    return saved;
  }

  /**
   * Updates an account; it returns to pending authorization.
   *
   * @param id id
   * @param request request
   * @return updated account
   */
  public GlAccount update(Long id, GlAccountRequest request) {
    GlAccount account = get(id);
    account.setName(request.name());
    apply(account, request);
    account.markModified();
    audit.record(
        ENTITY, account.getCode(), AuditAction.UPDATE, "Updated account " + account.getName());
    return account;
  }

  /**
   * Authorizes a pending account (checker).
   *
   * @param id id
   * @return account
   */
  public GlAccount authorize(Long id) {
    GlAccount account = get(id);
    account.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, account.getCode(), AuditAction.AUTHORIZE, "Authorized account");
    return account;
  }

  /**
   * Freezes an account for posting.
   *
   * @param id id
   * @param reason reason
   * @return account
   */
  public GlAccount freeze(Long id, String reason) {
    GlAccount account = get(id);
    account.freeze(reason);
    audit.record(ENTITY, account.getCode(), AuditAction.UPDATE, "Frozen: " + reason);
    return account;
  }

  /**
   * Removes a posting freeze.
   *
   * @param id id
   * @return account
   */
  public GlAccount unfreeze(Long id) {
    GlAccount account = get(id);
    account.unfreeze();
    audit.record(ENTITY, account.getCode(), AuditAction.UPDATE, "Unfrozen");
    return account;
  }

  /**
   * Closes an account from today (GL Closure).
   *
   * @param id id
   * @return account
   */
  public GlAccount close(Long id) {
    GlAccount account = get(id);
    LocalDate today = LocalDate.now(clock);
    account.close(today);
    audit.record(ENTITY, account.getCode(), AuditAction.CLOSE, "Closed from " + today);
    return account;
  }

  /**
   * Lists GL categories.
   *
   * @return categories
   */
  @Transactional(readOnly = true)
  public List<GlCategory> listCategories() {
    return categories.findAllByOrderByCode();
  }

  /**
   * Creates a GL category.
   *
   * @param request request
   * @return category
   */
  public GlCategory createCategory(GlCategoryRequest request) {
    if (categories.findByCode(request.code()).isPresent()) {
      throw new DuplicateResourceException("GL category", request.code());
    }
    GlCategory saved =
        categories.save(
            new GlCategory(
                request.code(), request.name(), request.accountClass(), request.bankCategory()));
    audit.record("GlCategory", saved.getCode(), AuditAction.CREATE, "Created category");
    return saved;
  }

  private void attachParent(GlAccount account, String parentCode) {
    if (parentCode == null || parentCode.isBlank()) {
      if (account.getLevel() != AccountLevel.GROUP && account.getLevel() != AccountLevel.MAIN) {
        throw new BusinessRuleException(
            "PARENT_REQUIRED", "Sub and Micro GL accounts require a parent account");
      }
      return;
    }
    GlAccount parent = getByCode(account.getCompanyId(), parentCode);
    validateHierarchy(parent, account);
    if (parent.isPostable()) {
      if (usageChecker.hasPostings(parent.getId())) {
        throw new BusinessRuleException(
            "PARENT_HAS_POSTINGS",
            "Account " + parent.getCode() + " already has postings and cannot become a heading");
      }
      parent.setPostable(false);
    }
    account.setParent(parent);
  }

  private static void validateHierarchy(GlAccount parent, GlAccount child) {
    if (parent.getAccountClass() != child.getAccountClass()) {
      throw new BusinessRuleException(
          "CLASS_MISMATCH", "Child account class must match parent " + parent.getCode());
    }
    boolean validTier =
        parent.getLevel() == AccountLevel.GROUP
            ? child.getLevel() == AccountLevel.GROUP || child.getLevel() == AccountLevel.MAIN
            : child.getLevel().ordinal() == parent.getLevel().ordinal() + 1;
    if (!validTier) {
      throw new BusinessRuleException(
          "INVALID_TIER",
          "A " + child.getLevel() + " account cannot be placed under a " + parent.getLevel());
    }
  }

  private void apply(GlAccount account, GlAccountRequest request) {
    account.setShortName(shortCodes.checked(account, request.shortName()));
    account.setNegativeBalancePolicy(
        request.negativeBalancePolicy() == null
            ? NegativeBalancePolicy.ALLOW
            : request.negativeBalancePolicy());
    account.setCategory(
        request.categoryCode() == null || request.categoryCode().isBlank()
            ? null
            : categories
                .findByCode(request.categoryCode())
                .orElseThrow(
                    () -> new ResourceNotFoundException("GL category", request.categoryCode())));
    account.setControlAccount(request.controlAccount());
    account.setSubLedgerType(
        request.subLedgerType() == null ? SubLedgerType.NONE : request.subLedgerType());
    account.setAllowManualPosting(request.allowManualPosting());
    account.setCostCenterRequired(request.costCenterRequired());
    account.setBusinessLineRequired(request.businessLineRequired());
    account.setRevaluationRequired(request.revaluationRequired());
    account.setReconcilable(request.reconcilable());
    account.setInterBranch(request.interBranch());
    account.setContraAccountCode(request.contraAccountCode());
    account.setReportGroup(request.reportGroup());
    account.replaceAllowedCurrencies(nullSafe(request.allowedCurrencies()));
    account.replaceAllowedBranches(nullSafe(request.allowedBranchIds()));
    account.replaceAllowedRoles(nullSafe(request.allowedRoleCodes()));
  }

  private static <T> Set<T> nullSafe(Set<T> values) {
    return values == null ? Set.of() : values;
  }
}
