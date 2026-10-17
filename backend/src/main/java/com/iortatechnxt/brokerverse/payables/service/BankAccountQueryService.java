package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountRepository;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the company bank account master for every module (payments, receipts, PDC
 * registers, bank reconciliation, cash position).
 *
 * <p>Usage from another module (e.g. receivables):
 *
 * <pre>{@code
 * BankAccount bank = bankAccounts.requireActive(request.bankAccountId());
 * publisher.publish(new BusinessEvent("PREMIUM_RECEIPT", ..., Map.of("BANK", bank.getGlAccountCode())));
 * }</pre>
 *
 * <ul>
 *   <li>{@link BankAccount#getGlAccountCode()} is the value for the {@code BANK} account role of
 *       receipt and payment events.
 *   <li>{@link BankAccount#getCurrency()} is the only currency the account accepts.
 *   <li>Only {@code ACTIVE} (authorized) accounts may be used in transactions; {@link
 *       #requireActive(Long)} enforces it.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class BankAccountQueryService {

  private static final String BANK_ACCOUNT = "Bank account";

  private final BankAccountRepository accounts;
  private final ChequeBookRepository books;

  /**
   * Creates the service.
   *
   * @param accounts bank account repository
   * @param books cheque book repository
   */
  public BankAccountQueryService(BankAccountRepository accounts, ChequeBookRepository books) {
    this.accounts = accounts;
    this.books = books;
  }

  /**
   * Lists the bank accounts of a company (all statuses).
   *
   * @param companyId company
   * @return accounts ordered by code
   */
  public List<BankAccount> list(Long companyId) {
    return accounts.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Lists the authorized bank accounts of a company, optionally in one currency.
   *
   * @param companyId company
   * @param currency currency filter, null for all
   * @return active accounts
   */
  public List<BankAccount> listActive(Long companyId, String currency) {
    return list(companyId).stream()
        .filter(BankAccount::isActive)
        .filter(a -> currency == null || currency.equals(a.getCurrency()))
        .toList();
  }

  /**
   * Gets a bank account.
   *
   * @param id id
   * @return account
   */
  public BankAccount get(Long id) {
    return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException(BANK_ACCOUNT, id));
  }

  /**
   * Gets a bank account by code.
   *
   * @param companyId company
   * @param code code
   * @return account
   */
  public BankAccount getByCode(Long companyId, String code) {
    return accounts
        .findByCompanyIdAndCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException(BANK_ACCOUNT, code));
  }

  /**
   * Returns an authorized bank account or fails.
   *
   * @param id id
   * @return account
   */
  public BankAccount requireActive(Long id) {
    BankAccount account = get(id);
    if (!account.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_BANK_ACCOUNT", "Bank account " + account.getCode() + " is not active");
    }
    return account;
  }

  /**
   * Finds the bank account mapped to a GL account (e.g. to label ledger lines).
   *
   * @param companyId company
   * @param glAccountCode GL account code
   * @return account if mapped
   */
  public Optional<BankAccount> findByGlAccount(Long companyId, String glAccountCode) {
    return accounts.findByCompanyIdAndGlAccountCode(companyId, glAccountCode);
  }

  /**
   * Lists the cheque books of a bank account.
   *
   * @param bankAccountId bank account
   * @return books, lowest numbers first
   */
  public List<ChequeBook> chequeBooks(Long bankAccountId) {
    return books.findByBankAccountIdOrderByFirstNo(bankAccountId);
  }
}
