package com.iortatechnxt.finverse.closing.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.budget.domain.BudgetStatus;
import com.iortatechnxt.finverse.budget.service.BudgetLineService;
import com.iortatechnxt.finverse.budget.service.BudgetService;
import com.iortatechnxt.finverse.closing.service.FxRevaluationService;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.finverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.finverse.consolidation.service.ConsolidationRunService;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyReconciliationService;
import com.iortatechnxt.finverse.consolidation.service.IntercompanyService;
import com.iortatechnxt.finverse.journal.service.SystemJournalService;
import com.iortatechnxt.finverse.organization.domain.CompanyRepository;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Runs the demo-profile data loader against the test database (twice, to prove idempotency). */
@IntegrationTest
class PlanningDemoDataIT {

  @Autowired private OrganizationService organization;
  @Autowired private PeriodService periods;
  @Autowired private SystemJournalService journals;
  @Autowired private IntercompanyService intercompany;
  @Autowired private IntercompanyReconciliationService reconciliation;
  @Autowired private BudgetService budgets;
  @Autowired private BudgetLineService budgetLines;
  @Autowired private FxRevaluationService revaluations;
  @Autowired private ConsolidationGroupService groups;
  @Autowired private ConsolidationRunService runs;
  @Autowired private UserDetailsService users;
  @Autowired private CompanyRepository companies;
  @Autowired private TestData data;

  @Test
  void loadsIdempotentDemoData() {
    PlanningDemoData loader =
        new PlanningDemoData(
            organization,
            periods,
            journals,
            intercompany,
            budgets,
            budgetLines,
            revaluations,
            groups,
            runs,
            users);
    loader.run(null);
    loader.run(null);

    Long fvi = data.company().getId();
    Long fvs = companies.findByCode("FVS").orElseThrow().getId();
    assertThat(intercompany.transactions(fvs)).hasSize(7);
    assertThat(budgets.approved(fvi, 2026)).isPresent();
    assertThat(budgets.list(fvi, 2026))
        .anyMatch(b -> b.getStatus() == BudgetStatus.APPROVED && b.getLines().size() == 13);
    assertThat(revaluations.list(fvi))
        .extracting(r -> r.getPeriodName())
        .contains("2026-07", "2026-08");

    ConsolidationGroup group = groups.getByCode("FVGRP");
    List<ConsolidationRun> list = runs.list(group.getId());
    assertThat(list).hasSize(1);
    assertThat(list.get(0).isBalanced()).isTrue();
    assertThat(reconciliation.reconcile(fvi, LocalDate.of(2026, 8, 31)))
        .anyMatch(l -> l.creditorCompanyId().equals(fvi) && l.matched());
  }
}
