package com.iortatechnxt.brokerverse.cashiering;

import static com.iortatechnxt.brokerverse.cashiering.RecordFixtures.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster.Outcome;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import com.iortatechnxt.brokerverse.cashiering.service.ReversalRecordService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BDOI's cancellation and reinstatement records (Operations Cashiering FRS v3.2: FRS.CSH.03.01,
 * 04.01): the reason checks and their messages, one waiting record per receipt, posting by a second
 * user, the reversal of a payment not remitted, the AR Insurer Refund of a remitted payment, the
 * Commission Receivable payment of a cancelled OR, the partial and full reinstatement of an issued
 * AR and the AP Unapplied Commission of a reinstated OR.
 */
@IntegrationTest
class ReversalRecordIT {

  @Autowired private CashFixtures fx;
  @Autowired private RecordFixtures rf;
  @Autowired private ReceiptRecordService records;
  @Autowired private ReversalRecordService reversals;
  @Autowired private ReceiptRecordPoster poster;
  @Autowired private CashReceiptRepository receipts;
  @Autowired private UnappliedRepository unapplied;
  @Autowired private InvoiceLedgerService ledgerWriter;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;

  private Receipt issued(TenderType type, OpsInvoice... invoices) {
    BigDecimal total = BigDecimal.ZERO;
    var accounts = new ArrayList<RecordAccount>();
    for (OpsInvoice i : invoices) {
      BigDecimal part = new BigDecimal("1000.00").min(i.premiumBalance());
      total = total.add(part);
      accounts.add(account(i.getInvoiceNo(), part));
    }
    var draft =
        rf.ar(
            type,
            type == TenderType.CHECK ? RecordFixtures.today().minusDays(14) : null,
            total,
            accounts.toArray(new RecordAccount[0]));
    ReceiptRecord record = rf.submitted(draft);
    rf.post(record.getId());
    return receipts.findById(records.get(record.getId()).getReceiptId()).orElseThrow();
  }

  private static RecordReason cancelReason(String code, String text) {
    return new RecordReason(code, text, null, null, null, null, null);
  }

  private static RecordReason reinstateReason(ReinstatementType type, String unitHead) {
    return new RecordReason(
        "PRM_MISAPPLICATION",
        "Applied to the wrong invoice",
        type,
        null,
        "AO One",
        unitHead,
        "TL One");
  }

  private Outcome postAsTl(ReceiptRecord saved) {
    as.run("cashier", () -> records.submit(saved.getId()));
    return rf.post(saved.getId()).get(0);
  }

  @Test
  void cancellationRecordsCheckTheReasonAndPostTheReversalOfANotRemittedPayment() {
    OpsInvoice invoice = fx.motorInvoice();
    Receipt check = issued(TenderType.CHECK, invoice);

    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () -> reversals.cancel(List.of(check.getId()), cancelReason(null, null))))
        .hasMessage("Select a reason");
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        reversals.cancel(
                            List.of(check.getId()), cancelReason("GEN_DOUBLE_ISSUANCE", null))))
        .hasMessage("The reason Double issuance is not allowed for a check payment");
    ReceiptRecord saved =
        as.run(
                "cashier",
                () ->
                    reversals.cancel(
                        List.of(check.getId()), cancelReason("PRM_CHECK_AMOUNT", null)))
            .get(0);
    assertThat(saved.getRecordNo()).matches("CN-AR-\\d{6}");
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        reversals.cancel(
                            List.of(check.getId()), cancelReason("PRM_NO_SIGNATURE", null))))
        .hasMessage(
            "Receipt "
                + check.getReceiptNo()
                + " already has a cancellation or reinstatement record waiting");

    as.run("cashier", () -> records.submit(saved.getId()));
    assertThat(as.run("cashier", () -> poster.post(List.of(saved.getId()))).get(0).posted())
        .isFalse();
    ReceiptRecord returned =
        as.run("cashtl", () -> poster.returnToCreator(saved.getId(), "Attach the bank advice"));
    assertThat(returned.label()).isEqualTo("Returned");
    assertThat(returned.isEditable()).isTrue();
    as.run(
        "cashier",
        () -> reversals.edit(saved.getId(), cancelReason("PRM_INCORRECT_CHECK", null), List.of()));
    Outcome posted = postAsTl(saved);
    assertThat(posted.posted()).isTrue();
    assertThat(records.get(saved.getId()).label()).isEqualTo("AR/OR Cancelled");
    assertThat(receipts.findById(check.getId()).orElseThrow().getStatus())
        .isEqualTo(ReceiptStatus.CANCELLED);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isNotEqualTo(PaymentStatus.PAID);
  }

  @Test
  void theCancellationOfARemittedArCreatesAnArInsurerRefundPerAccount() {
    OpsInvoice invoice = fx.motorInvoice();
    Receipt ar = issued(TenderType.CASH, invoice);
    tx.executeWithoutResult(
        s ->
            ledgerWriter.setRemittanceStatus(
                invoice.getInvoiceNo(), RemittanceStatus.FULLY_REMITTED, "REMITTANCE", "Remitted"));
    ReceiptRecord saved =
        as.run(
                "cashier",
                () ->
                    reversals.cancel(List.of(ar.getId()), cancelReason("GEN_ISSUANCE_ERROR", null)))
            .get(0);
    postAsTl(saved);
    List<Unapplied> items = unapplied.findByReceiptIdOrderByIdAsc(ar.getId());
    assertThat(items)
        .anySatisfy(
            u -> {
              assertThat(u.getOrigin()).isEqualTo(UnappliedOrigin.AR_INSURER_REFUND);
              assertThat(u.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo());
              assertThat(u.getSourceRef()).startsWith(saved.getRecordNo());
            });
    assertThat(receipts.findById(ar.getId()).orElseThrow().getStatus())
        .isEqualTo(ReceiptStatus.CANCELLED);
  }

  @Test
  void aPartialReinstatementReversesTheChosenAccountsAndKeepsTheArIssued() {
    OpsInvoice one = fx.motorInvoice();
    OpsInvoice two = fx.motorInvoice();
    OpsInvoice three = fx.motorInvoice();
    Receipt ar = issued(TenderType.CASH, one, two, three);

    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        reversals.reinstate(
                            List.of(ar.getId()),
                            reinstateReason(ReinstatementType.PARTIAL, "UH One"),
                            List.of())))
        .hasMessage("Select at least one account for a partial reinstatement");
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        reversals.reinstate(
                            List.of(ar.getId()),
                            reinstateReason(ReinstatementType.FULL, null),
                            List.of())))
        .hasMessage("Enter the Unit Head");
    ReceiptRecord saved =
        as.run(
                "cashier",
                () ->
                    reversals.reinstate(
                        List.of(ar.getId()),
                        reinstateReason(ReinstatementType.PARTIAL, "UH One"),
                        List.of(two.getInvoiceNo(), three.getInvoiceNo())))
            .get(0);
    assertThat(saved.getRecordNo()).matches("RE-AR-\\d{6}");
    assertThat(saved.getAccounts()).hasSize(2);
    postAsTl(saved);

    assertThat(records.get(saved.getId()).label()).isEqualTo("AR/OR Reinstated");
    assertThat(receipts.findById(ar.getId()).orElseThrow().getStatus())
        .isEqualTo(ReceiptStatus.ISSUED);
    List<Unapplied> items = unapplied.findByReceiptIdOrderByIdAsc(ar.getId());
    assertThat(items)
        .filteredOn(u -> u.getOrigin() == UnappliedOrigin.REINSTATEMENT)
        .extracting(Unapplied::getInvoiceNo)
        .containsExactlyInAnyOrder(two.getInvoiceNo(), three.getInvoiceNo());
    assertThat(fx.invoice(one.getInvoiceNo()).premiumBalance())
        .isLessThan(fx.invoice(two.getInvoiceNo()).premiumBalance().add(BigDecimal.ONE));
  }

  @Test
  void aFullReinstatementOfARemittedArReversesTheRemittanceStatusAndRefundsTheInsurer() {
    OpsInvoice invoice = fx.motorInvoice();
    Receipt ar = issued(TenderType.CASH, invoice);
    tx.executeWithoutResult(
        s ->
            ledgerWriter.setRemittanceStatus(
                invoice.getInvoiceNo(), RemittanceStatus.FULLY_REMITTED, "REMITTANCE", "Remitted"));
    ReceiptRecord saved =
        as.run(
                "cashier",
                () ->
                    reversals.reinstate(
                        List.of(ar.getId()),
                        reinstateReason(ReinstatementType.FULL, "UH Two"),
                        List.of()))
            .get(0);
    assertThat(postAsTl(saved).posted()).isTrue();
    assertThat(unapplied.findByReceiptIdOrderByIdAsc(ar.getId()))
        .anySatisfy(
            u -> {
              assertThat(u.getOrigin()).isEqualTo(UnappliedOrigin.AR_INSURER_REFUND);
              assertThat(u.getInvoiceNo()).isEqualTo(invoice.getInvoiceNo());
            });
    assertThat(fx.invoice(invoice.getInvoiceNo()).getRemittanceStatus())
        .isNotIn(RemittanceStatus.FULLY_REMITTED, RemittanceStatus.PARTIALLY_REMITTED);
    assertThat(receipts.findById(ar.getId()).orElseThrow().getStatus())
        .isEqualTo(ReceiptStatus.ISSUED);
  }

  @Test
  void aReinstatedOrBecomesAnApUnappliedCommissionAndACancelledOrACommissionReceivablePayment() {
    OpsInvoice invoice = fx.motorInvoice();
    String insurer = invoice.getInsurerCode();
    var draft =
        rf.or(
            "COMMISSION",
            insurer,
            new BigDecimal("1120.00"),
            new BigDecimal("120.00"),
            BigDecimal.ZERO);
    var withAccount =
        new Draft(
            draft.kind(),
            draft.receiptType(),
            draft.branchId(),
            draft.party(),
            draft.tender(),
            List.of(account(invoice.getInvoiceNo(), new BigDecimal("1120.00"))));
    ReceiptRecord created = rf.submitted(withAccount);
    rf.post(created.getId());
    Long orId = records.get(created.getId()).getReceiptId();

    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        reversals.reinstate(
                            List.of(orId),
                            new RecordReason(
                                "DP_WTAX_ADJUSTMENT",
                                null,
                                ReinstatementType.PARTIAL,
                                null,
                                null,
                                null,
                                null),
                            List.of(invoice.getInvoiceNo()))))
        .hasMessage("Partial reinstatement is not available for an OR");
    ReceiptRecord reinstated =
        as.run(
                "cashier",
                () ->
                    reversals.reinstate(
                        List.of(orId),
                        new RecordReason(
                            "DP_WTAX_ADJUSTMENT",
                            null,
                            ReinstatementType.FULL,
                            null,
                            null,
                            null,
                            null),
                        List.of()))
            .get(0);
    assertThat(reinstated.getRecordNo()).matches("RE-OR-\\d{6}");
    postAsTl(reinstated);
    assertThat(unapplied.findByReceiptIdOrderByIdAsc(orId))
        .anySatisfy(
            u -> assertThat(u.getOrigin()).isEqualTo(UnappliedOrigin.AP_UNAPPLIED_COMMISSION));

    ReceiptRecord cancelled =
        as.run(
                "cashier",
                () -> reversals.cancel(List.of(orId), cancelReason("COM_INCORRECT_DETAILS", null)))
            .get(0);
    postAsTl(cancelled);
    assertThat(unapplied.findByReceiptIdOrderByIdAsc(orId))
        .anySatisfy(
            u -> assertThat(u.getOrigin()).isEqualTo(UnappliedOrigin.COMMISSION_RECEIVABLE));
    assertThat(records.get(cancelled.getId()).getStage()).isEqualTo(RecordStage.POSTED);
  }
}
