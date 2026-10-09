package com.iortatechnxt.brokerverse.renewal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.budget.service.RenewalBudgetService;
import com.iortatechnxt.brokerverse.renewal.budget.service.RenewalBudgetService.BudgetInput;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardDrillService;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.ProcessingDashboardService;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.RenewalDashboardService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Renewal and Processing dashboards and the Annual Renewal Budget (BDOI Renewal FRS FRRN.002.02,
 * FRRN.003, FRRN.042) against the real database: the figures of the user's scope and their
 * drill-down lists, the segment filter, the budget against actual, the budget history and
 * validations, the combined processing view and the Placement Processor.
 */
@IntegrationTest
class RenewalDashboardIT {

  private static final String ADMIN = "badmin";
  private static final LocalDate FROM = LocalDate.of(2027, 1, 1);
  private static final LocalDate TO = LocalDate.of(2027, 12, 31);

  @Autowired private RenewalFixtures fx;
  @Autowired private BookingFixtures booking;
  @Autowired private RenewalDashboardService dashboard;
  @Autowired private DashboardDrillService drill;
  @Autowired private ProcessingDashboardService processing;
  @Autowired private RenewalBudgetService budgets;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private DashboardFilter period(String segment) {
    return new DashboardFilter(fx.company(), FROM, TO, segment, null, null);
  }

  private long card(RenewalDashboardService.Dashboard d, String key) {
    return d.cards().stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow().count();
  }

  @Test
  void everyCardListsExactlyTheRenewalsItCountsAndFollowsTheSegment() {
    RenewalCandidate c = fx.unassignedRetail();
    RenewalDashboardService.Dashboard d =
        as.run(RenewalFixtures.TL, () -> dashboard.dashboard(period(null), 50));
    for (String key : List.of("TOTAL_EXPIRING", "FOR_DISPOSITION", "UNRENEWED")) {
      DashboardDrillService.Drill list =
          as.run(RenewalFixtures.TL, () -> drill.drill(period(null), "CARD|" + key, null));
      assertThat(list.rows()).as(key).hasSize((int) card(d, key));
      assertThat(list.rows()).as(key).anyMatch(r -> r.ref().equals(c.getRenewalRef()));
    }
    RenewalDashboardService.Dashboard cbg =
        as.run(RenewalFixtures.TL, () -> dashboard.dashboard(period("CBG"), 50));
    DashboardDrillService.Drill cbgList =
        as.run(RenewalFixtures.TL, () -> drill.drill(period("CBG"), "CARD|TOTAL_EXPIRING", null));
    assertThat(cbgList.rows()).noneMatch(r -> r.ref().equals(c.getRenewalRef()));
    assertThat(cbgList.rows()).hasSize((int) card(cbg, "TOTAL_EXPIRING"));
    assertThat(card(cbg, "TOTAL_EXPIRING")).isLessThan(card(d, "TOTAL_EXPIRING"));
  }

  @Test
  void pipelineAgeingAndProductMixTotalsEqualTheirParts() {
    fx.unassignedRetail();
    RenewalDashboardService.Dashboard d =
        as.run(RenewalFixtures.TL, () -> dashboard.dashboard(period(null), 50));
    RenewalDashboardService.PipelineRow total = d.pipeline().total();
    long stages = total.cells().stream().mapToLong(RenewalDashboardService.Cell::count).sum();
    assertThat(total.total().count()).isEqualTo(stages);
    BigDecimal premium =
        total.cells().stream()
            .map(RenewalDashboardService.Cell::premium)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total.total().premium()).isEqualByComparingTo(premium);
    RenewalDashboardService.Cell ageingTotal = d.ageing().get(d.ageing().size() - 1);
    assertThat(ageingTotal.count())
        .isEqualTo(
            d.ageing().subList(0, 4).stream().mapToLong(RenewalDashboardService.Cell::count).sum());
    DashboardDrillService.Drill pipelineList =
        as.run(RenewalFixtures.TL, () -> drill.drill(period(null), total.total().metric(), null));
    assertThat(pipelineList.rows()).hasSize((int) total.total().count());
    assertThat(pipelineList.totalPremium()).isEqualByComparingTo(total.total().premium());
    assertThat(d.topOptions()).containsExactly(50, 75, 100);
  }

  @Test
  void theBudgetIsComparedWithTheActualAndKeepsItsHistory() {
    RenewalBudget.Key key =
        new RenewalBudget.Key(2027, "PREMIUM", "INSTITUTIONAL", "NCR", null, null, null);
    List<RenewalBudget.Month> months =
        List.of(
            new RenewalBudget.Month(
                3, BigDecimal.ZERO, new BigDecimal("4000000.00"), BigDecimal.ZERO),
            new RenewalBudget.Month(
                4, BigDecimal.ZERO, new BigDecimal("6000000.00"), BigDecimal.ZERO));
    RenewalBudget saved =
        as.run(
            ADMIN,
            () ->
                budgets.save(
                    fx.company(),
                    new BudgetInput(
                        key, new RenewalBudget.Heads(null, null, null, "mkttl"), months)));
    assertThat(saved.totals().renewalTotal()).isEqualByComparingTo("10000000.00");
    assertThat(saved.totals().grandTotal()).isEqualByComparingTo("10000000.00");
    DashboardFilter q1 =
        new DashboardFilter(
            fx.company(),
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 4, 30),
            "INSTITUTIONAL",
            null,
            null);
    RenewalDashboardService.Production premium =
        as.run(ADMIN, () -> dashboard.dashboard(q1, 50)).production().get(0);
    assertThat(premium.budget()).isEqualByComparingTo("10000000.00");
    assertThat(premium.variance())
        .isEqualByComparingTo(premium.actual().subtract(premium.budget()));

    as.run(
        ADMIN,
        () ->
            budgets.save(
                fx.company(),
                new BudgetInput(
                    key,
                    new RenewalBudget.Heads(null, null, null, "mkttl"),
                    List.of(
                        new RenewalBudget.Month(
                            3, BigDecimal.ZERO, new BigDecimal("4500000.00"), BigDecimal.ZERO)))));
    var history = as.run(ADMIN, () -> budgets.history(fx.company(), saved.getId()));
    assertThat(history).hasSize(1);
    assertThat(history.get(0).getField()).isEqualTo("Renewal budget - March");
    assertThat(history.get(0).getPrevious()).isEqualByComparingTo("4000000.00");
    assertThat(history.get(0).getUpdated()).isEqualByComparingTo("4500000.00");
    assertThat(history.get(0).getModifiedBy()).isEqualTo(ADMIN);
  }

  @Test
  void budgetValidationsRefuseNegativeAmountsAndCorporateWithoutTeam() {
    BudgetInput corporate =
        new BudgetInput(
            new RenewalBudget.Key(2027, "PREMIUM", "CORBANK", null, null, null, null),
            new RenewalBudget.Heads(null, null, null, null),
            List.of());
    assertThatThrownBy(() -> as.run(ADMIN, () -> budgets.save(fx.company(), corporate)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Team and Sub-Team are required");
    BudgetInput negative =
        new BudgetInput(
            new RenewalBudget.Key(2027, "PREMIUM", "RETAIL", "NCR", null, null, "ao"),
            new RenewalBudget.Heads(null, null, null, null),
            List.of(
                new RenewalBudget.Month(
                    1, new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO)));
    assertThatThrownBy(() -> as.run(ADMIN, () -> budgets.save(fx.company(), negative)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be negative");
  }

  @Test
  void theCombinedProcessingViewIsNewBusinessPlusRenewalAndTheProcessorIsAssigned() {
    Account issued =
        booking.issued(BookingFixtures.spec("MTR10", "RETAIL", PaymentArrangement.VIA_BROKER));
    DashboardFilter all =
        new DashboardFilter(fx.company(), BookingFixtures.FROM.minusDays(1), TO, null, null, null);
    ProcessingDashboardService.Dashboard d =
        as.run(RenewalFixtures.PROC_TL, () -> processing.dashboard(all));
    for (ProcessingDashboardService.Card c : d.cards()) {
      assertThat(c.count()).as(c.key()).isEqualTo(c.newBusiness() + c.renewal());
    }
    var forBooking =
        as.run(RenewalFixtures.PROC_TL, () -> processing.drill(all, "FOR_BOOKING", null));
    assertThat(forBooking).anyMatch(i -> issued.getArn().equals(i.text("arn")));
    as.run(
        RenewalFixtures.PROC_TL,
        () -> processing.assign(fx.company(), List.of(issued.getArn()), RenewalFixtures.PO));
    var after = as.run(RenewalFixtures.PROC_TL, () -> processing.drill(all, "FOR_BOOKING", null));
    assertThat(after)
        .filteredOn(i -> issued.getArn().equals(i.text("arn")))
        .allMatch(i -> RenewalFixtures.PO.equals(i.text("processor")));
  }

  @Test
  void theDrillDownExportsThroughTheReportCentre() {
    fx.unassignedRetail();
    ReportResult result =
        as.run(
            RenewalFixtures.TL,
            () ->
                reports.run(
                    "RNW-DASHBOARD",
                    Map.of(
                        "companyId", fx.company().toString(),
                        "metric", "CARD|TOTAL_EXPIRING",
                        "from", FROM.toString(),
                        "to", TO.toString())));
    assertThat(result.code()).isEqualTo("RNW-DASHBOARD");
    assertThat(result.rows()).isNotEmpty();
  }
}
