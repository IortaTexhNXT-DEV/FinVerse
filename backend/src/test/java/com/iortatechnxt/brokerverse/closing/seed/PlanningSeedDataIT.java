package com.iortatechnxt.brokerverse.closing.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.budget.domain.BudgetStatus;
import com.iortatechnxt.brokerverse.budget.service.BudgetLineService;
import com.iortatechnxt.brokerverse.budget.service.BudgetService;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Runs the seed-profile data loader against the test database (twice, to prove idempotency). */
@IntegrationTest
class PlanningSeedDataIT {

  @Autowired private OrganizationService organization;
  @Autowired private PeriodService periods;
  @Autowired private BudgetService budgets;
  @Autowired private BudgetLineService budgetLines;
  @Autowired private FxRevaluationService revaluations;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;

  @Test
  void loadsIdempotentSeedData() {
    PlanningSeedData loader =
        new PlanningSeedData(organization, periods, budgets, budgetLines, revaluations, users);
    loader.run(null);
    loader.run(null);

    Long fvi = data.company().getId();
    assertThat(budgets.approved(fvi, 2026)).isPresent();
    assertThat(budgets.list(fvi, 2026))
        .anyMatch(b -> b.getStatus() == BudgetStatus.APPROVED && b.getLines().size() == 13);
    assertThat(revaluations.list(fvi))
        .extracting(r -> r.getPeriodName())
        .contains("2026-07", "2026-08");
  }
}
