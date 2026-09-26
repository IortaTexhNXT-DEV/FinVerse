package com.iortatechnxt.brokerverse.dashboard;

import static com.iortatechnxt.brokerverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.dashboard.service.BudgetWidget;
import com.iortatechnxt.brokerverse.dashboard.service.CashWidget;
import com.iortatechnxt.brokerverse.dashboard.service.ClaimsWidget;
import com.iortatechnxt.brokerverse.dashboard.service.CollectionsWidget;
import com.iortatechnxt.brokerverse.dashboard.service.DashboardService;
import com.iortatechnxt.brokerverse.dashboard.service.LabelledAmount;
import com.iortatechnxt.brokerverse.dashboard.service.LedgerDashboardService;
import com.iortatechnxt.brokerverse.dashboard.service.OperationsDashboardService;
import com.iortatechnxt.brokerverse.dashboard.service.PayablesWidget;
import com.iortatechnxt.brokerverse.dashboard.service.PremiumWidget;
import com.iortatechnxt.brokerverse.dashboard.service.TrendPoint;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Executive dashboard widgets: exact figures on a company with known postings, zeros on a company
 * without data, and consistency with the headline summary on the seed company.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExecutiveDashboardIT {

  private static final String PHP = "PHP";
  private static final String FIRE = "FIRE";
  private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);

  @Autowired private LedgerDashboardService ledger;
  @Autowired private OperationsDashboardService operations;
  @Autowired private DashboardService summary;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private TestCompanies companies;
  @Autowired private TestData data;
  @Autowired private AsUser as;

  private Long companyId;
  private Long emptyCompanyId;

  @BeforeAll
  void setUp() {
    companyId = companies.create("TDSH", PHP).getId();
    companies.openYear(companyId, 2025);
    companies.openYear(companyId, 2026);
    premium(LocalDate.of(2025, 3, 10), "400");
    premium(LocalDate.of(2025, 9, 5), "100");
    premium(LocalDate.of(2026, 3, 5), "1000");
    premium(LocalDate.of(2026, 9, 10), "250");
    companies.post(
        companyId,
        LocalDate.of(2026, 4, 15),
        PHP,
        List.of(
            line("5100", BalanceSide.DEBIT, "300", null, FIRE),
            line("1111", BalanceSide.CREDIT, "300")));
    companies.post(
        companyId,
        LocalDate.of(2026, 4, 15),
        PHP,
        List.of(
            line("5200", BalanceSide.DEBIT, "700", null, FIRE),
            line("2102", BalanceSide.CREDIT, "700", null, FIRE)));
    emptyCompanyId = companies.create("TDSE", PHP).getId();
  }

  private void premium(LocalDate date, String amount) {
    companies.post(
        companyId,
        date,
        PHP,
        List.of(
            line("1111", BalanceSide.DEBIT, amount),
            line("4100", BalanceSide.CREDIT, amount, null, FIRE)));
  }

  @Test
  void premiumIsComparedWithTheSamePeriodOfThePriorYear() {
    PremiumWidget p = ledger.premium(companyId, null, AS_OF);
    assertThat(p.yearStart()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(p.monthToDate()).isEqualByComparingTo("250");
    assertThat(p.monthToDatePriorYear()).isEqualByComparingTo("100");
    assertThat(p.yearToDate()).isEqualByComparingTo("1250");
    assertThat(p.yearToDatePriorYear()).isEqualByComparingTo("500");
    assertThat(p.monthly()).hasSize(9);
    assertThat(p.monthly().get(2))
        .isEqualTo(new TrendPoint("2026-03", new BigDecimal("1000.00"), new BigDecimal("400.00")));
    assertThat(p.monthly().get(0).current()).isEqualByComparingTo("0");
    Long headOffice = companies.headOffice(companyId);
    assertThat(ledger.premium(companyId, headOffice, AS_OF).yearToDate())
        .isEqualByComparingTo("1250");
  }

  @Test
  void claimsPaidAndOutstandingComeFromTheClaimAccounts() {
    ClaimsWidget c = ledger.claims(companyId, null, AS_OF);
    assertThat(c.paidYearToDate()).isEqualByComparingTo("300");
    assertThat(c.paidMonthToDate()).isEqualByComparingTo("0");
    assertThat(c.outstanding()).isEqualByComparingTo("700");
    assertThat(c.monthly().get(3).current()).isEqualByComparingTo("300");
  }

  @Test
  void cashPositionListsTheBankAccountsAndTheMonthEndTrend() {
    CashWidget cash = ledger.cash(companyId, null, AS_OF);
    assertThat(cash.total()).isEqualByComparingTo("1450");
    assertThat(cash.accounts())
        .singleElement()
        .satisfies(a -> assertThat(a.label()).startsWith("1111 "));
    assertThat(cash.monthly()).hasSize(9);
    assertThat(cash.monthly().get(0).amount()).isEqualByComparingTo("500");
    assertThat(cash.monthly().get(8).amount()).isEqualByComparingTo("1450");
  }

  @Test
  void budgetWithoutApprovedVersionShowsActualsOnly() {
    BudgetWidget b = operations.budget(companyId, AS_OF);
    assertThat(b.fiscalYear()).isEqualTo(2026);
    assertThat(b.budgetVersion()).isNull();
    assertThat(b.annualBudget()).isEqualByComparingTo("0");
    assertThat(b.actualToDate()).isEqualByComparingTo("1000");
    assertThat(b.utilizationPct()).isNull();
    assertThat(b.lines()).extracting(l -> l.account().substring(0, 4)).contains("5100", "5200");
  }

  @Test
  void aCompanyWithoutDataGetsZerosEverywhere() {
    PremiumWidget p = ledger.premium(emptyCompanyId, null, AS_OF);
    assertThat(p.yearToDate()).isEqualByComparingTo("0");
    assertThat(p.monthly()).allSatisfy(m -> assertThat(m.priorYear()).isEqualByComparingTo("0"));
    assertThat(ledger.claims(emptyCompanyId, null, AS_OF).outstanding()).isEqualByComparingTo("0");
    CashWidget cash = ledger.cash(emptyCompanyId, null, AS_OF);
    assertThat(cash.accounts()).isEmpty();
    assertThat(cash.total()).isEqualByComparingTo("0");
    PayablesWidget payables = ledger.payables(emptyCompanyId, null, AS_OF);
    assertThat(payables.total()).isEqualByComparingTo("0");
    assertThat(payables.openItems()).isZero();
    CollectionsWidget collections = operations.collections(emptyCompanyId, null, AS_OF);
    assertThat(collections.monthly()).isEmpty();
    assertThat(collections.ageing())
        .hasSize(5)
        .allSatisfy(b -> assertThat(b.amount()).isEqualByComparingTo("0"));
    BudgetWidget budget = operations.budget(emptyCompanyId, AS_OF);
    assertThat(budget.budgetVersion()).isNull();
    assertThat(budget.lines()).isEmpty();
    assertThat(as.run("checker", () -> operations.workload(emptyCompanyId)).openAlerts()).isZero();
  }

  @Test
  void seedCompanyWidgetsAreConsistentWithTheHeadlineSummary() {
    Long seed = data.company().getId();
    LocalDate today = summary.summary(seed, null).asOf();
    assertThat(ledger.cash(seed, null, null).total())
        .isEqualByComparingTo(summary.summary(seed, null).cashPosition());

    CollectionsWidget c = operations.collections(seed, null, today);
    assertThat(c.receivables())
        .isEqualByComparingTo(
            c.ageing().stream()
                .map(LabelledAmount::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    assertThat(c.ageingSlots()).isEqualTo("30/60/90/120");

    PayablesWidget p = ledger.payables(seed, null, today);
    assertThat(p.total()).isGreaterThanOrEqualTo(p.dueIn30Days().add(p.overdue()));
    assertThat(p.dueIn30Days()).isGreaterThanOrEqualTo(p.dueIn7Days());

    var workload = as.run("checker", () -> operations.workload(seed));
    assertThat(workload.pendingApprovals())
        .isEqualTo(as.run("checker", () -> inbox.counts(seed)).total());
    assertThat(ledger.premium(seed, null, today).monthly()).isNotEmpty();
  }
}
