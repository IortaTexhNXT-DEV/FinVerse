package com.iortatechnxt.brokerverse.payrequest;

import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.APPROVER;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.HR;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.PROCESSOR;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.REVIEWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccount;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestApprovalSource;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts;
import com.iortatechnxt.brokerverse.payrequest.service.RequestFormService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Refund requests from the RRF to Disbursement and back (MKT 1.2.0-2.26.0): approval with four
 * eyes, the Disbursement request through the gateway, the CA / SA information on the client, one
 * live refund per AR, returns by Disbursement, check cancellations and the approval inbox.
 */
@IntegrationTest
class PayRequestFlowIT {

  @Autowired private PayRequestFixtures fx;
  @Autowired private RequestFormService forms;
  @Autowired private PayRequestWorkflowService workflow;
  @Autowired private WorkflowService workCases;
  @Autowired private WorkflowViewService views;
  @Autowired private ClientPayoutAccounts payouts;
  @Autowired private PayRequestApprovalSource approvals;
  @Autowired private AsUser as;

  @Test
  void anRrfIsApprovedSentToDisbursementAndDisbursed() {
    OpsInvoice invoice = fx.invoice();
    PaymentRequest raised =
        fx.raise(
            PayRequestFixtures.refund(PayRequestFixtures.line(invoice, "OVERPAYMENT", "250.00")));
    assertThat(raised.getRequestNo()).startsWith("RRF-");
    assertThat(raised.getStage()).isEqualTo(RequestStage.DRAFT);
    assertThat(raised.getAmount()).isEqualByComparingTo("250.00");
    assertThat(raised.isValidationRequired()).isFalse();
    assertThat(raised.rootInvoiceNo()).isEqualTo(invoice.getRootInvoiceNo());

    as.run(PROCESSOR, () -> workflow.submit(raised.getId(), "Refund please"));
    assertThatThrownBy(() -> as.run(PROCESSOR, () -> workflow.endorse(raised.getId(), null)))
        .isInstanceOf(RuntimeException.class);
    as.run(REVIEWER, () -> workflow.endorse(raised.getId(), "OK"));
    assertThat(
            approvals.pendingFor(ApprovalViewer.user(APPROVER, Set.of("PRQ_APPROVE", "PRQ_VIEW"))))
        .anySatisfy(p -> assertThat(p.reference()).isEqualTo(raised.getRequestNo()));
    assertThat(approvals.pendingFor(ApprovalViewer.user(REVIEWER, Set.of("PRQ_REVIEW")))).isEmpty();
    as.run(APPROVER, () -> workflow.approve(raised.getId(), "Go"));

    PaymentRequest sent = fx.reload(raised);
    assertThat(sent.getStage()).isEqualTo(RequestStage.SENT_TO_DISBURSEMENT);
    DisbursementRequest gateway = fx.gatewayRequest(sent);
    assertThat(gateway.getRequestType()).isEqualTo(DisbursementRequest.Type.REFUND);
    assertThat(gateway.getAmount()).isEqualByComparingTo("250.00");
    assertThat(sent.trackOrNone().requestNo()).isEqualTo(gateway.getRequestNo());
    assertThat(sent.isPayoutRecorded()).isTrue();
    assertThat(payouts.forClient(fx.company(), invoice.getClientCode()))
        .extracting(ClientPayoutAccount::getMode, ClientPayoutAccount::getPayeeName)
        .contains(
            org.assertj.core.groups.Tuple.tuple(PayoutMode.CHECK, sent.getPayee().accountName()));

    fx.pay(sent, "DV-T-" + sent.getId());
    PaymentRequest paid = fx.reload(raised);
    assertThat(paid.getStage()).isEqualTo(RequestStage.DISBURSED);
    assertThat(paid.trackOrNone().dvNo()).isEqualTo("DV-T-" + sent.getId());
    assertThat(paid.trackOrNone().instrumentStatus()).isEqualTo("RELEASED");
    assertThat(paid.trackOrNone().disbursedAt()).isNotNull();
  }

  @Test
  void fourEyesAndOneLiveRefundPerAr() {
    OpsInvoice invoice = fx.invoice();
    var line = PayRequestFixtures.line(invoice, "DOUBLE_PAYMENT", "80.00");
    PaymentRequest first = fx.raise(PayRequestFixtures.refund(line));
    assertThatThrownBy(() -> fx.raise(PayRequestFixtures.refund(line)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(first.getRequestNo());
    assertThatThrownBy(() -> fx.raise(PayRequestFixtures.refund(line, line)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("twice");

    as.run(PROCESSOR, () -> workflow.submit(first.getId(), null));
    as.run(REVIEWER, () -> workflow.endorse(first.getId(), null));
    assertThatThrownBy(() -> as.run(REVIEWER, () -> workflow.approve(first.getId(), null)))
        .isInstanceOf(RuntimeException.class);

    Long caseId =
        views
            .view("PaymentRequest", String.valueOf(first.getId()))
            .orElseThrow()
            .workCase()
            .getId();
    as.run(
        APPROVER,
        () ->
            workCases.genericTransition(caseId, "return", new TransitionNote("OTHERS", "Fix it")));
    PaymentRequest returned = fx.reload(first);
    assertThat(returned.getStage()).isEqualTo(RequestStage.FOR_REVIEW);
    assertThat(returned.trailOrNone().returnReason()).isEqualTo("OTHERS");

    as.run(
        REVIEWER,
        () -> workCases.genericTransition(caseId, "cancel", new TransitionNote("DUPLICATE", null)));
    assertThat(fx.reload(first).getStage()).isEqualTo(RequestStage.CANCELLED);
    PaymentRequest again = fx.raise(PayRequestFixtures.refund(line));
    assertThat(again.getLines()).singleElement().satisfies(l -> assertThat(l.isLive()).isTrue());
  }

  @Test
  void aRefundReturnedByDisbursementIsSentAgainUnderANewReference() {
    PaymentRequest sent = fx.approvedRefund();
    String firstRef = fx.reload(sent).currentSendRef();
    fx.returnToSource(sent, "Payee not maintained");
    PaymentRequest back = fx.reload(sent);
    assertThat(back.getStage()).isEqualTo(RequestStage.PREPARING);
    assertThat(back.trackOrNone().message()).isEqualTo("Payee not maintained");

    PaymentRequest resent = fx.approve(back);
    assertThat(resent.getStage()).isEqualTo(RequestStage.SENT_TO_DISBURSEMENT);
    assertThat(fx.reload(sent).currentSendRef()).isNotEqualTo(firstRef).startsWith(firstRef + "/");
    fx.pay(resent, "DV-R-" + sent.getId());
    assertThat(fx.reload(sent).getStage()).isEqualTo(RequestStage.DISBURSED);
  }

  @Test
  void aCashAdvanceNeedsMarketingThenHrApproval() {
    PaymentRequest ca =
        as.run(
            PROCESSOR,
            () -> forms.createCashAdvance(fx.company(), PayRequestFixtures.cashAdvance("5000.00")));
    assertThat(ca.getKind()).isEqualTo(RequestKind.CASH_ADVANCE);
    assertThat(ca.getRequestNo()).startsWith("RFP-");
    PaymentRequest atHr = fx.approve(ca);
    assertThat(atHr.getStage()).isEqualTo(RequestStage.HR_APPROVAL);
    assertThat(approvals.pendingFor(ApprovalViewer.user(HR, Set.of("PRQ_HR_APPROVE", "PRQ_VIEW"))))
        .anySatisfy(p -> assertThat(p.reference()).isEqualTo(ca.getRequestNo()));
    assertThatThrownBy(() -> as.run(APPROVER, () -> workflow.approve(ca.getId(), null)))
        .isInstanceOf(RuntimeException.class);
    as.run(HR, () -> workflow.approve(ca.getId(), "HR ok"));
    PaymentRequest sent = fx.reload(ca);
    assertThat(sent.getStage()).isEqualTo(RequestStage.SENT_TO_DISBURSEMENT);
    assertThat(sent.trailOrNone().hrApprovedBy()).isEqualTo(HR);
    assertThat(fx.gatewayRequest(sent).getRequestType())
        .isEqualTo(DisbursementRequest.Type.CASH_ADVANCE);
    assertThat(sent.isPayoutRecorded()).isFalse();
  }

  @Test
  void aDisbursedCheckIsCancelledThroughAHandOff() {
    PaymentRequest paid = fx.approvedRefund();
    RequestDrafts.CheckCancellation draft =
        new RequestDrafts.CheckCancellation(paid.getRequestNo(), "CHK-1", "CHECK_SPOILED", "Lost");
    assertThatThrownBy(
            () -> as.run(PROCESSOR, () -> forms.createCheckCancellation(fx.company(), draft)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no disbursement voucher");
    fx.pay(paid, "DV-C-" + paid.getId());

    PaymentRequest ccr =
        as.run(PROCESSOR, () -> forms.createCheckCancellation(fx.company(), draft));
    assertThat(ccr.getStage()).isEqualTo(RequestStage.REQUESTED);
    assertThat(ccr.targetOrNone().dvNo()).isEqualTo("DV-C-" + paid.getId());
    assertThatThrownBy(
            () -> as.run(PROCESSOR, () -> forms.createCheckCancellation(fx.company(), draft)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already in progress");
    PaymentRequest sent = fx.approve(ccr);
    assertThat(sent.getStage()).isEqualTo(RequestStage.SENT);
    assertThat(sent.trackOrNone().handoffRef()).startsWith("HANDOFF-");
  }
}
