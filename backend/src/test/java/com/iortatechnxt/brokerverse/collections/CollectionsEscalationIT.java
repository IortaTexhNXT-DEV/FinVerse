package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkActions;
import com.iortatechnxt.brokerverse.collections.bulk.service.ItemResult;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Kind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule.Filters;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationApprovalSource;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationEngine;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationOverdueCheck;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationRuleService;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService.ManualEscalation;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseCheckJob;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Promises to pay and escalations (BRCLXN.049/050/051/055): a broken promise escalates to the team
 * lead, kept and partly kept promises, manual and bulk escalation, the escalation workflow up to
 * the automatic close of a collected account, maker-checker rules with their preview, the bulk
 * update handler and the reports.
 */
@IntegrationTest
class CollectionsEscalationIT {

  private static final String HANDLER = "mktcoll";
  private static final String TEAM_LEAD = "mkttl";
  private static final LocalDate TODAY = LocalDate.now();

  @Autowired private CollectionsFixtures fx;
  @Autowired private PromiseService promises;
  @Autowired private PromiseCheckJob promiseJob;
  @Autowired private EscalationService escalations;
  @Autowired private EscalationEngine engine;
  @Autowired private EscalationRuleService rules;
  @Autowired private EscalationApprovalSource approvals;
  @Autowired private EscalationOverdueCheck overdue;
  @Autowired private CollectionsBulkActions bulk;
  @Autowired private InstallmentPlanService plans;
  @Autowired private WorkflowService workflow;
  @Autowired private WorkflowViewService views;
  @Autowired private ReportService reports;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  private PaymentPromise promise(String invoiceNo, PromiseInput input) {
    return as.run(HANDLER, () -> promises.record(fx.company(), invoiceNo, input, null));
  }

  @Test
  void aBrokenPromiseEscalatesToTheTeamLead() {
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    PaymentPromise promise =
        promise(
            no,
            new PromiseInput(TODAY.minusDays(6), TODAY.minusDays(2), d("500.00"), null, "call"));
    assertThat(promise.getStatus()).isEqualTo(PromiseStatus.OPEN);
    assertThat(as.run("admin", () -> promises.dueForEvaluation(TODAY))).contains(promise.getId());

    promiseJob.execute(TODAY);

    assertThat(promises.get(promise.getId()).getStatus()).isEqualTo(PromiseStatus.BROKEN);
    List<Escalation> raised = escalations.forInvoice(no);
    assertThat(raised).hasSize(1);
    Escalation escalation = raised.get(0);
    assertThat(escalation.getKind()).isEqualTo(Kind.AUTO);
    assertThat(escalation.getRuleCode()).isEqualTo("CLX-BROKEN-PROMISE");
    assertThat(escalation.getStatus()).isEqualTo(Stage.WITH_TL);
    assertThat(escalation.getTargetUsername()).isEqualTo(TEAM_LEAD);
    assertThat(escalation.getItems()).extracting(i -> i.getInvoiceNo()).containsExactly(no);
    assertThat(
            views
                .view(EscalationService.ENTITY, String.valueOf(escalation.getId()))
                .orElseThrow()
                .workCase()
                .getAssignee())
        .isEqualTo(TEAM_LEAD);
    assertThat(notifications(TEAM_LEAD, escalation.getId())).isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and entity_type = ?"
                    + " and entity_id = ?",
                Long.class,
                HANDLER,
                PromiseService.ENTITY,
                String.valueOf(promise.getId())))
        .isPositive();

    // the same broken promise does not escalate twice
    promiseJob.execute(TODAY);
    assertThat(escalations.forInvoice(no)).hasSize(1);
    assertThatThrownBy(() -> as.run(HANDLER, () -> promises.cancel(promise.getId(), "late")))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_CLOSED");
  }

  private long notifications(String user, Long escalationId) {
    Long n =
        jdbc.queryForObject(
            "select count(*) from msg_notification where recipient = ? and entity_type = ?"
                + " and entity_id = ?",
            Long.class,
            user,
            EscalationService.ENTITY,
            String.valueOf(escalationId));
    return n == null ? 0 : n;
  }

  @Test
  void promisesAreKeptPartlyKeptSupersededAndWithdrawn() {
    OpsInvoice kept = fx.motorInvoice();
    PaymentPromise p1 =
        promise(
            kept.getInvoiceNo(),
            new PromiseInput(TODAY.minusDays(3), TODAY.minusDays(1), d("300.00"), null, null));
    fx.pay(kept.getInvoiceNo(), "300.00", TODAY.minusDays(2));
    assertThat(as.run("admin", () -> promises.evaluate(p1.getId(), TODAY)).getStatus())
        .isEqualTo(PromiseStatus.KEPT);
    assertThat(promises.get(p1.getId()).getActualPaid()).isEqualByComparingTo("300.00");

    OpsInvoice partly = fx.motorInvoice();
    PaymentPromise p2 =
        promise(
            partly.getInvoiceNo(),
            new PromiseInput(TODAY.minusDays(3), TODAY.minusDays(1), d("1000.00"), null, null));
    fx.pay(partly.getInvoiceNo(), "400.00", TODAY.minusDays(1));
    fx.pay(partly.getInvoiceNo(), "50.00", TODAY);
    // a new promise evaluates the past one first
    PaymentPromise p3 =
        promise(
            partly.getInvoiceNo(), new PromiseInput(null, TODAY.plusDays(10), null, null, null));
    assertThat(promises.get(p2.getId()).getStatus()).isEqualTo(PromiseStatus.PARTIALLY_KEPT);
    assertThat(promises.get(p2.getId()).getActualPaid()).isEqualByComparingTo("400.00");
    assertThat(p3.getPromisedAmount())
        .isEqualByComparingTo(fx.invoice(partly.getInvoiceNo()).premiumBalance());
    PaymentPromise p4 =
        promise(
            partly.getInvoiceNo(), new PromiseInput(null, TODAY.plusDays(20), d("10"), null, null));
    assertThat(promises.get(p3.getId()).getStatus()).isEqualTo(PromiseStatus.CANCELLED);
    assertThat(promises.get(p3.getId()).getClosingNote()).contains("Superseded");
    assertThat(as.run(HANDLER, () -> promises.cancel(p4.getId(), "Withdrawn")).getStatus())
        .isEqualTo(PromiseStatus.CANCELLED);
    assertThat(promises.forInvoice(partly.getInvoiceNo())).hasSize(3);
    assertThat(
            promises
                .search(
                    fx.company(),
                    List.of(PromiseStatus.PARTIALLY_KEPT),
                    TODAY.minusDays(5),
                    TODAY,
                    partly.getInvoiceNo(),
                    PageRequest.of(0, 5))
                .getContent())
        .extracting(PaymentPromise::getId)
        .containsExactly(p2.getId());
    // not yet due: nothing happens
    assertThat(as.run("admin", () -> promises.evaluate(p1.getId(), TODAY)).getStatus())
        .isEqualTo(PromiseStatus.KEPT);

    String no = partly.getInvoiceNo();
    assertThatThrownBy(
            () ->
                promise(
                    no, new PromiseInput(TODAY.plusDays(1), TODAY.plusDays(2), null, null, null)))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_DATES");
    assertThatThrownBy(
            () -> promise(no, new PromiseInput(null, TODAY, d("99999999.00"), null, null)))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_OVER_BALANCE");
    OpsInvoice other = fx.motorInvoice();
    InstallmentPlan plan =
        as.run(
            HANDLER,
            () -> plans.generate(fx.company(), other.getInvoiceNo(), "MONTHLY", TODAY, 2, null));
    Long installment = plan.getInstallments().get(0).getId();
    assertThatThrownBy(() -> promise(no, new PromiseInput(null, TODAY, null, installment, null)))
        .extracting("code")
        .isEqualTo("CLX_PROMISE_INSTALLMENT");
    PaymentPromise onInstallment =
        promise(other.getInvoiceNo(), new PromiseInput(null, TODAY, d("5.00"), installment, null));
    assertThat(onInstallment.getInstallmentId()).isEqualTo(installment);
  }

  @Test
  void invoicesAreEscalatedManuallyWorkedAndClosedWhenCollected() {
    OpsInvoice first = fx.motorInvoice();
    OpsInvoice second = fx.motorInvoice();
    OpsInvoice direct = fx.directPaymentInvoice();
    List<ItemResult> results =
        as.run(
            HANDLER,
            () ->
                bulk.escalate(
                    new ManualEscalation(
                        fx.company(),
                        List.of(
                            first.getInvoiceNo(),
                            second.getInvoiceNo(),
                            "BI-NOPE-" + CollectionsFixtures.token(),
                            direct.getInvoiceNo()),
                        TargetLevel.TL,
                        TEAM_LEAD,
                        "NO_COMMITMENT",
                        "Client not answering")));
    assertThat(results).filteredOn(ItemResult::ok).hasSize(2);
    assertThat(results).filteredOn(r -> !r.ok()).hasSize(2);

    Escalation e1 = escalations.forInvoice(first.getInvoiceNo()).get(0);
    Escalation e2 = escalations.forInvoice(second.getInvoiceNo()).get(0);
    assertThat(e1.getKind()).isEqualTo(Kind.MANUAL);
    assertThat(e1.getStatus()).isEqualTo(Stage.WITH_TL);
    assertThat(e1.getBulkRef()).isNotNull().isEqualTo(e2.getBulkRef());
    assertThat(e1.getCreatedBy()).isEqualTo(HANDLER);
    String id = String.valueOf(e1.getId());

    as.run(
        TEAM_LEAD,
        () ->
            workflow.transition(EscalationService.ENTITY, id, "acknowledge", TransitionNote.NONE));
    assertThat(escalations.get(e1.getId()).getStatus()).isEqualTo(Stage.IN_ACTION);
    assertThatThrownBy(
            () ->
                as.run(
                    TEAM_LEAD, () -> escalations.act(e1.getId(), "resolve", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("CLX_RESOLUTION_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(TEAM_LEAD, () -> escalations.act(e1.getId(), "route", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("CLX_ESCALATION_ACTION");
    assertThatThrownBy(
            () ->
                as.run(
                    TEAM_LEAD,
                    () -> escalations.act(e1.getId(), "escalate_further", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
    Escalation further =
        as.run(
            TEAM_LEAD,
            () ->
                escalations.act(
                    e1.getId(), "escalate_further", new TransitionNote("AGING", "Over 90 days")));
    assertThat(further.getStatus()).isEqualTo(Stage.WITH_UH);
    assertThat(further.getTargetLevel()).isEqualTo(TargetLevel.UH);
    as.run(
        TEAM_LEAD,
        () ->
            workflow.transition(
                EscalationService.ENTITY,
                id,
                "return_to_handler",
                new TransitionNote("INCOMPLETE_DETAILS", "Add the call log")));
    assertThat(escalations.get(e1.getId()).getStatus()).isEqualTo(Stage.RETURNED);
    as.run(HANDLER, () -> escalations.act(e1.getId(), "resubmit", TransitionNote.comment("Added")));
    assertThat(escalations.get(e1.getId()).getStatus()).isEqualTo(Stage.WITH_TL);
    as.run(
        TEAM_LEAD,
        () ->
            workflow.transition(EscalationService.ENTITY, id, "acknowledge", TransitionNote.NONE));
    Escalation resolved =
        as.run(
            TEAM_LEAD,
            () ->
                escalations.act(
                    e1.getId(), "resolve", TransitionNote.comment("Payment arrangement agreed")));
    assertThat(resolved.getStatus()).isEqualTo(Stage.RESOLVED);
    assertThat(resolved.getResolution()).isEqualTo("Payment arrangement agreed");
    assertThat(as.run("admin", () -> escalations.autoCloseIfCollected(e1.getId()))).isFalse();

    assertThat(as.run("admin", () -> escalations.autoCloseIfCollected(e2.getId()))).isFalse();
    fx.payInFull(second.getInvoiceNo(), TODAY);
    assertThat(as.run("admin", () -> escalations.autoCloseIfCollected(e2.getId()))).isTrue();
    assertThat(escalations.get(e2.getId()).getStatus()).isEqualTo(Stage.RESOLVED);
    assertThat(
            escalations
                .search(
                    fx.company(),
                    List.of(Stage.RESOLVED),
                    second.getInvoiceNo(),
                    PageRequest.of(0, 5))
                .getContent())
        .extracting(Escalation::getId)
        .containsExactly(e2.getId());

    List<ItemResult> refused =
        as.run(
            HANDLER,
            () ->
                bulk.escalate(
                    new ManualEscalation(
                        fx.company(),
                        List.of(first.getInvoiceNo()),
                        TargetLevel.USER,
                        null,
                        "AGING",
                        null)));
    assertThat(refused).singleElement().extracting(ItemResult::ok).isEqualTo(false);
    List<ItemResult> notHandler =
        as.run(
            HANDLER,
            () ->
                bulk.escalate(
                    new ManualEscalation(
                        fx.company(),
                        List.of(first.getInvoiceNo()),
                        TargetLevel.USER,
                        "ao",
                        "AGING",
                        null)));
    assertThat(notHandler.get(0).message()).contains("does not handle escalations");
    List<ItemResult> head =
        as.run(
            HANDLER,
            () ->
                bulk.escalate(
                    new ManualEscalation(
                        fx.company(),
                        List.of(first.getInvoiceNo()),
                        TargetLevel.SECTION_HEAD,
                        null,
                        "OTHERS",
                        "Large account")));
    assertThat(head.get(0).ok()).isTrue();
    assertThat(escalations.forInvoice(first.getInvoiceNo()).get(0).getStatus())
        .isEqualTo(Stage.WITH_UH);
  }

  @Test
  void rulesAreMakerCheckedPreviewedAndRaiseOneEscalationPerMonth() {
    Long company = fx.company();
    OpsInvoice invoice = fx.motorInvoice();
    InstallmentPlan plan =
        as.run(
            HANDLER,
            () ->
                plans.generate(
                    company, invoice.getInvoiceNo(), "MONTHLY", TODAY.minusDays(40), 3, null));
    assertThat(plan.installment(1).getStatus().name()).isEqualTo("OVERDUE");
    String code = "T-INST-" + CollectionsFixtures.token();
    EscalationRule rule =
        as.run(
            "badmin",
            () ->
                rules.create(
                    company,
                    code.toLowerCase(Locale.ROOT),
                    terms(Basis.INSTALLMENT_OVERDUE_DAYS, "30", TargetLevel.TL, null)));
    assertThat(rule.getCode()).isEqualTo(code);
    assertThat(rule.getRecordStatus().name()).isEqualTo("PENDING_AUTHORIZATION");
    assertThat(approvals.pendingFor(ApprovalViewer.system()))
        .extracting(PendingApproval::reference)
        .contains(code);
    assertThat(engine.rulesOn(TODAY)).extracting(EscalationRule::getCode).doesNotContain(code);
    assertThatThrownBy(() -> as.run("badmin", () -> rules.authorize(rule.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rules.create(
                            company, code, terms(Basis.AMOUNT_OVER, "1", TargetLevel.TL, null))))
        .isInstanceOf(DuplicateResourceException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rules.create(
                            company,
                            code + "X",
                            terms(Basis.AMOUNT_OVER, "1", TargetLevel.USER, "ao"))))
        .extracting("code")
        .isEqualTo("CLX_ESCALATION_TARGET");
    as.run("approver", () -> rules.authorize(rule.getId()));
    assertThat(engine.rulesOn(TODAY)).extracting(EscalationRule::getCode).contains(code);

    EscalationRule active = rules.get(rule.getId());
    List<Candidate> matches = as.run("badmin", () -> engine.matches(active, TODAY));
    Candidate mine =
        matches.stream()
            .filter(c -> c.invoiceNo().equals(invoice.getInvoiceNo()))
            .findFirst()
            .orElseThrow();
    assertThat(as.run("admin", () -> escalations.raiseForRule(active, mine, TODAY))).isPresent();
    assertThat(as.run("admin", () -> escalations.raiseForRule(active, mine, TODAY))).isEmpty();
    Escalation auto = escalations.forInvoice(invoice.getInvoiceNo()).get(0);
    assertThat(auto.getRuleCode()).isEqualTo(code);
    assertThat(auto.getStatus()).isEqualTo(Stage.WITH_TL);
    assertThat(auto.getTargetUsername()).isNull();
    assertThat(notifications(TEAM_LEAD, auto.getId())).isPositive();

    // a later payment and a lower threshold: the other bases are evaluated too
    assertThat(
            as.run(
                "badmin",
                () ->
                    engine.matches(
                        new EscalationRule(
                            company,
                            "PREVIEW",
                            terms(Basis.NO_COMMITMENT_BY_DAY, "1", TargetLevel.TL, null)),
                        TODAY.plusDays(30))))
        .extracting(Candidate::invoiceNo)
        .contains(invoice.getInvoiceNo());
    assertThat(
            as.run(
                "badmin",
                () ->
                    engine.matches(
                        new EscalationRule(
                            company,
                            "PREVIEW2",
                            terms(Basis.BROKEN_PROMISES_COUNT, "1", TargetLevel.TL, null)),
                        TODAY)))
        .extracting(Candidate::invoiceNo)
        .doesNotContain(invoice.getInvoiceNo());

    EscalationRule changed =
        as.run(
            "badmin",
            () ->
                rules.update(
                    rule.getId(),
                    terms(Basis.INSTALLMENT_OVERDUE_DAYS, "60", TargetLevel.UH, null)));
    assertThat(changed.getRecordStatus().name()).isEqualTo("PENDING_AUTHORIZATION");
    assertThat(as.run("badmin", () -> rules.deactivate(rule.getId())).getRecordStatus().name())
        .isEqualTo("INACTIVE");
    assertThat(rules.list(company)).extracting(EscalationRule::getCode).contains(code);
    assertThat(overdue.evaluate(TODAY)).isNotNull();
  }

  private static EscalationRule.Terms terms(
      Basis basis, String threshold, TargetLevel level, String user) {
    return new EscalationRule.Terms(
        "Test rule",
        basis,
        new BigDecimal(threshold),
        new Filters("CBG", null, null, null, null),
        level,
        user,
        "INSTALLMENT_OVERDUE",
        24,
        true,
        TODAY.minusDays(1),
        null);
  }

  @Test
  void oneBulkPromiseIsRecordedPerInvoiceWithItsOwnOutcome() {
    OpsInvoice a = fx.motorInvoice();
    OpsInvoice b = fx.motorInvoice();
    List<ItemResult> results =
        as.run(
            TEAM_LEAD,
            () ->
                bulk.promise(
                    fx.company(),
                    List.of(a.getInvoiceNo(), b.getInvoiceNo(), " ", a.getInvoiceNo(), "BI-NOPE"),
                    new PromiseInput(null, TODAY.plusDays(7), d("100.00"), null, "bulk")));
    assertThat(results).hasSize(3);
    assertThat(results).filteredOn(ItemResult::ok).hasSize(2);
    PaymentPromise pa = promises.forInvoice(a.getInvoiceNo()).get(0);
    PaymentPromise pb = promises.forInvoice(b.getInvoiceNo()).get(0);
    assertThat(pa.getBulkRef()).isNotNull().isEqualTo(pb.getBulkRef());
  }

  @Test
  void theCollectionsReportsRunAndExport() {
    Map<String, String> params =
        Map.of(
            "companyId",
            String.valueOf(fx.company()),
            "from",
            TODAY.minusYears(1).toString(),
            "to",
            TODAY.plusYears(3).toString());
    for (String code : List.of("CLX-ESCALATIONS", "CLX-BROKEN-PROMISES", "CLX-INSTALLMENTS-DUE")) {
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(as.run(TEAM_LEAD, () -> reports.export(code, params, format)).content())
            .isNotEmpty();
      }
    }
    Map<String, String> open = new HashMap<>(params);
    open.put("status", "OPEN");
    assertThat(as.run(TEAM_LEAD, () -> reports.run("CLX-ESCALATIONS", open))).isNotNull();
  }
}
