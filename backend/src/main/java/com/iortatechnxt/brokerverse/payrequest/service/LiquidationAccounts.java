package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationAccount;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationAccountRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The GL accounts of the account roles of event {@code PRQ_CA_LIQUIDATION}: configuration kept by
 * Comptrollership until BDOI gives the real accounts (AQ02, OQ07).
 */
@Service
@Transactional
public class LiquidationAccounts {

  /** Account roles of the event, in form order. */
  public static final List<String> ROLES =
      List.of("PER_DIEM", "REPRESENTATION", "TRANSPORT", "LODGING", "OTHER", "CASH");

  private static final String ENTITY = "LiquidationAccount";

  private final LiquidationAccountRepository accounts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param accounts accounts of the event roles
   * @param audit audit trail
   */
  public LiquidationAccounts(LiquidationAccountRepository accounts, AuditTrailService audit) {
    this.accounts = accounts;
    this.audit = audit;
  }

  /**
   * The accounts of the event roles.
   *
   * @param companyId company
   * @return accounts by role
   */
  @Transactional(readOnly = true)
  public List<LiquidationAccount> accounts(Long companyId) {
    return accounts.findByCompanyIdOrderByAccountRole(companyId);
  }

  /**
   * Sets the account of an event role (Comptrollership configuration, AQ02).
   *
   * @param companyId company
   * @param role role
   * @param accountCode GL account
   * @return the configuration
   */
  public LiquidationAccount assign(Long companyId, String role, String accountCode) {
    if (!ROLES.contains(role) || accountCode == null || accountCode.isBlank()) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_ROLE", "Give one of the roles " + ROLES + " and an account code");
    }
    LiquidationAccount account =
        accounts
            .findByCompanyIdAndAccountRole(companyId, role)
            .orElseGet(
                () -> accounts.save(new LiquidationAccount(companyId, role, accountCode.strip())));
    account.changeAccount(accountCode.strip());
    audit.record(ENTITY, role, AuditAction.UPDATE, "Account of " + role + ": " + accountCode);
    return account;
  }

  /**
   * The account of every role the amounts of a liquidation need.
   *
   * @param companyId company
   * @param amounts amounts of the event by component
   * @return account code by role
   */
  Map<String, String> forAmounts(Long companyId, Map<String, BigDecimal> amounts) {
    Map<String, String> roles = new LinkedHashMap<>();
    for (LiquidationAccount a : accounts.findByCompanyIdOrderByAccountRole(companyId)) {
      roles.put(a.getAccountRole(), a.getAccountCode());
    }
    List<String> missing =
        ROLES.stream()
            .filter(r -> amounts.containsKey("CASH".equals(r) ? "CASH_RETURNED" : r))
            .filter(r -> !roles.containsKey(r))
            .toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "PRQ_LIQUIDATION_ACCOUNT_MISSING",
          "Comptrollership has not set the account of " + missing + " for liquidations (AQ02)");
    }
    return roles;
  }
}
