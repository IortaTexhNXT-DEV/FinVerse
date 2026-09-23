package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import com.iortatechnxt.finverse.dimension.service.DimensionService;
import org.springframework.stereotype.Component;

/**
 * Early validation of the expense account and cost centre typed on supplier invoice lines and petty
 * cash vouchers, so users get a precise message at capture time rather than at posting.
 */
@Component
public class ExpenseAccountValidator {

  private final ChartOfAccountsService chart;
  private final DimensionService dimensions;

  /**
   * Creates the validator.
   *
   * @param chart chart of accounts
   * @param dimensions dimensions
   */
  public ExpenseAccountValidator(ChartOfAccountsService chart, DimensionService dimensions) {
    this.chart = chart;
    this.dimensions = dimensions;
  }

  /**
   * Checks an expense line.
   *
   * @param companyId company
   * @param accountCode GL account
   * @param costCenter cost centre, may be null
   */
  public void validate(Long companyId, String accountCode, String costCenter) {
    GlAccount account = chart.getByCode(companyId, accountCode);
    if (!account.isActive() || !account.isPostable() || account.isControlAccount()) {
      throw new BusinessRuleException(
          "INVALID_EXPENSE_ACCOUNT",
          "Account " + accountCode + " must be an active, postable, non-control account");
    }
    if (account.isCostCenterRequired() && (costCenter == null || costCenter.isBlank())) {
      throw new BusinessRuleException(
          "COST_CENTER_REQUIRED", "Cost centre is mandatory for account " + accountCode);
    }
    dimensions.validateOptional(companyId, DimensionType.COST_CENTER, costCenter);
  }
}
