package com.iortatechnxt.brokerverse.budget.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validated values of a budget line.
 *
 * @param accountId GL account
 * @param accountCode GL account code
 * @param costCenter cost centre code, or null for the whole account
 * @param months twelve monthly amounts (natural sign)
 */
public record BudgetLineValues(
    Long accountId, String accountCode, String costCenter, List<BigDecimal> months) {

  /** Canonical constructor copying the month list. */
  public BudgetLineValues {
    months = List.copyOf(months);
  }
}
