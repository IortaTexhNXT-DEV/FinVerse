package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount.Details;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The maintained list "Post to Bank Account" (FRS.CSH.02.01.02; Appendix H): add, edit and
 * deactivate, one default per currency, every change in the audit trail.
 */
@Service
@Transactional
public class CashBankAccountService {

  private static final String ENTITY = "CashBankAccount";

  private final CashBankAccountRepository accounts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param accounts the list
   * @param audit audit trail
   */
  public CashBankAccountService(CashBankAccountRepository accounts, AuditTrailService audit) {
    this.accounts = accounts;
    this.audit = audit;
  }

  /**
   * The accounts of a company.
   *
   * @param companyId company
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<CashBankAccount> list(Long companyId) {
    return accounts.findByCompanyIdOrderByCurrencyAscNameAsc(companyId);
  }

  /**
   * Adds an account or changes it.
   *
   * @param companyId company
   * @param code code
   * @param details name, currency, GL account, default and active flags
   * @return the account
   */
  public CashBankAccount save(Long companyId, String code, Details details) {
    if (code == null || code.isBlank() || details.name() == null || details.name().isBlank()) {
      throw new BusinessRuleException("BANK_ACCOUNT_REQUIRED", "Enter the code and the name");
    }
    String key = code.strip().toUpperCase(Locale.ROOT);
    CashBankAccount account =
        accounts
            .findByCompanyIdAndCode(companyId, key)
            .orElseGet(() -> new CashBankAccount(companyId, key, details));
    String before = account.getId() == null ? "new" : describe(account);
    account.change(details);
    if (details.defaultForCurrency()) {
      accounts.findByCompanyIdOrderByCurrencyAscNameAsc(companyId).stream()
          .filter(a -> a.getCurrency().equals(details.currency()) && !a.getCode().equals(key))
          .forEach(CashBankAccount::notDefault);
    }
    CashBankAccount saved = accounts.save(account);
    audit.record(
        ENTITY, key, AuditAction.UPDATE, "Before: " + before + ". After: " + describe(saved));
    return saved;
  }

  /**
   * One account.
   *
   * @param id id
   * @return account
   */
  @Transactional(readOnly = true)
  public CashBankAccount get(Long id) {
    return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private static String describe(CashBankAccount a) {
    return a.getName()
        + ", "
        + a.getCurrency()
        + ", GL "
        + a.getGlAccountCode()
        + (a.isDefaultForCurrency() ? ", default" : "")
        + (a.isActive() ? "" : ", inactive");
  }
}
