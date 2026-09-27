package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptActionType;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.Reason;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.ReinstatementFields;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptActionService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptActionService.ReinstateRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cancellation and reinstatement of receipts (CSHID.001-005/012/013): reason lists, maker-checker,
 * reversal of the applications and of the receipt event, full and partial reinstatement with the
 * encoded fields and the re-application.
 */
@IntegrationTest
class ReceiptLifecycleIT {

  @Autowired private CashFixtures fx;
  @Autowired private ReceiptActionService actions;
  @Autowired private CashReceiptRepository receipts;
  @Autowired private ApplicationRepository applications;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private Receipt receipt(Long id) {
    return receipts.findById(id).orElseThrow();
  }

  @Test
  void aCancelledReceiptReversesItsApplicationsAndIsReinstatedInPart() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal due = invoice.premiumBalance();
    IntakeResult paid = fx.pay(invoice.getInvoiceNo(), due);
    Long receiptId = paid.receipt().getId();

    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () -> actions.requestCancel(receiptId, new Reason("GEN_OTHERS", " "))))
        .extracting("code")
        .isEqualTo("RECEIPT_REASON_TEXT_REQUIRED");
    ReceiptAction cancel =
        as.run(
            "cashier",
            () -> actions.requestCancel(receiptId, new Reason("GEN_BOUNCED_CHECK", null)));
    assertThat(cancel.getTransactionNo()).startsWith("CAN-");
    assertThat(actions.get(cancel.getId()).getStage()).isEqualTo("FOR_APPROVAL");
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        actions.requestCancel(receiptId, new Reason("GEN_DOUBLE_ISSUANCE", null))))
        .extracting("code")
        .isEqualTo("RECEIPT_ACTION_PENDING");
    assertThatThrownBy(() -> as.run("cashier", () -> actions.approve(cancel.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");

    ReceiptAction posted = as.run("cashtl", () -> actions.approve(cancel.getId()));
    assertThat(actions.get(posted.getId()).getStage()).isEqualTo("POSTED");
    assertThat(receipt(receiptId).getStatus()).isEqualTo(ReceiptStatus.CANCELLED);
    assertThat(receipt(receiptId).getAppliedAmount()).isZero();
    Application app = applications.findByReceiptIdOrderByIdAsc(receiptId).get(0);
    assertThat(app.isActive()).isFalse();
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.UNPAID);
    assertThat(ledger.movementsOf("CASHIERING", app.reference() + ":" + cancel.getTransactionNo()))
        .allMatch(m -> m.getMovementType() == MovementType.UNAPPLIED);
    Long cancelJournals =
        jdbc.queryForObject(
            "select count(*) from jnl_batch where source_module = 'CASHIERING' and source_reference = ?",
            Long.class,
            "AR:" + paid.receipt().getReceiptNo() + ":" + cancel.getTransactionNo());
    assertThat(cancelJournals).isEqualTo(1);

    BigDecimal half = due.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.DOWN);
    ReceiptAction reinstate =
        as.run(
            "cashier",
            () ->
                actions.requestReinstatement(
                    receiptId,
                    new ReinstateRequest(
                        false,
                        half,
                        new Reason("PRM_CANCELLATION", null),
                        new ReinstatementFields(
                            invoice.getInvoiceNo(), null, null, null, null, null))));
    assertThat(reinstate.getAction()).isEqualTo(ReceiptActionType.REINSTATE_PARTIAL);
    as.run("cashtl", () -> actions.approve(reinstate.getId()));
    Receipt back = receipt(receiptId);
    assertThat(back.getStatus()).isEqualTo(ReceiptStatus.REINSTATED);
    assertThat(back.getReinstatedAmount()).isEqualByComparingTo(half);
    assertThat(back.getAppliedAmount()).isEqualByComparingTo(half);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
  }

  @Test
  void aPremiumReinstatementNeedsItsEncodedFields() {
    OpsInvoice invoice = fx.motorInvoice();
    IntakeResult paid = fx.pay(invoice.getInvoiceNo(), new BigDecimal("300.00"));
    Long receiptId = paid.receipt().getId();
    ReceiptAction cancel =
        as.run(
            "cashier",
            () -> actions.requestCancel(receiptId, new Reason("GEN_ISSUANCE_ERROR", null)));
    as.run("cashtl", () -> actions.approve(cancel.getId()));

    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        actions.requestReinstatement(
                            receiptId,
                            new ReinstateRequest(
                                true,
                                null,
                                new Reason("PRM_MISAPPLICATION", null),
                                new ReinstatementFields(
                                    invoice.getInvoiceNo(), null, null, null, null, null)))))
        .extracting("code")
        .isEqualTo("REINSTATEMENT_FIELDS_REQUIRED");

    ReceiptAction full =
        as.run(
            "cashier",
            () ->
                actions.requestReinstatement(
                    receiptId,
                    new ReinstateRequest(
                        true,
                        null,
                        new Reason("PRM_MISAPPLICATION", null),
                        new ReinstatementFields(
                            invoice.getInvoiceNo(),
                            paid.receipt().getReceiptNo(),
                            "Test Payor",
                            "ao",
                            "Unit Head CBG",
                            "Team Leader CBG"))));
    assertThat(full.getAmount()).isEqualByComparingTo("300.00");
    assertThat(
            actions.list(
                fx.company(),
                List.of("FOR_APPROVAL"),
                org.springframework.data.domain.Pageable.ofSize(50)))
        .extracting(ReceiptAction::getId)
        .contains(full.getId());
    as.run("cashtl", () -> actions.approve(full.getId()));
    assertThat(receipt(receiptId).getStatus()).isEqualTo(ReceiptStatus.REINSTATED);
    assertThat(actions.ofReceipt(receiptId)).hasSize(2);
  }
}
