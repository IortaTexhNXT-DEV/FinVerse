package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountRepository;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountStatus;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookRepository;
import com.iortatechnxt.brokerverse.payables.domain.NotificationFormat;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance of company bank accounts and their cheque books (maker-checker), and allocation of
 * cheque leaves to payments.
 */
@Service
@Transactional
public class BankAccountService {

  private static final String ENTITY = "BankAccount";
  private static final String CHEQUE_BOOK = "Cheque book";

  private final BankAccountRepository accounts;
  private final ChequeBookRepository books;
  private final BankAccountQueryService query;
  private final ChartOfAccountsService chart;
  private final CurrencyService currencies;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts bank account repository
   * @param books cheque book repository
   * @param query read service
   * @param chart chart of accounts
   * @param currencies currency service
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BankAccountService(
      BankAccountRepository accounts,
      ChequeBookRepository books,
      BankAccountQueryService query,
      ChartOfAccountsService chart,
      CurrencyService currencies,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.books = books;
    this.query = query;
    this.chart = chart;
    this.currencies = currencies;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a bank account (pending authorization).
   *
   * @param c values
   * @return account
   */
  public BankAccount create(BankAccountCommand c) {
    if (accounts.existsByCompanyIdAndCode(c.companyId(), c.code())) {
      throw new DuplicateResourceException(ENTITY, c.code());
    }
    BankAccount account = new BankAccount(c.companyId(), c.code(), c.currency());
    apply(account, c);
    BankAccount saved = accounts.save(account);
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created bank account " + c.name());
    return saved;
  }

  /**
   * Updates a bank account; it returns to pending authorization.
   *
   * @param id id
   * @param c values
   * @return account
   */
  public BankAccount update(Long id, BankAccountCommand c) {
    BankAccount account = query.get(id);
    account.setCurrency(c.currency());
    apply(account, c);
    account.markModified();
    audit.record(ENTITY, account.getCode(), AuditAction.UPDATE, "Updated bank account");
    return account;
  }

  /**
   * Authorizes a bank account (checker).
   *
   * @param id id
   * @return account
   */
  public BankAccount authorize(Long id) {
    BankAccount account = query.get(id);
    account.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, account.getCode(), AuditAction.AUTHORIZE, "Authorized bank account");
    return account;
  }

  /**
   * Registers a new cheque book; ranges of one account may not overlap.
   *
   * @param bankAccountId bank account
   * @param firstNo first leaf
   * @param lastNo last leaf
   * @param receivedOn date received
   * @return book
   */
  public ChequeBook addChequeBook(
      Long bankAccountId, long firstNo, long lastNo, LocalDate receivedOn) {
    BankAccount account = query.get(bankAccountId);
    boolean overlap =
        books.findByBankAccountIdOrderByFirstNo(bankAccountId).stream()
            .anyMatch(b -> b.overlaps(firstNo, lastNo));
    if (overlap) {
      throw new BusinessRuleException(
          "CHEQUE_RANGE_OVERLAP", "Cheque range overlaps an existing book of " + account.getCode());
    }
    ChequeBook book = books.save(new ChequeBook(bankAccountId, firstNo, lastNo, receivedOn));
    audit.record(
        ENTITY,
        account.getCode(),
        AuditAction.UPDATE,
        "Added cheque book " + firstNo + "-" + lastNo);
    return book;
  }

  /**
   * Asks to tag a bank account active or inactive (DIS 2.24.2); it applies on authorisation by
   * another user ({@link #authorize(Long)}).
   *
   * @param id bank account
   * @param status requested status
   * @return account (pending authorisation)
   */
  public BankAccount requestStatus(Long id, BankAccountStatus status) {
    BankAccount account = query.get(id);
    account.requestStatus(status);
    audit.record(ENTITY, account.getCode(), AuditAction.UPDATE, "Requested status " + status);
    return account;
  }

  /**
   * Edits the beginning check series of an unused cheque book (DIS 2.23.2); the new range may not
   * overlap another book of the account.
   *
   * @param bookId cheque book
   * @param firstNo new first leaf
   * @param lastNo new last leaf
   * @return book
   */
  public ChequeBook editChequeBook(Long bookId, long firstNo, long lastNo) {
    ChequeBook book =
        books
            .findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException(CHEQUE_BOOK, bookId));
    boolean overlap =
        books.findByBankAccountIdOrderByFirstNo(book.getBankAccountId()).stream()
            .filter(b -> !b.getId().equals(bookId))
            .anyMatch(b -> b.overlaps(firstNo, lastNo));
    if (overlap) {
      throw new BusinessRuleException(
          "CHEQUE_RANGE_OVERLAP", "Cheque range overlaps another book of the account");
    }
    String before = book.getFirstNo() + "-" + book.getLastNo();
    book.editRange(firstNo, lastNo, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        book.getBankAccountId(),
        AuditAction.UPDATE,
        "Edited cheque book " + before + " to " + firstNo + "-" + lastNo);
    return book;
  }

  /**
   * Withdraws a cheque book.
   *
   * @param bookId book
   * @return book
   */
  public ChequeBook cancelChequeBook(Long bookId) {
    ChequeBook book =
        books
            .findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException(CHEQUE_BOOK, bookId));
    book.cancel();
    audit.record(ENTITY, book.getBankAccountId(), AuditAction.UPDATE, "Cancelled cheque book");
    return book;
  }

  /**
   * Takes the next cheque leaf of a bank account (joins the payment transaction; the book row lock
   * serializes concurrent payments).
   *
   * @param bankAccountId bank account
   * @return cheque number
   */
  public String nextChequeNo(Long bankAccountId) {
    return books.lockActive(bankAccountId).stream()
        .findFirst()
        .map(ChequeBook::allocate)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_CHEQUE_BOOK",
                    "Bank account "
                        + query.get(bankAccountId).getCode()
                        + " has no active cheque book"));
  }

  private void apply(BankAccount account, BankAccountCommand c) {
    currencies.requireActive(c.currency());
    validateBankAccount(c.companyId(), c.glAccountCode(), c.currency());
    if (c.pdcClearingAccountCode() != null && !c.pdcClearingAccountCode().isBlank()) {
      GlAccount clearing = chart.getByCode(c.companyId(), c.pdcClearingAccountCode());
      if (!clearing.isPostable() || clearing.getAccountClass() != AccountClass.LIABILITY) {
        throw new BusinessRuleException(
            "INVALID_PDC_CLEARING_ACCOUNT",
            "PDC clearing account must be a postable liability account");
      }
    }
    query
        .findByGlAccount(c.companyId(), c.glAccountCode())
        .filter(other -> !other.getCode().equals(account.getCode()))
        .ifPresent(
            other -> {
              throw new BusinessRuleException(
                  "GL_ACCOUNT_IN_USE",
                  "GL account "
                      + c.glAccountCode()
                      + " is mapped to bank account "
                      + other.getCode());
            });
    account.setName(c.name());
    account.setBankPartyCode(blankToNull(c.bankPartyCode()));
    account.setBankName(c.bankName());
    account.setAccountNo(c.accountNo());
    account.setGlAccountCode(c.glAccountCode());
    account.setPdcClearingAccountCode(blankToNull(c.pdcClearingAccountCode()));
    account.setBranchId(c.branchId());
    account.setNotificationFormat(
        c.notificationFormat() == null ? NotificationFormat.FIXED_WIDTH : c.notificationFormat());
  }

  private void validateBankAccount(Long companyId, String glCode, String currency) {
    GlAccount gl = chart.getByCode(companyId, glCode);
    boolean bankCategory = gl.getCategory() != null && gl.getCategory().isBankCategory();
    if (!gl.isPostable() || !bankCategory) {
      throw new BusinessRuleException(
          "INVALID_BANK_GL_ACCOUNT",
          "GL account " + glCode + " must be a postable account of a bank or cash category");
    }
    if (!gl.acceptsCurrency(currency)) {
      throw new BusinessRuleException(
          "INVALID_BANK_GL_ACCOUNT", "GL account " + glCode + " does not accept " + currency);
    }
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
