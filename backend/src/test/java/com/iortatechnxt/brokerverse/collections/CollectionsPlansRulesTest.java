package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine.LineKind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Kind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationItem;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule.Filters;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Dates;
import com.iortatechnxt.brokerverse.collections.escalation.service.RuleMatcher;
import com.iortatechnxt.brokerverse.collections.escalation.service.RuleMatcher.Signals;
import com.iortatechnxt.brokerverse.collections.installment.domain.BillingFrequency;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentSchedule;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentSchedule.Cycle;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Account;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Terms;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseEvaluator;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure rules of the Collections plans and escalations: billing cycles, equal installments,
 * allocation oldest due first and installment status (BRCLXN.053/054/058), promise evaluation
 * (BRCLXN.055), rule matching (BRCLXN.049) and the entity invariants.
 */
class CollectionsPlansRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  @Test
  void aThreeYearPeriodHasOneCycleAYearAnnuallyAndFourQuarterly() {
    List<Cycle> annual =
        InstallmentSchedule.cycles(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1), 12);
    assertThat(annual)
        .containsExactly(new Cycle(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31)));
    List<Cycle> quarterly =
        InstallmentSchedule.cycles(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1), 3);
    assertThat(quarterly).hasSize(4);
    assertThat(quarterly.get(1))
        .isEqualTo(new Cycle(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 28)));
    assertThat(quarterly.get(3).to()).isEqualTo(LocalDate.of(2027, 8, 31));
    // a short period still has one cycle
    assertThat(InstallmentSchedule.cycles(TODAY, TODAY, 12))
        .containsExactly(new Cycle(TODAY, TODAY));
    assertThat(InstallmentSchedule.consecutive(LocalDate.of(2026, 1, 31), 2, 1))
        .containsExactly(
            new Cycle(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 27)),
            new Cycle(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 30)));
  }

  @Test
  void installmentsAreEqualWithTheRoundingOnTheLastAndAllocatedOldestFirst() {
    assertThat(InstallmentSchedule.split(d("100.00"), 3))
        .containsExactly(d("33.33"), d("33.33"), d("33.34"));
    assertThat(InstallmentSchedule.split(d("50"), 1)).containsExactly(d("50.00"));
    List<BigDecimal> amounts = List.of(d("40.00"), d("40.00"), d("20.00"));
    assertThat(InstallmentSchedule.allocate(amounts, d("50.00")))
        .containsExactly(d("40.00"), d("10.00"), d("0.00"));
    assertThat(InstallmentSchedule.allocate(amounts, d("-5.00")))
        .containsExactly(d("0.00"), d("0.00"), d("0.00"));
    assertThat(InstallmentSchedule.allocate(amounts, d("500.00")))
        .containsExactly(d("40.00"), d("40.00"), d("20.00"));
  }

  @Test
  void theInstallmentStatusFollowsTheDueDateAndThePayments() {
    InstallmentPlan plan =
        InstallmentPlan.create(
            new InstallmentPlan.Header(
                1L,
                "IPL-T",
                "ARN-T",
                "BI-T",
                "CL-T",
                "Assured",
                "PHP",
                "QUARTERLY",
                PlanSource.GENERATED,
                null),
            List.of(
                terms(1, TODAY.minusDays(10), "100.00"),
                terms(2, TODAY, "100.00"),
                terms(3, TODAY.plusDays(30), "100.00")));
    assertThat(plan.getTotal()).isEqualByComparingTo("300.00");
    assertThat(plan.getFirstDue()).isEqualTo(TODAY.minusDays(10));
    Installment first = plan.installment(1);
    Installment second = plan.installment(2);
    Installment third = plan.installment(3);

    first.allocate(d("40.00"), TODAY);
    second.allocate(BigDecimal.ZERO, TODAY);
    third.allocate(d("5.00"), TODAY);
    assertThat(first.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    assertThat(first.getOverdueSince()).isEqualTo(TODAY.minusDays(9));
    assertThat(second.getStatus()).isEqualTo(InstallmentStatus.DUE);
    assertThat(third.getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
    third.allocate(BigDecimal.ZERO, TODAY);
    assertThat(third.getStatus()).isEqualTo(InstallmentStatus.NOT_DUE);

    first.allocate(d("150.00"), TODAY);
    assertThat(first.getStatus()).isEqualTo(InstallmentStatus.PAID);
    assertThat(first.getPaidAmount()).isEqualByComparingTo("100.00");
    assertThat(first.getPaidOn()).isEqualTo(TODAY);
    first.allocate(d("100.00"), TODAY.plusDays(1));
    assertThat(first.getPaidOn()).isEqualTo(TODAY);
    assertThat(first.balance()).isEqualByComparingTo("0");

    second.allocate(d("100.00"), TODAY);
    third.allocate(d("100.00"), TODAY);
    plan.refreshed(Instant.now());
    assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
    assertThat(plan.getPaidTotal()).isEqualByComparingTo("300.00");
    assertThatThrownBy(() -> plan.cancel("late"))
        .extracting("code")
        .isEqualTo("CLX_PLAN_NOT_ACTIVE");
    assertThatThrownBy(() -> plan.installment(9)).extracting("code").isEqualTo("CLX_CYCLE_UNKNOWN");
    assertThatThrownBy(
            () ->
                InstallmentPlan.create(
                    new InstallmentPlan.Header(
                        1L,
                        "IPL-E",
                        "ARN-T",
                        null,
                        "CL-T",
                        "A",
                        "PHP",
                        "ANNUAL",
                        PlanSource.POLICY_YEARS,
                        null),
                    List.of()))
        .extracting("code")
        .isEqualTo("CLX_PLAN_EMPTY");
  }

  private static Installment.Terms terms(int seq, LocalDate due, String amount) {
    return new Installment.Terms(
        seq, 1, "BI-T", due, due, due.plusMonths(3).minusDays(1), d(amount));
  }

  @Test
  void billingFrequenciesHaveACycleLength() {
    assertThat(BillingFrequency.of("SEMI_ANNUAL").months()).isEqualTo(6);
    assertThat(BillingFrequency.of("MONTHLY").months()).isEqualTo(1);
    assertThatThrownBy(() -> BillingFrequency.of("WEEKLY"))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("CLX_FREQUENCY_UNSUPPORTED");
  }

  @Test
  void aPromiseIsKeptPartlyKeptOrBroken() {
    assertThat(PromiseEvaluator.evaluate(d("100"), d("100"), TODAY, false).status())
        .isEqualTo(PromiseStatus.KEPT);
    assertThat(PromiseEvaluator.evaluate(d("100"), d("40"), TODAY, false).status())
        .isEqualTo(PromiseStatus.PARTIALLY_KEPT);
    assertThat(PromiseEvaluator.evaluate(d("100"), d("-5"), null, false).status())
        .isEqualTo(PromiseStatus.BROKEN);
    assertThat(PromiseEvaluator.evaluate(d("100"), BigDecimal.ZERO, null, true).status())
        .isEqualTo(PromiseStatus.KEPT);

    PaymentPromise promise =
        new PaymentPromise(
            new Account(1L, "BI-T", "ARN-T", "CL-T", "Assured", "PHP"),
            new Terms(TODAY.minusDays(3), TODAY, d("100"), null),
            "call",
            null);
    assertThat(promise.deadline(2)).isEqualTo(TODAY.plusDays(2));
    promise.evaluate(PromiseEvaluator.evaluate(d("100"), d("30"), TODAY, false), Instant.now());
    assertThat(promise.getStatus()).isEqualTo(PromiseStatus.PARTIALLY_KEPT);
    assertThat(promise.getActualPaid()).isEqualByComparingTo("30.00");
    assertThatThrownBy(() -> promise.cancel("again", Instant.now()))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_CLOSED");
    assertThatThrownBy(
            () ->
                new PaymentPromise(
                    new Account(1L, "BI-T", "ARN-T", "CL-T", "A", "PHP"),
                    new Terms(TODAY, TODAY.minusDays(1), d("1"), null),
                    null,
                    null))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_DATES");
    assertThatThrownBy(
            () ->
                new PaymentPromise(
                    new Account(1L, "BI-T", "ARN-T", "CL-T", "A", "PHP"),
                    new Terms(TODAY, TODAY, BigDecimal.ZERO, null),
                    null,
                    null))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_AMOUNT");
  }

  @Test
  void rulesMatchOnAgingCommitmentsPromisesInstallmentsAndAmount() {
    Candidate account =
        new Candidate(
            "BI-T",
            "ARN-T",
            "CL-T",
            "Assured",
            null,
            "PHP",
            new Dates(TODAY.minusDays(46), TODAY.minusDays(61)),
            "ao",
            d("5000.00"));
    Signals none = Signals.NONE;
    assertThat(RuleMatcher.matches(Basis.AGING_FROM_BOOKING, d("45"), account, none, TODAY))
        .isTrue();
    assertThat(RuleMatcher.matches(Basis.AGING_FROM_BOOKING, d("47"), account, none, TODAY))
        .isFalse();
    assertThat(RuleMatcher.matches(Basis.AGING_FROM_INCEPTION, d("60"), account, none, TODAY))
        .isTrue();
    assertThat(RuleMatcher.matches(Basis.NO_COMMITMENT_BY_DAY, d("30"), account, none, TODAY))
        .isTrue();
    assertThat(
            RuleMatcher.matches(
                Basis.NO_COMMITMENT_BY_DAY, d("30"), account, new Signals(0, true, 0), TODAY))
        .isFalse();
    assertThat(
            RuleMatcher.matches(
                Basis.BROKEN_PROMISES_COUNT, d("2"), account, new Signals(2, false, 0), TODAY))
        .isTrue();
    assertThat(RuleMatcher.matches(Basis.BROKEN_PROMISES_COUNT, d("1"), account, none, TODAY))
        .isFalse();
    assertThat(
            RuleMatcher.matches(
                Basis.INSTALLMENT_OVERDUE_DAYS, d("15"), account, new Signals(0, false, 15), TODAY))
        .isTrue();
    assertThat(RuleMatcher.matches(Basis.AMOUNT_OVER, d("5000"), account, none, TODAY)).isTrue();
    assertThat(RuleMatcher.matches(Basis.AMOUNT_OVER, d("5000.01"), account, none, TODAY))
        .isFalse();
    assertThat(RuleMatcher.days(TODAY.plusDays(3), TODAY)).isZero();
  }

  @Test
  void aRuleIsValidatedAndAppliesOnceAuthorizedWithinItsDates() {
    EscalationRule rule = new EscalationRule(1L, "R1", ruleTerms(TargetLevel.TL, null, null));
    assertThat(rule.appliesOn(TODAY)).isFalse();
    rule.authorize("checker", Instant.now());
    assertThat(rule.appliesOn(TODAY)).isTrue();
    assertThat(rule.appliesOn(LocalDate.of(2025, 12, 31))).isFalse();
    rule.change(ruleTerms(TargetLevel.UH, null, TODAY.minusDays(1)));
    assertThat(rule.getRecordStatus().name()).isEqualTo("PENDING_AUTHORIZATION");
    rule.authorize("checker", Instant.now());
    assertThat(rule.appliesOn(TODAY)).isFalse();
    assertThat(rule.filters().segment()).isEqualTo("CBG");

    assertThatThrownBy(() -> new EscalationRule(1L, "R2", ruleTerms(TargetLevel.USER, null, null)))
        .extracting("code")
        .isEqualTo("CLX_RULE_TARGET");
    assertThatThrownBy(
            () ->
                new EscalationRule(
                    1L, "R3", ruleTerms(TargetLevel.TL, null, LocalDate.of(2025, 1, 1))))
        .extracting("code")
        .isEqualTo("CLX_RULE_DATES");
    assertThatThrownBy(
            () ->
                new EscalationRule(
                    1L,
                    "R4",
                    new EscalationRule.Terms(
                        "x",
                        Basis.AMOUNT_OVER,
                        BigDecimal.ZERO,
                        filters(null, null),
                        TargetLevel.TL,
                        null,
                        "AGING",
                        24,
                        true,
                        TODAY,
                        null)))
        .extracting("code")
        .isEqualTo("CLX_RULE_INVALID");
    assertThatThrownBy(
            () ->
                new EscalationRule(
                    1L,
                    "R5",
                    new EscalationRule.Terms(
                        "x",
                        Basis.AMOUNT_OVER,
                        BigDecimal.TEN,
                        filters(d("10"), d("5")),
                        TargetLevel.TL,
                        null,
                        "AGING",
                        24,
                        true,
                        TODAY,
                        null)))
        .extracting("code")
        .isEqualTo("CLX_RULE_AMOUNTS");
  }

  private static Filters filters(BigDecimal from, BigDecimal to) {
    return new Filters("CBG", null, null, from, to);
  }

  private static EscalationRule.Terms ruleTerms(
      TargetLevel level, String user, LocalDate effectiveTo) {
    return new EscalationRule.Terms(
        "Aging",
        Basis.AGING_FROM_BOOKING,
        d("45"),
        filters(null, null),
        level,
        user,
        "AGING",
        24,
        true,
        LocalDate.of(2026, 1, 1),
        effectiveTo);
  }

  @Test
  void anEscalationTotalsItsInvoicesMirrorsItsStageAndIsOverduePastItsSla() {
    Instant raised = Instant.parse("2026-09-20T00:00:00Z");
    Escalation e =
        Escalation.raise(
            new Escalation.Header(
                1L,
                "ESC-T",
                Kind.MANUAL,
                null,
                "ARN-T",
                "CL-T",
                "Assured",
                TargetLevel.TL,
                null,
                "AGING",
                "remarks",
                "PHP",
                24,
                null,
                null),
            List.of(
                new EscalationItem.Facts("BI-1", null, d("100.00"), 10),
                new EscalationItem.Facts("BI-2", "POL", d("50.00"), 5)),
            raised);
    assertThat(e.getTotalBalance()).isEqualByComparingTo("150.00");
    assertThat(e.getItems()).hasSize(2);
    assertThat(e.isOverdue(raised.plus(2, ChronoUnit.DAYS))).isFalse(); // RAISED is not timed
    e.markStage(Stage.WITH_TL, null, raised);
    assertThat(e.isOverdue(raised.plus(23, ChronoUnit.HOURS))).isFalse();
    assertThat(e.isOverdue(raised.plus(25, ChronoUnit.HOURS))).isTrue();
    e.markStage(Stage.WITH_UH, null, raised);
    assertThat(e.getTargetLevel()).isEqualTo(TargetLevel.UH);
    e.markStage(Stage.RESOLVED, "Paid", raised);
    assertThat(e.getResolution()).isEqualTo("Paid");
    assertThat(e.getResolvedAt()).isEqualTo(raised);
    assertThat(e.isOverdue(raised.plus(30, ChronoUnit.DAYS))).isFalse();
    assertThat(Stage.RESOLVED.isOpen()).isFalse();
    assertThat(TargetLevel.SECTION_HEAD.isHead()).isTrue();
  }

  @Test
  void aStatementTotalsItsLinesAndCannotBeSentOnceCancelled() {
    BillingStatement soa =
        BillingStatement.create(
            new BillingStatement.Header(
                1L,
                "SOA-T",
                1L,
                2,
                "ARN-T",
                "CL-T",
                "Assured",
                "PHP",
                "ANNUAL",
                TODAY,
                TODAY.plusYears(1).minusDays(1),
                TODAY),
            List.of(
                new BillingStatementLine.Facts(
                    LineKind.ARREARS,
                    "BI-1",
                    1,
                    1,
                    TODAY.minusYears(1),
                    TODAY.minusDays(1),
                    TODAY.minusYears(1),
                    d("100.00"),
                    d("60.00")),
                new BillingStatementLine.Facts(
                    LineKind.CURRENT,
                    null,
                    2,
                    2,
                    TODAY,
                    TODAY.plusYears(1).minusDays(1),
                    TODAY,
                    d("100.00"),
                    BigDecimal.ZERO)));
    assertThat(soa.getTotal()).isEqualByComparingTo("200.00");
    assertThat(soa.getPaid()).isEqualByComparingTo("60.00");
    assertThat(soa.getBalance()).isEqualByComparingTo("140.00");
    assertThat(soa.getLines())
        .extracting(BillingStatementLine::getBalance)
        .containsExactly(d("40.00"), d("100.00"));
    soa.cancel("wrong cycle");
    assertThatThrownBy(() -> soa.sent("a@b.c", 1L, Instant.now()))
        .extracting("code")
        .isEqualTo("CLX_SOA_CANCELLED");
  }
}
