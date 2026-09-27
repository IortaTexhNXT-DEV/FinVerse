package com.iortatechnxt.brokerverse.budget.report;

import com.iortatechnxt.brokerverse.budget.service.BudgetMonitoringService.BudgetComparison;

/** Shared texts of the budget reports. */
final class BudgetReports {

  private BudgetReports() {}

  /**
   * Footnote naming the budget version compared.
   *
   * @param c comparison
   * @return note
   */
  static String basisNote(BudgetComparison c) {
    return c.budgetVersion() == null
        ? "No approved budget exists for fiscal year " + c.fiscalYear() + "; budget shown as zero."
        : "Approved budget version "
            + c.budgetVersion()
            + " of fiscal year "
            + c.fiscalYear()
            + ", month "
            + c.periodNo()
            + ".";
  }
}
