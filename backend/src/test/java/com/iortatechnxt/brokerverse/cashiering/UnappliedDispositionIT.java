package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.DispositionService;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/**
 * The unapplied workbench (CSHID.024/025): tabs by workflow stage, refund through Disbursement with
 * the team leader's approval, application to another invoice without approval, reversal of a
 * completed disposition, reclass to another client, and the return of the approver.
 */
@IntegrationTest
class UnappliedDispositionIT {

  @Autowired private CashFixtures fx;
  @Autowired private DispositionService dispositions;
  @Autowired private UnappliedService unapplied;
  @Autowired private DisbursementQueueService disbursements;
  @Autowired private WorkflowService workflow;
  @Autowired private AsUser as;

  private Unapplied unmatched(BigDecimal amount) {
    return fx.pay("UNKNOWN-" + System.nanoTime(), amount).unapplied();
  }

  private static DispositionDetails amount(BigDecimal amount) {
    return new DispositionDetails(amount, null, null, null, null, "test");
  }

  @Test
  void aRefundNeedsApprovalAndGoesToDisbursement() {
    Unapplied item = unmatched(new BigDecimal("420.00"));
    Long id = item.getId();
    assertThat(
            dispositions.list(
                fx.company(),
                DispositionService.TAB_UNAPPLIED,
                item.getReference(),
                Pageable.ofSize(5)))
        .extracting(Unapplied::getId)
        .containsExactly(id);

    as.run("cashier", () -> dispositions.assign(id, "REFUND", amount(new BigDecimal("420.00"))));
    assertThat(unapplied.get(id).getStage()).isEqualTo("MONITORING");
    as.run("cashier", () -> dispositions.submit(id));
    assertThat(unapplied.get(id).getStage()).isEqualTo("FOR_APPROVAL");

    as.run(
        "cashtl",
        () ->
            workflow.transition(
                UnappliedService.ENTITY,
                id.toString(),
                "return",
                new TransitionNote("INCOMPLETE_DETAILS", "Add the payee")));
    assertThat(unapplied.get(id).getStage()).isEqualTo("MONITORING");
    assertThat(dispositions.history(id).get(0).getStatus()).isEqualTo(DispositionStatus.MONITORING);
    as.run(
        "cashier",
        () ->
            dispositions.update(
                id,
                "REFUND",
                new DispositionDetails(
                    new BigDecimal("420.00"), null, null, null, "Refund Payee", "payee added")));
    as.run("cashier", () -> dispositions.submit(id));
    assertThatThrownBy(() -> as.run("cashier", () -> dispositions.approve(id)))
        .isInstanceOf(RuntimeException.class);

    Disposition done = as.run("cashtl", () -> dispositions.approve(id));
    assertThat(done.getStatus()).isEqualTo(DispositionStatus.COMPLETED);
    assertThat(done.getDisbursementRequestNo()).startsWith("DSQ-");
    assertThat(unapplied.get(id).getBalance()).isZero();
    assertThat(unapplied.get(id).getStage()).isEqualTo("COMPLETED");
    DisbursementRequest request =
        disbursements.find("CASHIERING", "DSP:" + done.getId()).orElseThrow();
    assertThat(request.getRequestType()).isEqualTo(DisbursementRequest.Type.REFUND);
    assertThat(request.getAmount()).isEqualByComparingTo("420.00");
  }

  @Test
  void applyingToAnotherInvoiceNeedsNoApprovalAndCanBeReversed() {
    OpsInvoice target = fx.motorInvoice();
    Unapplied item = unmatched(new BigDecimal("150.00"));
    Long id = item.getId();

    as.run(
        "cashier",
        () ->
            dispositions.assign(
                id,
                "APPLY_OTHER_INVOICE",
                new DispositionDetails(
                    new BigDecimal("150.00"), target.getInvoiceNo(), null, null, null, null)));
    Disposition applied = as.run("cashier", () -> dispositions.submit(id));
    assertThat(applied.getStatus()).isEqualTo(DispositionStatus.COMPLETED);
    assertThat(applied.getApplicationId()).isNotNull();
    assertThat(unapplied.get(id).getStage()).isEqualTo("COMPLETED");
    assertThat(fx.invoice(target.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);

    as.run("cashier", () -> dispositions.markReversal(id, "Wrong invoice"));
    assertThat(unapplied.get(id).getStage()).isEqualTo("FOR_REVERSAL");
    as.run("cashtl", () -> dispositions.approveReversal(id));
    assertThat(unapplied.get(id).getStage()).isEqualTo("UNAPPLIED");
    assertThat(unapplied.get(id).getBalance()).isEqualByComparingTo("150.00");
    assertThat(fx.invoice(target.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.UNPAID);
  }

  @Test
  void aReclassMovesTheWholeBalanceToAnotherClientAndReopensTheItem() {
    Unapplied item = unmatched(new BigDecimal("90.00"));
    Long id = item.getId();
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () -> dispositions.assign(id, "RECLASS", amount(new BigDecimal("50.00")))))
        .extracting("code")
        .isEqualTo("DISPOSITION_TARGET_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        dispositions.assign(
                            id,
                            "RECLASS",
                            new DispositionDetails(
                                new BigDecimal("50.00"),
                                null,
                                "CL-2026-000002",
                                null,
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("DISPOSITION_WHOLE_BALANCE");
    as.run(
        "cashier",
        () ->
            dispositions.assign(
                id,
                "RECLASS",
                new DispositionDetails(
                    new BigDecimal("90.00"), null, "CL-2026-000002", null, null, null)));
    as.run("cashier", () -> dispositions.submit(id));
    as.run("cashtl", () -> dispositions.approve(id));
    Unapplied after = unapplied.get(id);
    assertThat(after.getClientCode()).isEqualTo("CL-2026-000002");
    assertThat(after.getStage()).isEqualTo("UNAPPLIED");
    assertThat(after.getBalance()).isEqualByComparingTo("90.00");

    as.run("cashier", () -> dispositions.assign(id, "OTHERS", amount(new BigDecimal("90.00"))));
    as.run("cashier", () -> dispositions.withdraw(id));
    assertThat(unapplied.get(id).getStage()).isEqualTo("UNAPPLIED");
    assertThat(dispositions.types())
        .extracting(r -> r.getTypeCode())
        .contains("REFUND", "TRANSFER_UNIT");
  }
}
