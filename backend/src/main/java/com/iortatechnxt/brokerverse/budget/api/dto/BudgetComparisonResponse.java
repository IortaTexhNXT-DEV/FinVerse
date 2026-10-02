package com.iortatechnxt.brokerverse.budget.api.dto;

import com.iortatechnxt.brokerverse.budget.service.BudgetMonitoringService.BudgetComparison;
import java.util.List;

/**
 * Budget vs actual result.
 *
 * @param fiscalYear fiscal year
 * @param periodNo selected month (1..12)
 * @param budgetId approved budget used (null when none)
 * @param budgetVersion its version number
 * @param lines lines
 */
public record BudgetComparisonResponse(
    int fiscalYear,
    int periodNo,
    Long budgetId,
    Integer budgetVersion,
    List<VarianceLineResponse> lines) {

  /**
   * Maps a comparison.
   *
   * @param c comparison
   * @return response
   */
  public static BudgetComparisonResponse from(BudgetComparison c) {
    return new BudgetComparisonResponse(
        c.fiscalYear(),
        c.periodNo(),
        c.budgetId(),
        c.budgetVersion(),
        c.lines().stream().map(VarianceLineResponse::from).toList());
  }
}
