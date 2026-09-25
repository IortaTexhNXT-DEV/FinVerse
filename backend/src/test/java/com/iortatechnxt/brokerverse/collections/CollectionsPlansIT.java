package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement.StatementStatus;
import com.iortatechnxt.brokerverse.collections.billing.service.BillingStatementService;
import com.iortatechnxt.brokerverse.collections.billing.service.SoaDispatch;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService.ManualEntry;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Installment plans and statements of account (BRCLXN.053/054/058/060): the policy-year plan of a
 * three-year account billed per cycle, generated and manual plans, allocation of the ledger
 * payments oldest due first, overdue flags, the billing run of a period, sending and cancelling a
 * statement.
 */
@IntegrationTest
class CollectionsPlansIT {

  private static final String HANDLER = "mktcoll";

  @Autowired private CollectionsFixtures fx;
  @Autowired private InstallmentPlanService plans;
  @Autowired private BillingStatementService statements;
  @Autowired private SoaDispatch dispatch;
  @Autowired private MessageService messages;
  @Autowired private AsUser as;

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  @Test
  void aThreeYearAccountGetsAStatementOfAccountPerBillingCycle() {
    BookedInvoice year1 = fx.threeYearAccount();
    Long company = fx.company();
    InstallmentPlan plan =
        as.run(
            HANDLER, () -> plans.fromPolicyYears(company, year1.getArn(), "ANNUAL", "three years"));

    assertThat(plan.getSource()).isEqualTo(PlanSource.POLICY_YEARS);
    assertThat(plan.getInvoiceNo()).isNull();
    assertThat(plan.getInstallments())
        .extracting(Installment::getPolicyYear)
        .containsExactly(1, 2, 3);
    assertThat(plan.getInstallments())
        .extracting(Installment::getDueDate)
        .containsExactly(
            CollectionsFixtures.MULTI_YEAR_FROM,
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(1),
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(2));
    assertThat(plan.getInstallments())
        .extracting(Installment::getInvoiceNo)
        .containsExactly(year1.getInvoiceNo(), null, null);
    assertThat(plan.installment(1).getAmount())
        .isEqualByComparingTo(fx.invoice(year1.getInvoiceNo()).getGrossPremium());
    assertThat(plan.getTotal()).isPositive();

    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER, () -> plans.fromPolicyYears(company, year1.getArn(), "ANNUAL", null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_EXISTS");
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.generate(
                            company, year1.getInvoiceNo(), "QUARTERLY", LocalDate.now(), 4, null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_EXISTS");

    List<BillingStatement> soas =
        List.of(1, 2, 3).stream()
            .map(seq -> as.run(HANDLER, () -> statements.generate(plan.getId(), seq)))
            .toList();
    assertThat(soas)
        .extracting(BillingStatement::getCycleFrom)
        .containsExactly(
            CollectionsFixtures.MULTI_YEAR_FROM,
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(1),
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(2));
    assertThat(soas)
        .extracting(BillingStatement::getCycleTo)
        .containsExactly(
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(1).minusDays(1),
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(2).minusDays(1),
            CollectionsFixtures.MULTI_YEAR_FROM.plusYears(3).minusDays(1));
    assertThat(soas).allSatisfy(s -> assertThat(s.getSoaNo()).startsWith("SOA-"));
    assertThat(soas.get(0).getBalance()).isEqualByComparingTo(plan.installment(1).getAmount());
    assertThat(statements.get(soas.get(2).getId()).getLines())
        .extracting(l -> l.getKind().name())
        .contains("CURRENT");
    assertThat(statements.forPlan(plan.getId())).hasSize(3);
    byte[] pdf = statements.document(soas.get(0).getId()).getContent();
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    assertThat(statements.get(soas.get(0).getId()).getTemplateCode()).isEqualTo("CLX_SOA");

    assertThatThrownBy(() -> as.run(HANDLER, () -> statements.generate(plan.getId(), 1)))
        .extracting("code")
        .isEqualTo("CLX_SOA_EXISTS");
    BillingStatement cancelled =
        as.run(HANDLER, () -> statements.cancel(soas.get(0).getId(), "Wrong address"));
    assertThat(cancelled.getStatus()).isEqualTo(StatementStatus.CANCELLED);
    BillingStatement again = as.run(HANDLER, () -> statements.generate(plan.getId(), 1));
    assertThat(again.getSoaNo()).isNotEqualTo(soas.get(0).getSoaNo());

    BillingStatement sent =
        as.run(
            HANDLER,
            () ->
                dispatch.send(
                    again.getId(),
                    new SoaDispatch.Mail(
                        List.of("client@example.com"), List.of(), "Your SOA", "Please pay")));
    assertThat(sent.getStatus()).isEqualTo(StatementStatus.SENT);
    assertThat(sent.getSentTo()).isEqualTo("client@example.com");
    assertThat(messages.forRecord(BillingStatementService.ENTITY, String.valueOf(again.getId())))
        .isNotEmpty();
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        dispatch.send(
                            again.getId(), new SoaDispatch.Mail(List.of(), null, "s", "b"))))
        .extracting("code")
        .isEqualTo("CLX_SOA_RECIPIENT");
    assertThat(as.run(HANDLER, () -> dispatch.clientEmail(again.getId()))).isNotNull();

    fx.pay(year1.getInvoiceNo(), "1000.00", LocalDate.now());
    InstallmentPlan refreshed = as.run(HANDLER, () -> plans.refresh(plan.getId()));
    assertThat(refreshed.installment(1).getPaidAmount()).isEqualByComparingTo("1000.00");
    assertThat(refreshed.getPaidTotal()).isEqualByComparingTo("1000.00");
    assertThat(plans.forAccount(year1.getArn())).hasSize(1);
    assertThat(
            statements
                .search(company, List.of(), null, null, year1.getArn(), PageRequest.of(0, 10))
                .getTotalElements())
        .isEqualTo(4);
  }

  @Test
  void aGeneratedPlanAllocatesPaymentsOldestDueFirstAndFlagsOverdue() {
    OpsInvoice invoice = fx.motorInvoice();
    Long company = fx.company();
    BigDecimal outstanding = invoice.premiumBalance();
    LocalDate firstDue = LocalDate.of(2026, 6, 1);
    InstallmentPlan plan =
        as.run(
            HANDLER,
            () -> plans.generate(company, invoice.getInvoiceNo(), "QUARTERLY", firstDue, 4, "q"));
    assertThat(plan.getTotal()).isEqualByComparingTo(outstanding);
    assertThat(plan.getInstallments())
        .extracting(Installment::getDueDate)
        .containsExactly(
            firstDue, firstDue.plusMonths(3), firstDue.plusMonths(6), firstDue.plusMonths(9));
    assertThat(plan.installment(1).getStatus()).isEqualTo(InstallmentStatus.OVERDUE);

    BigDecimal first = plan.installment(1).getAmount();
    fx.pay(invoice.getInvoiceNo(), first.add(d("100.00")).toPlainString(), LocalDate.now());
    InstallmentPlan allocated = as.run(HANDLER, () -> plans.refresh(plan.getId()));
    assertThat(allocated.installment(1).getStatus()).isEqualTo(InstallmentStatus.PAID);
    assertThat(allocated.installment(2).getPaidAmount()).isEqualByComparingTo("100.00");
    assertThat(allocated.installment(2).getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    assertThat(allocated.installment(3).getPaidAmount()).isEqualByComparingTo("0");

    List<BillingStatement> run =
        as
            .run(HANDLER, () -> statements.generateDue(company, firstDue, firstDue.plusMonths(3)))
            .stream()
            .filter(s -> s.getPlanId().equals(plan.getId()))
            .toList();
    assertThat(run).extracting(BillingStatement::getCycleSeq).containsExactly(1, 2);
    assertThat(
            as
                .run(
                    HANDLER,
                    () -> statements.generateDue(company, firstDue, firstDue.plusMonths(3)))
                .stream()
                .filter(s -> s.getPlanId().equals(plan.getId())))
        .isEmpty();
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () -> statements.generateDue(company, firstDue, firstDue.minusDays(1))))
        .extracting("code")
        .isEqualTo("CLX_SOA_PERIOD");
    assertThat(
            plans
                .search(
                    company,
                    List.of(PlanStatus.ACTIVE),
                    invoice.getInvoiceNo(),
                    PageRequest.of(0, 5))
                .getContent())
        .extracting(InstallmentPlan::getId)
        .containsExactly(plan.getId());
    as.run(
        HANDLER,
        () -> {
          plans.refresh(plan.getId(), LocalDate.now());
          return plan.getId();
        });

    InstallmentPlan cancelled = as.run(HANDLER, () -> plans.cancel(plan.getId(), "Replaced"));
    assertThat(cancelled.getStatus()).isEqualTo(PlanStatus.CANCELLED);
    assertThatThrownBy(() -> as.run(HANDLER, () -> statements.generate(plan.getId(), 3)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_NOT_ACTIVE");

    BigDecimal left = fx.invoice(invoice.getInvoiceNo()).premiumBalance();
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.manual(
                            company,
                            invoice.getInvoiceNo(),
                            "MONTHLY",
                            List.of(new ManualEntry(LocalDate.now(), d("1.00"))),
                            null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_TOTAL_MISMATCH");
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.manual(
                            company,
                            invoice.getInvoiceNo(),
                            "MONTHLY",
                            List.of(
                                new ManualEntry(LocalDate.now(), d("1.00")),
                                new ManualEntry(LocalDate.now().minusDays(1), d("1.00"))),
                            null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_ENTRY_INVALID");
    BigDecimal half = left.divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
    InstallmentPlan manual =
        as.run(
            HANDLER,
            () ->
                plans.manual(
                    company,
                    invoice.getInvoiceNo(),
                    "MONTHLY",
                    List.of(
                        new ManualEntry(LocalDate.now().plusDays(30), half),
                        new ManualEntry(LocalDate.now().plusDays(60), left.subtract(half))),
                    "agreed by phone"));
    assertThat(manual.getSource()).isEqualTo(PlanSource.MANUAL);
    assertThat(manual.installment(1).getCycleTo()).isEqualTo(LocalDate.now().plusDays(59));
  }

  @Test
  void onlyOpenReceivablesCanBeScheduled() {
    Long company = fx.company();
    OpsInvoice direct = fx.directPaymentInvoice();
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.generate(
                            company, direct.getInvoiceNo(), "QUARTERLY", LocalDate.now(), 2, null)))
        .extracting("code")
        .isEqualTo("CLX_INVOICE_NOT_COLLECTIBLE");
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.generate(
                            company, "BI-NONE-1", "QUARTERLY", LocalDate.now(), 2, null)))
        .extracting("code")
        .isEqualTo("CLX_INVOICE_UNKNOWN");
    OpsInvoice paid = fx.motorInvoice();
    fx.payInFull(paid.getInvoiceNo(), LocalDate.now());
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.generate(
                            company, paid.getInvoiceNo(), "QUARTERLY", LocalDate.now(), 2, null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_NOTHING_DUE");
    assertThatThrownBy(
            () ->
                as.run(
                    HANDLER,
                    () ->
                        plans.fromPolicyYears(
                            company, "ARN-NONE-" + CollectionsFixtures.token(), "ANNUAL", null)))
        .extracting("code")
        .isEqualTo("CLX_PLAN_NOT_BOOKED");
  }
}
