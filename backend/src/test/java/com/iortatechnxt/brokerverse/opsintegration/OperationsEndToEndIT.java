package com.iortatechnxt.brokerverse.opsintegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures;
import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.DispositionDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.DispositionService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.placement.service.PaymentConfirmationSweep;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Operations modules working as one system (OPERATIONS_DESIGN section 13, wave O2), through the
 * real services as the demo users and without mocks: booking, over-the-counter payment applied per
 * component, the placement payment gate opened by cashiering, remittance extraction, exclusion and
 * restore, four-eyes approval, the Disbursement queue with the DV, the insurer OR, the production
 * reconciliation cycle, a cancellation after remittance with the AR Insurer and the payments
 * re-applied into unapplied items, a direct payment commission billed and collected with its OR,
 * and a write-off on the minimal balance file. Every step checks the ledger (components, movements,
 * flags and statuses), the journals and the open items (BRQID.004, CSHID.001/020/022,
 * RMTID.001-013/019/034/036/038, PRCID.009/026/028, ADJID.009/012/013/026, CMRID.009/010,
 * MKTID.012).
 */
@IntegrationTest
class OperationsEndToEndIT {

  private static final String REMIT = "remit";

  @Autowired private OpsJourney journey;
  @Autowired private OpsJourneyLater later;
  @Autowired private ExtractionService extraction;
  @Autowired private BatchService batches;
  @Autowired private InvoiceTagRepository tags;
  @Autowired private DisbursementQueueService queue;
  @Autowired private InsurerOrUploads orUploads;
  @Autowired private PlacementTestData placement;
  @Autowired private PaymentConfirmationSweep sweep;
  @Autowired private PaymentGateService gate;
  @Autowired private AccountQueryService accounts;
  @Autowired private AdjustmentFixtures adjustments;
  @Autowired private UnappliedRepository unapplied;
  @Autowired private DispositionService dispositions;

  @Test
  void aPremiumTravelsFromBookingToTheInsurerAndBackOnCancellation() {
    String invoiceNo = booked();
    paidOverTheCounter(invoiceNo);
    gateOpenedByCashiering();
    Long batchId = extracted(invoiceNo);
    excludedRestoredAndApproved(batchId, invoiceNo);
    RemittanceBatch batch = remittedThroughDisbursement(batchId, invoiceNo);
    insurerOrReceived(batch, invoiceNo);
    later.reconciled(invoiceNo);
    cancelledAfterRemittance(invoiceNo);
    later.directPaymentCommissionCollected();
    later.minimalBalanceWrittenOff();
  }

  /** 1. Booking copies the invoice into the ledger with its open items and journal. */
  private String booked() {
    String invoiceNo = journey.bookMotor().getInvoiceNo();
    OpsInvoice invoice = journey.invoice(invoiceNo);
    assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    assertThat(invoice.getRemittanceStatus()).isEqualTo(RemittanceStatus.WITH_OUTSTANDING_BALANCE);
    assertThat(invoice.premiumBalance()).isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(invoice.component(LedgerComponent.DTIP).getBalance())
        .isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(journey.movements(invoiceNo, MovementType.BOOKED)).isNotEmpty();
    assertThat(journey.openItems(invoiceNo))
        .containsKeys(
            OpenItemRole.CLIENT_PREMIUM, OpenItemRole.INSURER_DTIP, OpenItemRole.INSURER_COMMISSION)
        .satisfies(
            items ->
                assertThat(items.get(OpenItemRole.CLIENT_PREMIUM))
                    .isEqualByComparingTo(invoice.getGrossPremium()));
    return invoiceNo;
  }

  /** 2. An OTC payment issues its AR, matches the invoice and is applied per component. */
  private void paidOverTheCounter(String invoiceNo) {
    BigDecimal due = journey.invoice(invoiceNo).premiumBalance();
    IntakeResult paid = journey.pay(invoiceNo, due);
    assertThat(paid.payment().getMatchCategory()).isEqualTo(MatchCategory.APPLIED);
    assertThat(paid.receipt().getReceiptNo()).startsWith("AR-HO-");
    assertThat(journey.postedEvents("AR:" + paid.receipt().getReceiptNo()))
        .containsExactly("OPS_AR_RECEIPT");
    String applicationRef = paid.applications().get(0).reference();
    assertThat(journey.postedEvents(applicationRef)).contains("OPS_PAYMENT_APPLY");

    OpsInvoice after = journey.invoice(invoiceNo);
    assertThat(after.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(after.premiumBalance()).isZero();
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      assertThat(journey.moved(invoiceNo, MovementType.APPLIED, c))
          .as("applied %s", c)
          .isEqualByComparingTo(after.component(c).getBooked());
    }
    assertThat(journey.moved(invoiceNo, MovementType.APPLIED, LedgerComponent.DTIP)).isZero();
    assertThat(after.component(LedgerComponent.DTIP).getBalance())
        .isEqualByComparingTo(after.getGrossPremium());
    assertThat(journey.balancedJournals(invoiceNo)).isPositive();
  }

  /** 3. A payment received before booking opens the placement payment gate (CASHIERING source). */
  private void gateOpenedByCashiering() {
    Account account = placement.awaitingPayment(placement.motor());
    IntakeResult paid = journey.pay(account.getArn(), new BigDecimal("5000.00"));
    assertThat(paid.payment().getMatchCategory()).isEqualTo(MatchCategory.PREBOOKED);

    assertThat(sweep.sweep(journey.company(), List.of(account.getArn()))).isEqualTo(1);
    Account ready = accounts.requireByArn(account.getArn());
    assertThat(ready.getStatus()).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(gate.view(account.getArn()).evidence())
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.getSource()).isEqualTo("CASHIERING");
              assertThat(e.getReference()).isEqualTo("PRE:" + paid.prebooked().getId());
            });
  }

  /** 4. The paid invoice is extracted into a batch and locked by remittance. */
  private Long extracted(String invoiceNo) {
    journey.as(
        REMIT,
        () ->
            extraction.run(
                journey.company(),
                new Scope(ExtractionTrigger.MANUAL_INVOICE, null, null, invoiceNo),
                LocalDate.now()));
    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(invoiceNo))
        .hasValueSatisfying(t -> assertThat(t.getTag()).isEqualTo(ExtractionTag.EXTRACTED));
    OpsInvoice locked = journey.invoice(invoiceNo);
    assertThat(locked.getRemittanceStatus()).isEqualTo(RemittanceStatus.REVIEW_IN_PROCESS);
    assertThat(locked.getLockOwner()).isEqualTo("REMITTANCE");
    BatchLine line = later.lineOf(invoiceNo);
    assertThat(line.getAmounts().paidAr()).isEqualByComparingTo(locked.getGrossPremium());
    assertThat(line.getBatch().getStage()).isEqualTo(BatchStage.REVIEW_IN_PROCESS);
    return line.getBatch().getId();
  }

  /** 5. Exclusion gives the invoice back, restore takes it again; approval is four-eyes. */
  private void excludedRestoredAndApproved(Long batchId, String invoiceNo) {
    journey.as(REMIT, () -> batches.exclude(batchId, List.of(invoiceNo), "OTHERS", "Check"));
    OpsInvoice released = journey.invoice(invoiceNo);
    assertThat(released.getLockOwner()).isNull();
    assertThat(released.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    journey.as(REMIT, () -> batches.restore(batchId, invoiceNo));
    assertThat(journey.invoice(invoiceNo).getLockOwner()).isEqualTo("REMITTANCE");

    journey.as(REMIT, () -> batches.submit(batchId, "Checked"));
    assertThatThrownBy(() -> journey.as(REMIT, () -> batches.approve(batchId, null)))
        .hasMessageContaining("another user");
    RemittanceBatch approved = journey.as("remittl", () -> batches.approve(batchId, "OK"));
    assertThat(approved.getStage()).isEqualTo(BatchStage.APPROVED);
    assertThat(approved.getCommissionOrStatus()).isEqualTo("ISSUED");
    assertThat(approved.getCommissionOrNo()).startsWith("OR-HO-");

    OpsInvoice remitted = journey.invoice(invoiceNo);
    assertThat(remitted.getRemittanceStatus()).isEqualTo(RemittanceStatus.APPROVED);
    assertThat(remitted.component(LedgerComponent.DTIP).getBalance()).isZero();
    assertThat(remitted.component(LedgerComponent.COMMISSION).getBalance()).isZero();
    assertThat(journey.moved(invoiceNo, MovementType.REMITTED, LedgerComponent.DTIP))
        .isEqualByComparingTo(remitted.getGrossPremium());
    assertThat(journey.postedEvents("RMB:" + approved.getBatchNo() + ":" + invoiceNo))
        .containsExactly("OPS_REMITTANCE");
    assertThat(journey.openItems(invoiceNo)).containsKey(OpenItemRole.INSURER_DTIP);
  }

  /** 6. Disbursement acknowledges, assigns the DV and pays: the invoice is remitted, unlocked. */
  private RemittanceBatch remittedThroughDisbursement(Long batchId, String invoiceNo) {
    RemittanceBatch approved = journey.as(REMIT, () -> batches.get(batchId));
    DisbursementRequest request = queue.find("REMITTANCE", approved.getBatchNo()).orElseThrow();
    assertThat(request.getRequestType()).isEqualTo(DisbursementRequest.Type.REMITTANCE);
    assertThat(request.getAmount()).isEqualByComparingTo(approved.getTotals().payable());
    String dvNo = "DV-" + BookingFixtures.token();
    journey.inTx("disb", () -> queue.acknowledge(request.getId()));
    journey.inTx("disb", () -> queue.assignDv(request.getId(), dvNo));
    journey.inTx("disb", () -> queue.markPaid(request.getId()));

    RemittanceBatch done = journey.as(REMIT, () -> batches.get(batchId));
    assertThat(done.getStage()).isEqualTo(BatchStage.FULLY_REMITTED);
    assertThat(done.getDvNo()).isEqualTo(dvNo);
    OpsInvoice full = journey.invoice(invoiceNo);
    assertThat(full.getRemittanceStatus()).isEqualTo(RemittanceStatus.FULLY_REMITTED);
    assertThat(full.getLockOwner()).isNull();
    assertThat(queue.get(request.getId()).getStatus()).isEqualTo(DisbursementRequest.Status.PAID);
    return done;
  }

  /** 7. The insurer's OR schedule matches the line and closes the batch. */
  private void insurerOrReceived(RemittanceBatch batch, String invoiceNo) {
    BatchLine line = batch.line(invoiceNo);
    String csv =
        "batchNo,invoiceNo,orNo,orDate,orAmount\n"
            + String.join(
                ",",
                batch.getBatchNo(),
                invoiceNo,
                "INS-OR-" + BookingFixtures.token(),
                "2026-09-24",
                line.getAmounts().paidAr().toPlainString())
            + "\n";
    InsurerOrUploads.UploadResult result =
        journey.as(
            REMIT,
            () -> orUploads.upload(new FlowInFile("or.csv", csv.getBytes(StandardCharsets.UTF_8))));
    assertThat(result.updated())
        .singleElement()
        .satisfies(l -> assertThat(l.getOrStatus()).isEqualTo(OrStatus.MATCHED));
    assertThat(journey.as(REMIT, () -> batches.get(batch.getId())).getStage())
        .isEqualTo(BatchStage.OR_RECEIVED);
  }

  /**
   * 9. A flat cancellation after remittance sets up the AR Insurer, keeps the pending negative
   * adjustment for remittance and re-applies the payments: they become an unapplied item that is
   * refunded through Disbursement.
   */
  private void cancelledAfterRemittance(String invoiceNo) {
    OpsInvoice before = journey.invoice(invoiceNo);
    EndorsementRequest posted =
        adjustments.raiseAndPost(
            before,
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION", AdjustmentFixtures.FROM),
            AmountInput.NONE);
    assertThat(posted.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(posted.outcome().arInsurerAmount()).isEqualByComparingTo(before.getGrossPremium());
    assertThat(posted.outcome().excessAmount()).isEqualByComparingTo(before.getGrossPremium());
    assertThat(journey.postedEvents("ADJ:" + posted.getRequestNo() + ":ARI:INS-MGIC"))
        .containsExactly("OPS_AR_INSURER_SETUP");

    OpsInvoice after = journey.invoice(invoiceNo);
    assertThat(after.isCancelled()).isTrue();
    assertThat(after.isPendingNegAdj()).isTrue();
    assertThat(after.getLockOwner()).isNull();
    assertThat(after.component(LedgerComponent.DTIP).getBalance()).isZero();
    assertThat(journey.movements(invoiceNo, MovementType.ADJUSTED)).isNotEmpty();
    assertThat(journey.moved(invoiceNo, MovementType.APPLIED, LedgerComponent.BASIC)).isZero();
    assertThat(journey.balancedJournals(invoiceNo)).isPositive();

    Unapplied item = unapplied.findByReference(posted.outcome().unappliedRef()).orElseThrow();
    assertThat(item.getBalance()).isEqualByComparingTo(before.getGrossPremium());
    Long id = item.getId();
    journey.as(
        "cashier",
        () ->
            dispositions.assign(
                id,
                "REFUND",
                new DispositionDetails(
                    item.getBalance(), null, null, null, "Journey Payor", "Policy cancelled")));
    journey.as("cashier", () -> dispositions.submit(id));
    Disposition refund = journey.as("cashtl", () -> dispositions.approve(id));
    assertThat(queue.find("CASHIERING", "DSP:" + refund.getId()))
        .hasValueSatisfying(
            r -> assertThat(r.getRequestType()).isEqualTo(DisbursementRequest.Type.REFUND));
  }
}
