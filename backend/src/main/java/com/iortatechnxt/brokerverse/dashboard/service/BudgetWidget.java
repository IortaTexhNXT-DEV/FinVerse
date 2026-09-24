package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Expense budget against actual of the approved budget of the current fiscal year.
 *
 * @param fiscalYear fiscal year
 * @param budgetVersion approved version used, null when no budget is approved
 * @param annualBudget annual expense budget
 * @param budgetToDate expense budget up to the current month
 * @param actualToDate actual expenses of the fiscal year
 * @param utilizationPct actual in percent of the annual budget (null without budget)
 * @param lines largest expense accounts: budget to date and actual
 */
public record BudgetWidget(
    int fiscalYear,
    Integer budgetVersion,
    BigDecimal annualBudget,
    BigDecimal budgetToDate,
    BigDecimal actualToDate,
    BigDecimal utilizationPct,
    List<BudgetAccountFigures> lines) {

  /** Canonical constructor copying the list. */
  public BudgetWidget {
    lines = List.copyOf(lines);
  }
}
