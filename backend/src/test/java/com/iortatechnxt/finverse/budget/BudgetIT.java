package com.iortatechnxt.finverse.budget;

import static com.iortatechnxt.finverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.budget.api.dto.BudgetLineRequest;
import com.iortatechnxt.finverse.budget.api.dto.CreateBudgetRequest;
import com.iortatechnxt.finverse.budget.domain.Budget;
import com.iortatechnxt.finverse.budget.domain.BudgetStatus;
import com.iortatechnxt.finverse.budget.domain.BudgetVersionType;
import com.iortatechnxt.finverse.budget.service.BudgetLineService;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService;
import com.iortatechnxt.finverse.budget.service.BudgetMonitoringService.BudgetComparison;
import com.iortatechnxt.finverse.budget.service.BudgetService;
import com.iortatechnxt.finverse.budget.service.VarianceLine;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestCompanies;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BudgetIT {

  private static final String PHP = "PHP";
  private static final LocalDate FEB = LocalDate.of(2026, 2, 15);

  @Autowired private BudgetService budgets;
  @Autowired private BudgetLineService lines;
  @Autowired private BudgetMonitoringService monitoring;
  @Autowired private ReportService reports;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser asUser;

  private Long companyId;
  private Budget approved;

  @BeforeAll
  void setUp() {
    companyId = companies.create("TBUD", PHP).getId();
    companies.openYear(companyId, 2025);
    companies.openYear(companyId, 2026);
    expense(LocalDate.of(2026, 1, 20), "90000");
    expense(LocalDate.of(2026, 2, 10), "110000");
    companies.post(
        companyId,
        LocalDate.of(2026, 1, 25),
        PHP,
        List.of(
            line("1111", BalanceSide.DEBIT, "50000"), line("4501", BalanceSide.CREDIT, "50000")));
    companies.post(
        companyId,
        LocalDate.of(2025, 3, 10),
        PHP,
        List.of(
            line("5603", BalanceSide.DEBIT, "1000", "FIN", null),
            line("1111", BalanceSide.CREDIT, "1000")));
    approved = createApprovedOriginal();
  }

  private void expense(LocalDate date, String amount) {
    companies.post(
        companyId,
        date,
        PHP,
        List.of(
            line("5601", BalanceSide.DEBIT, amount, "FIN", null),
            line("1111", BalanceSide.CREDIT, amount)));
  }

  private static List<BigDecimal> monthly(String amount) {
    return Collections.nCopies(12, new BigDecimal(amount));
  }

  private static List<BudgetLineRequest> originalLines() {
    return List.of(
        new BudgetLineRequest("5601", "FIN", monthly("100000")),
        new BudgetLineRequest("4501", null, monthly("40000")));
  }

  private Budget createApprovedOriginal() {
    Budget draft =
        asUser.run(
            "accountant",
            () ->
                budgets.create(
                    new CreateBudgetRequest(
                        companyId, 2026, BudgetVersionType.ORIGINAL, "FY2026 budget", null)));
    asUser.run(
        "accountant",
        () ->
            lines.saveLines(
                draft.getId(),
                List.of(
                    new BudgetLineRequest("5601", "FIN", monthly("100000")),
                    new BudgetLineRequest("4501", null, monthly("40000")))));
    asUser.run("accountant", () -> budgets.submit(draft.getId()));
    assertThatThrownBy(() -> asUser.run("accountant", () -> budgets.approve(draft.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("submitted");
    return asUser.run("fmanager", () -> budgets.approve(draft.getId()));
  }

  private static VarianceLine find(BudgetComparison c, String account) {
    return c.lines().stream()
        .filter(l -> l.accountCode().equals(account))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void approvalFollowsMakerChecker() {
    assertThat(approved.getStatus()).isEqualTo(BudgetStatus.APPROVED);
    assertThat(approved.getApprovedBy()).isEqualTo("fmanager");
    assertThat(approved.getSubmittedBy()).isEqualTo("accountant");
    assertThat(approved.total()).isEqualByComparingTo("1680000");
    assertThatThrownBy(
            () ->
                asUser.run(
                    "accountant",
                    () ->
                        budgets.create(
                            new CreateBudgetRequest(
                                companyId, 2026, BudgetVersionType.ORIGINAL, "again", null))))
        .hasMessageContaining("already exists");
  }

  @Test
  void varianceMathsForMonthAndYearToDate() {
    BudgetComparison c = monitoring.compare(companyId, FEB, true);
    assertThat(c.periodNo()).isEqualTo(2);
    VarianceLine salaries = find(c, "5601");
    assertThat(salaries.costCenter()).isEqualTo("FIN");
    assertThat(salaries.budgetMonth()).isEqualByComparingTo("100000");
    assertThat(salaries.actualMonth()).isEqualByComparingTo("110000");
    assertThat(salaries.monthVariance()).isEqualByComparingTo("10000");
    assertThat(salaries.monthVariancePct()).isEqualByComparingTo("10.00");
    assertThat(salaries.budgetYtd()).isEqualByComparingTo("200000");
    assertThat(salaries.actualYtd()).isEqualByComparingTo("200000");
    assertThat(salaries.ytdVariance()).isEqualByComparingTo("0");
    assertThat(salaries.utilizationPct()).isEqualByComparingTo("16.67");
    assertThat(salaries.available()).isEqualByComparingTo("1000000");
    assertThat(salaries.favourable()).isTrue();

    VarianceLine interest = find(c, "4501");
    assertThat(interest.actualYtd()).isEqualByComparingTo("50000");
    assertThat(interest.budgetYtd()).isEqualByComparingTo("80000");
    assertThat(interest.ytdVariance()).isEqualByComparingTo("-30000");
    assertThat(interest.ytdVariancePct()).isEqualByComparingTo("-37.50");
    assertThat(interest.monthVariancePct()).isEqualByComparingTo("-100.00");
    assertThat(interest.favourable()).isFalse();

    VarianceLine unbudgeted = find(monitoring.compare(companyId, FEB, false), "5601");
    assertThat(unbudgeted.costCenter()).isNull();
  }

  @Test
  void alertsListExpenseAccountsOverThreshold() {
    assertThat(monitoring.alerts(companyId, FEB, new BigDecimal("15")))
        .extracting(VarianceLine::accountCode)
        .containsExactly("5601");
    assertThat(monitoring.alerts(companyId, FEB, new BigDecimal("20"))).isEmpty();
  }

  @Test
  void revisionSupersedesOriginalAndSupportsImportAndCopy() {
    Budget revision =
        asUser.run(
            "accountant",
            () ->
                budgets.create(
                    new CreateBudgetRequest(
                        companyId, 2026, BudgetVersionType.REVISED, "Mid-year revision", null)));
    assertThat(revision.getLines()).hasSize(2);
    assertThat(revision.getBasedOnId()).isEqualTo(approved.getId());

    Budget imported =
        asUser.run(
            "accountant",
            () ->
                lines.importCsv(
                    revision.getId(),
                    "account_code,cost_centre,annual\n5601,FIN,2400000\n4501,,480000\n"));
    assertThat(imported.getLines()).hasSize(2);
    assertThat(imported.total()).isEqualByComparingTo("2880000");
    assertThatThrownBy(
            () ->
                asUser.run(
                    "accountant",
                    () ->
                        lines.importCsv(
                            revision.getId(), "account_code,cost_centre,annual\n1111,,5\n")))
        .hasMessageContaining("not a postable income or expense account");

    Budget copied =
        asUser.run(
            "accountant", () -> lines.copyFromActuals(revision.getId(), 2025, BigDecimal.TEN));
    assertThat(copied.getLines()).hasSize(1);
    assertThat(copied.getLines().get(0).getAccountCode()).isEqualTo("5603");
    assertThat(copied.getLines().get(0).amount(3)).isEqualByComparingTo("1100.00");

    asUser.run("accountant", () -> lines.saveLines(revision.getId(), originalLines()));
    asUser.run("accountant", () -> budgets.submit(revision.getId()));
    asUser.run("fmanager", () -> budgets.approve(revision.getId()));

    assertThat(budgets.get(approved.getId()).getStatus()).isEqualTo(BudgetStatus.SUPERSEDED);
    assertThat(budgets.approved(companyId, 2026).orElseThrow().getId()).isEqualTo(revision.getId());
    assertThat(budgets.list(companyId, null)).hasSizeGreaterThanOrEqualTo(2);

    Budget rejectedDraft =
        asUser.run(
            "accountant",
            () ->
                budgets.create(
                    new CreateBudgetRequest(
                        companyId, 2026, BudgetVersionType.REVISED, "Rejected revision", null)));
    asUser.run("accountant", () -> budgets.submit(rejectedDraft.getId()));
    Budget rejected =
        asUser.run("fmanager", () -> budgets.reject(rejectedDraft.getId(), "Too high"));
    assertThat(rejected.getStatus()).isEqualTo(BudgetStatus.REJECTED);
    assertThat(rejected.getRejectionReason()).isEqualTo("Too high");
  }

  @Test
  void budgetReportsRunAndExport() {
    Map<String, String> params =
        Map.of(
            "companyId", companyId.toString(), "asOfDate", FEB.toString(), "byCostCenter", "true");
    asUser.run(
        "fmanager",
        () -> {
          for (String code : new String[] {"GL-BVA", "GL-BUTIL"}) {
            assertThat(reports.run(code, params).rows()).isNotEmpty();
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export(code, params, format).content()).isNotEmpty();
            }
          }
          return null;
        });
  }
}
