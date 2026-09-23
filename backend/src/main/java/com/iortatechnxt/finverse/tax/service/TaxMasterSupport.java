package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/** Validations shared by the tax master data services. */
@Component
public class TaxMasterSupport {

  private final ChartOfAccountsService chart;

  /**
   * Creates the helper.
   *
   * @param chart chart of accounts
   */
  public TaxMasterSupport(ChartOfAccountsService chart) {
    this.chart = chart;
  }

  /**
   * Requires a postable GL account.
   *
   * @param companyId company
   * @param code account code
   * @param purpose what the account is for (message)
   */
  public void requirePostable(Long companyId, String code, String purpose) {
    GlAccount account = chart.getByCode(companyId, code);
    if (!account.isPostable()) {
      throw new BusinessRuleException(
          "INVALID_TAX_ACCOUNT", purpose + " account " + code + " must be a postable account");
    }
  }

  /**
   * Trims a text and turns blanks into null.
   *
   * @param s text
   * @return trimmed text or null
   */
  public static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
