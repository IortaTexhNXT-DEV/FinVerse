package com.iortatechnxt.brokerverse.payrequest;

import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.PROCESSOR;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.REVIEWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CaseOutcome;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.service.CaseService;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringRefundValidationSource;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.RefundValidations;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundValidation;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.ValidationStatus;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RefundValidationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Validation of refunds of cancelled policies (MKT 1.11.0, ACSL 2.5.5): the refund goes to ACSL (an
 * analysis case through the port) and Cashiering (a validation task of its own adapter), both
 * answers are needed before review, and a rejection sends it back to the preparer.
 */
@IntegrationTest
class RefundValidationIT {

  @Autowired private PayRequestFixtures fx;
  @Autowired private PayRequestWorkflowService workflow;
  @Autowired private RefundValidationService validations;
  @Autowired private RefundValidations router;
  @Autowired private CaseService cases;
  @Autowired private AcslCaseRepository caseRepository;
  @Autowired private WorkflowService workCases;
  @Autowired private WorkflowViewService views;
  @Autowired private CashieringRefundValidationSource cashieringChecks;
  @Autowired private CashFixtures cash;
  @Autowired private AsUser as;

  private PaymentRequest submittedForValidation() {
    PaymentRequest r =
        fx.raise(
            PayRequestFixtures.refund(
                PayRequestFixtures.line(fx.invoice(), "CANCELLED_POLICY", "300.00")));
    assertThat(r.isValidationRequired()).isTrue();
    as.run(PROCESSOR, () -> workflow.submit(r.getId(), "Cancelled policy refund"));
    return fx.reload(r);
  }

  private AcslCase investigate(RefundValidation acsl) {
    AcslCase c = caseRepository.findByCaseNo(acsl.getTicketRef()).orElseThrow();
    assertThat(c.getCaseType()).isEqualTo(CaseType.ANALYSIS_REQUEST);
    assertThat(c.getRequesterModule()).isEqualTo("PAYREQUEST");
    as.run("acsltl", () -> cases.assign(c.getId(), "acsl", "Please check"));
    Long caseId =
        views.view("AcslCase", String.valueOf(c.getId())).orElseThrow().workCase().getId();
    as.run("acsl", () -> workCases.genericTransition(caseId, "start", TransitionNote.NONE));
    as.run("acsl", () -> cases.recordFindings(c.getId(), "Premium cancelled, insurer returned it"));
    return c;
  }

  @Test
  void acslAndCashieringConfirmThenTheRefundGoesToReview() {
    assertThat(router.installed()).contains("ACSL");
    PaymentRequest r = submittedForValidation();
    assertThat(r.getStage()).isEqualTo(RequestStage.FOR_VALIDATION);
    List<RefundValidation> tasks = validations.of(r.getId());
    assertThat(tasks).hasSize(2);
    RefundValidation acsl =
        tasks.stream().filter(t -> "ACSL".equals(t.getValidator())).findFirst().orElseThrow();
    RefundValidation cashiering =
        tasks.stream().filter(t -> "CASHIERING".equals(t.getValidator())).findFirst().orElseThrow();
    assertThat(acsl.getStatus()).isEqualTo(ValidationStatus.OPEN);
    assertThat(acsl.getTicketRef()).startsWith("ACS-");
    // Cashiering answers through its own adapter (wave C1-C): a task for the cashiers.
    assertThat(cashiering.getStatus()).isEqualTo(ValidationStatus.OPEN);
    assertThat(cashiering.getTicketRef()).startsWith("RVL-");

    AcslCase c = investigate(acsl);
    as.run("acsl", () -> cases.provideResult(c.getId(), CaseOutcome.CONFIRMED, "In order"));
    assertThat(cases.get(c.getId()).getStage()).isEqualTo(CaseStage.RESULT_PROVIDED);
    assertThat(fx.reload(r).getStage()).isEqualTo(RequestStage.FOR_VALIDATION);

    Unapplied premium =
        cash.pay("REINSTATED-" + System.nanoTime(), new BigDecimal("300.00")).unapplied();
    RefundCheck task =
        cashieringChecks
            .list(r.getCompanyId(), List.of(RefundCheck.Status.OPEN), PageRequest.of(0, 200))
            .stream()
            .filter(t -> t.getTaskNo().equals(cashiering.getTicketRef()))
            .findFirst()
            .orElseThrow();
    as.run(
        "cashier",
        () -> cashieringChecks.confirm(task.getId(), premium.getId(), "AR-NEW-1", "Reinstated"));
    RefundValidation recorded =
        validations.of(r.getId()).stream()
            .filter(t -> t.getId().equals(cashiering.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(recorded.getStatus()).isEqualTo(ValidationStatus.CONFIRMED);
    assertThat(recorded.getNewArNo()).isEqualTo("AR-NEW-1");
    assertThat(fx.reload(r).getStage()).isEqualTo(RequestStage.FOR_REVIEW);
    assertThatThrownBy(
            () ->
                as.run(
                    REVIEWER,
                    () -> validations.record(r.getId(), cashiering.getId(), true, null, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void aRejectionSendsTheRefundBackAndResubmissionOpensANewRound() {
    PaymentRequest r = submittedForValidation();
    RefundValidation acsl =
        validations.of(r.getId()).stream()
            .filter(t -> "ACSL".equals(t.getValidator()))
            .findFirst()
            .orElseThrow();
    AcslCase c = investigate(acsl);
    as.run("acsl", () -> cases.provideResult(c.getId(), CaseOutcome.REJECTED, "Not returned"));
    PaymentRequest back = fx.reload(r);
    assertThat(back.getStage()).isEqualTo(RequestStage.PREPARING);

    as.run(PROCESSOR, () -> workflow.submit(r.getId(), "Again"));
    PaymentRequest again = fx.reload(r);
    assertThat(again.getValidationRound()).isEqualTo(2);
    assertThat(validations.of(r.getId())).hasSize(4);
  }
}
