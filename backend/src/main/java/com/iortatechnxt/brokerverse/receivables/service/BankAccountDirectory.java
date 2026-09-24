package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identifies banks by their GL bank account: a postable, active account of a bank/cash category
 * (e.g. 1111 "Cash in Bank - BDO Current Account"). The receivables module keeps no bank master of
 * its own, so any bank account in the chart of accounts can receive collections.
 */
@Service
@Transactional(readOnly = true)
public class BankAccountDirectory {

  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;

  /**
   * Creates the directory.
   *
   * @param accounts chart of accounts
   * @param organization organization service
   */
  public BankAccountDirectory(ChartOfAccountsService accounts, OrganizationService organization) {
    this.accounts = accounts;
    this.organization = organization;
  }

  /**
   * Lists the bank and cash accounts of a company.
   *
   * @param companyId company
   * @return bank accounts ordered by code
   */
  public List<BankAccount> list(Long companyId) {
    String base = organization.getCompany(companyId).getBaseCurrency();
    return accounts.list(companyId).stream()
        .filter(BankAccountDirectory::isBankAccount)
        .map(a -> BankAccount.of(a, base))
        .toList();
  }

  /**
   * Returns a bank account that can receive money in a currency, or fails.
   *
   * @param companyId company
   * @param code GL account code
   * @param currency currency of the money
   * @return bank account
   */
  public BankAccount require(Long companyId, String code, String currency) {
    GlAccount account = accounts.getByCode(companyId, code);
    if (!isBankAccount(account)) {
      throw new BusinessRuleException(
          "NOT_A_BANK_ACCOUNT", "Account " + code + " is not an active bank or cash account");
    }
    if (currency != null && !account.acceptsCurrency(currency)) {
      throw new BusinessRuleException(
          "BANK_CURRENCY", "Bank account " + code + " does not accept " + currency);
    }
    return BankAccount.of(account, organization.getCompany(companyId).getBaseCurrency());
  }

  /**
   * Returns an active postable income account (other receipts), or fails.
   *
   * @param companyId company
   * @param code GL account code
   */
  public void requireIncomeAccount(Long companyId, String code) {
    GlAccount account = accounts.getByCode(companyId, code);
    if (account.getAccountClass() != AccountClass.INCOME
        || !account.isPostable()
        || !account.isActive()) {
      throw new BusinessRuleException(
          "NOT_AN_INCOME_ACCOUNT", "Account " + code + " is not a postable income account");
    }
  }

  private static boolean isBankAccount(GlAccount a) {
    return a.isPostable()
        && a.isActive()
        && a.getCategory() != null
        && a.getCategory().isBankCategory();
  }

  /**
   * Bank (GL) account.
   *
   * @param id GL account id
   * @param code GL account code
   * @param name account name
   * @param currency operating currency (the single allowed currency, else the base currency)
   */
  public record BankAccount(Long id, String code, String name, String currency) {

    static BankAccount of(GlAccount a, String baseCurrency) {
      String ccy =
          a.getAllowedCurrencies().size() == 1
              ? a.getAllowedCurrencies().iterator().next()
              : baseCurrency;
      return new BankAccount(a.getId(), a.getCode(), a.getName(), ccy);
    }
  }
}
