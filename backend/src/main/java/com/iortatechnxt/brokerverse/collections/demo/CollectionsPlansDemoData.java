package com.iortatechnxt.brokerverse.collections.demo;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.service.BillingStatementService;
import com.iortatechnxt.brokerverse.collections.billing.service.SoaDispatch;
import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkActions;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationJob;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService.ManualEscalation;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlanRepository;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demo storyline of the Collections plans and escalations (wave C1-B, demo profile only,
 * idempotent), each step signed in as the demo user whose job it is:
 *
 * <ul>
 *   <li>the collection handler ({@code mktcoll}) makes an annual policy-year plan for the
 *       three-year demo account and generates its statement of account for each of the three
 *       billing cycles, e-mailing the first (BRCLXN.058);
 *   <li>a quarterly plan for ARN-2026-940002, whose first installment is overdue (BRCLXN.053);
 *   <li>a promise kept on ARN-2026-940004 (paid by the cashiering demo), a promise running on the
 *       quarterly plan and a promise broken on the three-year account, which escalates it to the
 *       team lead {@code mkttl} (BRCLXN.055/049);
 *   <li>the job CLX_ESCALATION escalates the overdue installment; the handler escalates
 *       ARN-2026-940004 manually; the team lead acknowledges the broken-promise escalation
 *       (BRCLXN.049/050).
 * </ul>
 *
 * <p>A step that fails is logged and skipped. The escalation rules are seeded by V1901.
 */
@Component
@Profile("demo")
@Order(110)
public class CollectionsPlansDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CollectionsPlansDemoData.class);
  private static final String HANDLER = "mktcoll";
  private static final String TEAM_LEAD = "mkttl";
  private static final String QUARTERLY_ARN = "ARN-2026-940002";
  private static final String PAID_ARN = "ARN-2026-940004";
  private static final String BROKEN_PROMISE_RULE = "CLX-BROKEN-PROMISE";
  private static final int CYCLES = 3;
  private static final int QUARTERS = 4;
  private static final int BROKEN_PROMISE_DAYS = 7;
  private static final int PROMISE_WINDOW = 5;
  private static final int RUNNING_PROMISE_DAYS = 10;
  private static final BigDecimal BROKEN_AMOUNT = new BigDecimal("10000.00");

  private final InstallmentPlanRepository planRecords;
  private final InstallmentPlanService plans;
  private final BillingStatementService statements;
  private final SoaDispatch dispatch;
  private final PromiseService promises;
  private final CollectionsBulkActions bulk;
  private final EscalationService escalations;
  private final EscalationJob escalationJob;
  private final WorkflowService workflow;
  private final InvoiceLedgerQueryService ledger;
  private final DemoMultiYearAccount account;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param planRecords plans (idempotency)
   * @param plans installment plans
   * @param statements statements of account
   * @param dispatch statement e-mail
   * @param promises promises to pay
   * @param bulk manual escalation
   * @param escalations escalations
   * @param escalationJob job CLX_ESCALATION
   * @param workflow acknowledgement
   * @param ledger invoice ledger
   * @param account the three-year demo account
   * @param users demo sign-in
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CollectionsPlansDemoData(
      InstallmentPlanRepository planRecords,
      InstallmentPlanService plans,
      BillingStatementService statements,
      SoaDispatch dispatch,
      PromiseService promises,
      CollectionsBulkActions bulk,
      EscalationService escalations,
      EscalationJob escalationJob,
      WorkflowService workflow,
      InvoiceLedgerQueryService ledger,
      DemoMultiYearAccount account,
      DemoUsers users,
      Clock clock) {
    this.planRecords = planRecords;
    this.plans = plans;
    this.statements = statements;
    this.dispatch = dispatch;
    this.promises = promises;
    this.bulk = bulk;
    this.escalations = escalations;
    this.escalationJob = escalationJob;
    this.workflow = workflow;
    this.ledger = ledger;
    this.account = account;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (planRecords.count() == 0) {
      bookingInvoice(QUARTERLY_ARN)
          .ifPresentOrElse(
              this::load,
              () -> LOG.warn("Collections plans demo skipped: {} is not booked", QUARTERLY_ARN));
    }
  }

  private void load(OpsInvoice quarterly) {
    Long companyId = quarterly.getCompanyId();
    step("three-year account", () -> threeYearAccount(companyId));
    step("quarterly plan", () -> quarterlyPlan(companyId, quarterly));
    step("kept promise", () -> keptPromise(companyId));
    step("escalations", () -> escalate(companyId));
    LOG.info("Collections plans demo data: plans, statements, promises and escalations loaded");
  }

  private void threeYearAccount(Long companyId) {
    BookedInvoice year1 = account.book(companyId);
    InstallmentPlan plan =
        users.as(
            HANDLER,
            () ->
                plans.fromPolicyYears(
                    companyId,
                    year1.getArn(),
                    "ANNUAL",
                    "Demo: annual billing of the three-year property policy"));
    for (int cycle = 1; cycle <= CYCLES; cycle++) {
      int seq = cycle;
      BillingStatement soa = users.as(HANDLER, () -> statements.generate(plan.getId(), seq));
      if (seq == 1) {
        sendFirst(soa);
      }
    }
    LocalDate today = LocalDate.now(clock);
    PaymentPromise promise =
        users.as(
            HANDLER,
            () ->
                promises.record(
                    companyId,
                    year1.getInvoiceNo(),
                    new PromiseInput(
                        today.minusDays(BROKEN_PROMISE_DAYS),
                        today.minusDays(2),
                        BROKEN_AMOUNT,
                        plan.getInstallments().get(0).getId(),
                        "Client promised a first payment by phone"),
                    null));
    users.run(HANDLER, () -> promises.evaluate(promise.getId(), today));
  }

  private void sendFirst(BillingStatement soa) {
    users.run(
        HANDLER,
        () -> {
          String to = dispatch.clientEmail(soa.getId()).orElse("billing@brokerverse-demo.ph");
          dispatch.send(
              soa.getId(),
              new SoaDispatch.Mail(
                  List.of(to),
                  List.of(),
                  "Statement of account " + soa.getSoaNo(),
                  "Please find attached your statement of account for the billing cycle "
                      + soa.getCycleFrom()
                      + " to "
                      + soa.getCycleTo()
                      + "."));
        });
  }

  private void quarterlyPlan(Long companyId, OpsInvoice invoice) {
    InstallmentPlan plan =
        users.as(
            HANDLER,
            () ->
                plans.generate(
                    companyId,
                    invoice.getInvoiceNo(),
                    "QUARTERLY",
                    invoice.getClassification().inceptionDate(),
                    QUARTERS,
                    "Demo: quarterly installments agreed with the client"));
    Installment second = plan.getInstallments().get(1);
    LocalDate today = LocalDate.now(clock);
    users.run(
        HANDLER,
        () ->
            promises.record(
                companyId,
                invoice.getInvoiceNo(),
                new PromiseInput(
                    today,
                    today.plusDays(RUNNING_PROMISE_DAYS),
                    second.getAmount(),
                    second.getId(),
                    "Client will pay the second installment early"),
                null));
  }

  private void keptPromise(Long companyId) {
    OpsInvoice invoice = bookingInvoice(PAID_ARN).orElseThrow();
    List<OpsInvoiceMovement> applied =
        ledger.movements(invoice.getInvoiceNo()).stream()
            .filter(m -> m.getMovementType() == MovementType.APPLIED)
            .filter(m -> m.getComponent().isPremiumReceivable())
            .toList();
    if (applied.isEmpty()) {
      return;
    }
    LocalDate paidOn =
        applied.stream()
            .map(OpsInvoiceMovement::getValueDate)
            .max(LocalDate::compareTo)
            .orElseThrow();
    BigDecimal paid =
        applied.stream()
            .map(OpsInvoiceMovement::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .min(invoice.premiumBalance());
    PaymentPromise promise =
        users.as(
            HANDLER,
            () ->
                promises.record(
                    companyId,
                    invoice.getInvoiceNo(),
                    new PromiseInput(
                        paidOn.minusDays(PROMISE_WINDOW),
                        paidOn,
                        paid,
                        null,
                        "Client promised a partial payment by the end of the week"),
                    null));
    users.run(HANDLER, () -> promises.evaluate(promise.getId(), LocalDate.now(clock)));
  }

  private void escalate(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    users.run("admin", () -> escalationJob.execute(today));
    bookingInvoice(PAID_ARN)
        .ifPresent(
            invoice ->
                users.run(
                    HANDLER,
                    () ->
                        bulk.escalate(
                            new ManualEscalation(
                                companyId,
                                List.of(invoice.getInvoiceNo()),
                                TargetLevel.TL,
                                null,
                                "NO_COMMITMENT",
                                "Client stopped answering after the partial payment"))));
    planRecords.findAll().stream()
        .filter(p -> p.getInvoiceNo() == null)
        .findFirst()
        .flatMap(
            p ->
                escalations.forInvoice(firstInvoiceOf(p)).stream()
                    .filter(e -> BROKEN_PROMISE_RULE.equals(e.getRuleCode()))
                    .findFirst())
        .ifPresent(
            e ->
                users.run(
                    TEAM_LEAD,
                    () ->
                        workflow.transition(
                            EscalationService.ENTITY,
                            String.valueOf(e.getId()),
                            "acknowledge",
                            TransitionNote.comment("Calling the client today"))));
  }

  private String firstInvoiceOf(InstallmentPlan plan) {
    return plans.get(plan.getId()).getInstallments().get(0).getInvoiceNo();
  }

  private Optional<OpsInvoice> bookingInvoice(String arn) {
    return ledger.forArn(arn).stream()
        .filter(i -> i.getKind() == InvoiceKind.BOOKING && i.getPolicyYear() == 1)
        .findFirst();
  }

  private static void step(String name, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException ex) {
      LOG.warn("Collections plans demo step '{}' skipped: {}", name, ex.getMessage());
    }
  }
}
