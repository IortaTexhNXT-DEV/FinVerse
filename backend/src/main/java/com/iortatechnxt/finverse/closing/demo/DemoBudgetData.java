package com.iortatechnxt.finverse.closing.demo;

import com.iortatechnxt.finverse.budget.api.dto.BudgetLineRequest;
import com.iortatechnxt.finverse.budget.api.dto.CreateBudgetRequest;
import com.iortatechnxt.finverse.budget.domain.Budget;
import com.iortatechnxt.finverse.budget.domain.BudgetVersionType;
import com.iortatechnxt.finverse.budget.service.BudgetLineService;
import com.iortatechnxt.finverse.budget.service.BudgetService;
import com.iortatechnxt.finverse.budget.service.BudgetSpread;
import com.iortatechnxt.finverse.organization.domain.Company;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

/**
 * Approved FY2026 operating budget of the demo company: premium, investment and commission income
 * and the main expense accounts, spread by month (seasonal for premium-driven lines, even for fixed
 * costs), prepared by the accountant and approved by the finance manager.
 */
class DemoBudgetData {

  private static final int YEAR = 2026;

  /** Premium seasonality: renewals peak at quarter ends and in December. */
  private static final List<BigDecimal> SEASONAL =
      Stream.of(7, 7, 9, 8, 8, 9, 8, 8, 9, 8, 9, 10).map(BigDecimal::valueOf).toList();

  private final BudgetService budgets;
  private final BudgetLineService lines;
  private final DemoUsers users;

  DemoBudgetData(BudgetService budgets, BudgetLineService lines, DemoUsers users) {
    this.budgets = budgets;
    this.lines = lines;
    this.users = users;
  }

  /**
   * Creates and approves the budget unless the company already has a FY2026 budget.
   *
   * @param company demo company
   */
  void load(Company company) {
    if (!budgets.list(company.getId(), YEAR).isEmpty()) {
      return;
    }
    Budget budget =
        users.as(
            "accountant",
            () ->
                budgets.create(
                    new CreateBudgetRequest(
                        company.getId(),
                        YEAR,
                        BudgetVersionType.ORIGINAL,
                        "FY2026 Operating Budget",
                        null)));
    users.as("accountant", () -> lines.saveLines(budget.getId(), budgetLines()));
    users.as("accountant", () -> budgets.submit(budget.getId()));
    users.as("fmanager", () -> budgets.approve(budget.getId()));
  }

  private static List<BudgetLineRequest> budgetLines() {
    return List.of(
        seasonal("4100", null, "360000000"),
        even("4400", null, "12000000"),
        even("4501", null, "18000000"),
        seasonal("5100", null, "150000000"),
        seasonal("5400", null, "54000000"),
        even("5601", "FIN", "9600000"),
        even("5601", "UW", "12000000"),
        even("5601", "CLM", "8400000"),
        even("5601", "IT", "7200000"),
        even("5603", "FIN", "6000000"),
        even("5605", "FIN", "2400000"),
        seasonal("5609", "MKT", "3600000"),
        even("5610", "IT", "4800000"));
  }

  private static BudgetLineRequest seasonal(String account, String costCenter, String annual) {
    return new BudgetLineRequest(
        account, costCenter, BudgetSpread.weighted(new BigDecimal(annual), SEASONAL));
  }

  private static BudgetLineRequest even(String account, String costCenter, String annual) {
    return new BudgetLineRequest(account, costCenter, BudgetSpread.even(new BigDecimal(annual)));
  }
}
