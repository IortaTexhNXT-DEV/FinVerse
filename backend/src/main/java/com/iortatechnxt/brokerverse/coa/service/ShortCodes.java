package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/** Short codes of GL accounts (FRBS 2.3.3): unique per company, usable in place of the code. */
@Component
public class ShortCodes {

  private final GlAccountRepository accounts;

  /**
   * Creates the helper.
   *
   * @param accounts account repository
   */
  public ShortCodes(GlAccountRepository accounts) {
    this.accounts = accounts;
  }

  /**
   * Finds an account by its code or, failing that, its short code.
   *
   * @param companyId company
   * @param key account code or short code
   * @return account
   */
  public GlAccount lookup(Long companyId, String key) {
    String k = key.trim();
    return accounts
        .findByCompanyIdAndCode(companyId, k)
        .or(() -> accounts.findByCompanyIdAndShortNameIgnoreCase(companyId, k))
        .orElseThrow(() -> new ResourceNotFoundException("GL account", k));
  }

  /**
   * The short code to store on an account: trimmed, blank as none, refused when another account of
   * the company already uses it.
   *
   * @param account account being saved
   * @param requested requested short code
   * @return short code, or null
   */
  public String checked(GlAccount account, String requested) {
    if (requested == null || requested.isBlank()) {
      return null;
    }
    String shortName = requested.trim();
    accounts
        .findByCompanyIdAndShortNameIgnoreCase(account.getCompanyId(), shortName)
        .filter(other -> !other.getCode().equals(account.getCode()))
        .ifPresent(
            other -> {
              throw new BusinessRuleException(
                  "SHORT_CODE_TAKEN",
                  "Short code " + shortName + " is already used by account " + other.getCode());
            });
    return shortName;
  }
}
