package com.iortatechnxt.finverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.closing.service.CheckItem;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import com.iortatechnxt.finverse.reserves.domain.RunLine;
import com.iortatechnxt.finverse.reserves.domain.RunStatus;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.service.ReserveApprovalSource;
import com.iortatechnxt.finverse.reserves.service.ReserveCloseCheck;
import com.iortatechnxt.finverse.reserves.service.ReserveValuationJob;
import com.iortatechnxt.finverse.reserves.service.ValuationRunService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.system.service.SystemParameterService;
import com.iortatechnxt.finverse.underwriting.UwFixtures;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valuation runs without the claims and reinsurance modules (their kernel ports are absent in this
 * context): movement journals, ledger reconciliation, idempotency, cancellation, maker-checker, the
 * monthly job, the approval inbox and the period-end checklist control.
 */
@IntegrationTest
@Transactional
class ReservesIT {

  private static final LocalDate MARCH = LocalDate.of(2026, 3, 31);
  private static final LocalDate APRIL = LocalDate.of(2026, 4, 30);

  /** Reserve type, the part carried by an account, the account and its sign (asset +1). */
  private static final List<Object[]> ACCOUNTS =
      List.of(
          new Object[] {ReserveType.UPR, false, "2101", -1},
          new Object[] {ReserveType.UPR, true, "1301", 1},
          new Object[] {ReserveType.DAC, false, "1400", 1},
          new Object[] {ReserveType.DAC, true, "2400", -1},
          new Object[] {ReserveType.IBNR, false, "2103", -1},
          new Object[] {ReserveType.ULAE, false, "2105", -1},
          new Object[] {ReserveType.MFAD, false, "2106", -1},
          new Object[] {ReserveType.PDR, false, "2104", -1});

  @Autowired private ReserveFixtures fx;
  @Autowired private UwFixtures uw;
  @Autowired private ValuationRunService runs;
  @Autowired private ReserveCloseCheck closeCheck;
  @Autowired private ReserveApprovalSource inbox;
  @Autowired private ReserveValuationJob job;
  @Autowired private SystemParameterService systemParameters;
  @Autowired private AsUser as;

  private Map<String, BigDecimal> opening;

  @BeforeEach
  void portfolio() {
    fx.parameters("FIRE", ReserveFixtures.terms(IbnrMethod.RATE, "5", "95"));
    Product product = uw.product("FIRE", false);
    uw.issue(uw.brokerRequest(product), UwFixtures.ISSUE);
    opening = balances(LocalDate.of(2026, 2, 28));
  }

  private Map<String, BigDecimal> balances(LocalDate asOf) {
    Map<String, BigDecimal> out = new LinkedHashMap<>();
    for (Object[] a : ACCOUNTS) {
      out.put((String) a[2], fx.balance((String) a[2], asOf));
    }
    return out;
  }

  /** Every reserve account moved, since the opening, by exactly the reserve of the run. */
  private void assertLedgerEqualsReserves(ValuationRun run, LocalDate asOf) {
    for (Object[] a : ACCOUNTS) {
      boolean ri = (Boolean) a[1];
      BigDecimal reserve =
          fx.total(run, (ReserveType) a[0], ri ? RunLine::getRiAmount : RunLine::getGrossAmount);
      BigDecimal moved = fx.balance((String) a[2], asOf).subtract(opening.get((String) a[2]));
      assertThat(moved)
          .as("account %s vs %s", a[2], a[0])
          .isEqualByComparingTo(reserve.multiply(BigDecimal.valueOf((Integer) a[3])));
    }
  }

  @Test
  void postedRunsBookMovementsWhoseLedgerBalanceEqualsTheReserves() {
    ValuationRun march = fx.posted(MARCH);
    assertThat(march.getStatus()).isEqualTo(RunStatus.POSTED);
    assertThat(fx.total(march, ReserveType.UPR, RunLine::getGrossAmount)).isPositive();
    assertThat(fx.total(march, ReserveType.PDR, RunLine::getGrossAmount)).isPositive();
    // The claims module is deployed: OSLR comes from its ClaimsExperienceView.
    assertThat(march.getRemarks()).doesNotContain("Claims module not available");
    assertLedgerEqualsReserves(march, MARCH);

    ValuationRun april = fx.posted(APRIL);
    assertThat(april.getPreviousRunId()).isEqualTo(march.getId());
    assertLedgerEqualsReserves(april, APRIL);

    int journals = fx.reserveJournals();
    assertThat(as.run(ReserveFixtures.CHECKER, () -> runs.post(april.getId())).getJournalCount())
        .isEqualTo(april.getJournalCount());
    assertThat(fx.reserveJournals()).isEqualTo(journals);

    ValuationRun february = fx.submitted(LocalDate.of(2026, 2, 10));
    as.run(ReserveFixtures.CHECKER, () -> runs.approve(february.getId()));
    assertThatThrownBy(() -> as.run(ReserveFixtures.CHECKER, () -> runs.post(february.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("later valuation");
    assertThatThrownBy(
            () -> as.run(ReserveFixtures.CHECKER, () -> runs.cancel(march.getId(), "wrong")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("latest posted");

    ValuationRun cancelled =
        as.run(ReserveFixtures.CHECKER, () -> runs.cancel(april.getId(), "Late claims"));
    assertThat(cancelled.getStatus()).isEqualTo(RunStatus.CANCELLED);
    assertThat(cancelled.getCancelDate()).isEqualTo(APRIL);
    assertLedgerEqualsReserves(march, APRIL);
    assertThat(checklist(APRIL).passed()).isFalse();
    assertThat(checklist(MARCH).passed()).isTrue();
    assertThat(runs.forMonth(fx.companyId(), APRIL)).isEmpty();
    assertThat(fx.posted(APRIL).getPreviousRunId()).isEqualTo(march.getId());
  }

  private CheckItem checklist(LocalDate periodEnd) {
    return closeCheck
        .periodEndChecks(fx.companyId(), periodEnd.withDayOfMonth(1), periodEnd)
        .get(0);
  }

  @Test
  void lifecycleEnforcesMakerCheckerAndStatuses() {
    ValuationRun run = fx.submitted(MARCH);
    assertThatThrownBy(() -> as.run(ReserveFixtures.MAKER, () -> runs.approve(run.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("preparer");
    assertThatThrownBy(() -> fx.submitted(MARCH))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already exists");
    ApprovalViewer checker =
        ApprovalViewer.user("fmanager", Set.of("PERIOD_END_RUN", "MASTER_AUTHORIZE"));
    assertThat(inbox.pendingFor(checker)).anyMatch(i -> i.reference().equals("2026-03"));
    assertThat(inbox.pendingFor(ApprovalViewer.user("accountant", Set.of("PERIOD_END_RUN"))))
        .noneMatch(i -> i.reference().equals("2026-03"));

    ValuationRun rejected =
        as.run(ReserveFixtures.CHECKER, () -> runs.reject(run.getId(), "Check parameters"));
    assertThat(rejected.getStatus()).isEqualTo(RunStatus.PREVIEW);
    assertThat(rejected.getRejectionReason()).isEqualTo("Check parameters");
    assertThat(as.run(ReserveFixtures.MAKER, () -> runs.recalculate(run.getId())).getLines())
        .isNotEmpty();
    assertThatThrownBy(() -> as.run(ReserveFixtures.CHECKER, () -> runs.post(run.getId())))
        .isInstanceOf(BusinessRuleException.class);
    int journals = fx.reserveJournals();
    ValuationRun cancelled =
        as.run(ReserveFixtures.CHECKER, () -> runs.cancel(run.getId(), "Not needed"));
    assertThat(cancelled.getCancelDate()).isNull();
    assertThat(fx.reserveJournals()).isEqualTo(journals);
    assertThatThrownBy(
            () -> as.run(ReserveFixtures.CHECKER, () -> runs.cancel(run.getId(), "again")))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(runs.uprDetail(run.getId(), "FIRE", Pageable.ofSize(5))).isNotEmpty();
  }

  @Test
  void monthlyJobPreparesThePreviousMonthOnce() {
    assertThat(job.cron()).isEqualTo("-");
    assertThat(job.name()).isEqualTo(ReserveValuationJob.NAME);
    assertThat(job.description()).isNotBlank();
    assertThat(job.execute(LocalDate.of(2026, 6, 3)).itemsProcessed()).isZero();
    as.run("admin", () -> systemParameters.update(ReserveValuationJob.COMPANIES, "FVI"));
    assertThat(job.execute(LocalDate.of(2026, 6, 3)).itemsProcessed()).isEqualTo(1);
    assertThat(runs.forMonth(fx.companyId(), LocalDate.of(2026, 5, 31)))
        .map(ValuationRun::getStatus)
        .contains(RunStatus.PENDING_APPROVAL);
    assertThat(job.execute(LocalDate.of(2026, 6, 20)).itemsProcessed()).isZero();
  }
}
