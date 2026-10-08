package com.iortatechnxt.brokerverse.closing.seed;

import com.iortatechnxt.brokerverse.budget.service.BudgetLineService;
import com.iortatechnxt.brokerverse.budget.service.BudgetService;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Seed data of budgeting and FX revaluation (seed profile only, idempotent, runs after the
 * operational seed data): the approved FY2026 budget and posted July/August FX revaluations with
 * auto-reversal.
 */
@Component
@Profile("seed")
@Order(60)
public class PlanningSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(PlanningSeedData.class);
  private static final String PARENT = "FVI";
  private static final String MANAGER = "fmanager";
  private static final List<String> REVALUED_PERIODS = List.of("2026-07", "2026-08");
  private static final LocalDate REVALUATION_DATE = LocalDate.of(2026, 8, 31);

  private final OrganizationService organization;
  private final PeriodService periods;
  private final FxRevaluationService revaluations;
  private final SeedUsers users;
  private final SeedBudgetData budget;

  /**
   * Creates the runner.
   *
   * @param organization organization service
   * @param periods period service
   * @param budgets budget service
   * @param budgetLines budget line service
   * @param revaluations FX revaluation service
   * @param userDetails SIT/UAT users (maker and checker)
   */
  public PlanningSeedData(
      OrganizationService organization,
      PeriodService periods,
      BudgetService budgets,
      BudgetLineService budgetLines,
      FxRevaluationService revaluations,
      UserDetailsService userDetails) {
    this.organization = organization;
    this.periods = periods;
    this.revaluations = revaluations;
    this.users = new SeedUsers(userDetails);
    this.budget = new SeedBudgetData(budgets, budgetLines, users);
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> parent = company(PARENT);
    if (parent.isEmpty()) {
      LOG.info("Planning seed data skipped: seed company {} not found", PARENT);
      return;
    }
    Company fvi = parent.get();
    step("FY2026 budget", () -> budget.load(fvi));
    step("FX revaluation", () -> revalue(fvi));
    LOG.info("Planning and closing seed data ready");
  }

  /** Runs one step as the finance manager; a business rule failure is logged, not fatal. */
  private void step(String name, Runnable action) {
    try {
      users.as(
          MANAGER,
          () -> {
            action.run();
            return name;
          });
    } catch (BusinessRuleException ex) {
      LOG.warn("Planning seed data step '{}' skipped: {}", name, ex.getMessage());
    }
  }

  private void revalue(Company parent) {
    Long yearId = periods.yearContaining(parent.getId(), REVALUATION_DATE).getId();
    for (AccountingPeriod p : periods.listPeriods(yearId)) {
      if (REVALUED_PERIODS.contains(p.getName())) {
        revaluations.post(
            parent.getId(), p.getId(), true, FxRevaluationService.DEFAULT_GAIN_LOSS_ACCOUNT);
      }
    }
  }

  private Optional<Company> company(String code) {
    return organization.listCompanies().stream().filter(c -> c.getCode().equals(code)).findFirst();
  }
}
