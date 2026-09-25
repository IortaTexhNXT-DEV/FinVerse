package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffRefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.RefundValidations;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.EmptyUnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.HandoffDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.HandoffPaymentReversalRequester;
import com.iortatechnxt.brokerverse.opsledger.service.adapter.LedgerInvoiceCorrectionSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink.CorrectionRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester.ReversalRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionTicket;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The contract the Collections (BRD-4) and Accounting / Disbursement / ACSL (BRD-5) modules build
 * on: the new Operations ports with their default adapters, the extended payment request (routing,
 * references, cancellation, DV and instrument statuses) and ACSL corrections on the ledger.
 */
@IntegrationTest
class CollectionsAndDisbursementPortsIT {

  private static final String SOURCE = "TESTMOD";

  @Autowired private UnappliedDirectory directory;
  @Autowired private UnappliedDispositionRequests dispositions;
  @Autowired private RefundValidationSource defaultValidation;
  @Autowired private RefundValidations validations;
  @Autowired private PaymentReversalRequester reversals;
  @Autowired private InvoiceCorrectionSink corrections;
  @Autowired private CollectionFeed collection;
  @Autowired private DisbursementQueueService queue;
  @Autowired private HandoffService handoffs;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private CapturedLedgerEvents events;
  @Autowired private OpsLedgerFixtures fx;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;

  @Test
  void theNewPortsKeepTheirDefaultsUntilTheModulesAreBuilt() {
    assertThat(directory).isInstanceOf(EmptyUnappliedDirectory.class);
    assertThat(dispositions).isInstanceOf(HandoffDispositionRequests.class);
    // ACSL (wave A1-PRQ) now serves its own refund validations; Cashiering is still handed over.
    assertThat(defaultValidation.validator()).isEqualTo(RefundValidationSource.ACSL);
    assertThat(reversals).isInstanceOf(HandoffPaymentReversalRequester.class);
    assertThat(corrections).isInstanceOf(LedgerInvoiceCorrectionSink.class);
    assertThat(validations.installed()).containsExactly(RefundValidationSource.ACSL);

    assertThat(directory.open(fx.company(), UnappliedFilter.all(), PageRequest.of(0, 10)))
        .isEmpty();
    assertThat(directory.find("UPP-1")).isEmpty();
    assertThat(directory.history("UPP-1")).isEmpty();
    // Collections serves the feeds in-app; acknowledging an unknown key is harmless.
    collection.acknowledge(fx.company(), "COLLECTION_CWT2307", List.of("CWT:NO-SUCH:1"));
    assertThat(collection.transport()).isEqualTo("IN_APP");
  }

  @Test
  void collectorDispositionsAreHandedOverOncePerRequest() {
    String ref = "APR-" + BookingFixtures.token();
    DispositionRequest request =
        new DispositionRequest(
            fx.company(),
            "UPP-" + ref,
            Action.APPLY_TO_INVOICE,
            "BI-HO-2026-000001",
            new BigDecimal("500.00"),
            "mktcoll",
            "COLLECTIONS",
            ref,
            "Client asked to apply");
    DispositionTicket first =
        as.run("mktcoll", () -> tx.execute(s -> dispositions.request(request)));
    DispositionTicket again =
        as.run("mktcoll", () -> tx.execute(s -> dispositions.request(request)));
    assertThat(first.status()).isEqualTo(UnappliedDispositionRequests.Status.DEFERRED);
    assertThat(first.reference()).startsWith("HANDOFF-").isEqualTo(again.reference());
    assertThat(dispositions.status("COLLECTIONS", ref)).contains(first);
    assertThat(dispositions.status("COLLECTIONS", "NONE-" + ref)).isEmpty();

    OpsHandoff handoff =
        handoffs.find(HandoffDispositionRequests.PORT, "COLLECTIONS", ref).orElseThrow();
    assertThat(handoff.getSummary()).contains("APPLY_TO_INVOICE", "BI-HO-2026-000001");
    as.run("cashier", () -> tx.execute(s -> handoffs.close(handoff.getId(), "Applied by hand")));
    assertThat(dispositions.status("COLLECTIONS", ref).orElseThrow().message())
        .contains("Applied by hand");
  }

  @Test
  void refundValidationsAndReversalsAreHandedOverWhileTheirModulesAreMissing() {
    String ref = "RRF-" + BookingFixtures.token();
    // The ACSL validation opens a case on the invoice, so it must be in the ledger.
    String invoiceNo = fx.motorInvoice().getInvoiceNo();
    for (String validator :
        List.of(RefundValidationSource.ACSL, RefundValidationSource.CASHIERING)) {
      var ticket =
          as.run(
              "mktao",
              () ->
                  tx.execute(
                      s ->
                          validations.open(
                              new ValidationRequest(
                                  fx.company(),
                                  validator,
                                  invoiceNo,
                                  "AR-" + ref,
                                  BookingFixtures.CLIENT,
                                  "PHP",
                                  new BigDecimal("1200.00"),
                                  new RefundValidationSource.Source(
                                      "PAYREQUEST", ref, "mktao", null)))));
      assertThat(ticket.validator()).isEqualTo(validator);
      if (RefundValidationSource.CASHIERING.equals(validator)) {
        assertThat(ticket.status()).isEqualTo(RefundValidationSource.Status.DEFERRED);
        assertThat(ticket.reference()).startsWith("HANDOFF-");
      } else {
        assertThat(ticket.status()).isEqualTo(RefundValidationSource.Status.OPENED);
      }
    }
    assertThat(
            handoffs.find(
                HandoffRefundValidationSource.PORT,
                "PAYREQUEST",
                RefundValidationSource.CASHIERING + ":" + ref))
        .isPresent();

    var reversal =
        as.run(
            "acsl",
            () ->
                tx.execute(
                    s ->
                        reversals.request(
                            new ReversalRequest(
                                fx.company(),
                                "BI-HO-2026-000009",
                                "AR-" + ref,
                                "PHP",
                                null,
                                LocalDate.of(2026, 9, 25),
                                "Applied to the wrong invoice",
                                new PaymentReversalRequester.Source(
                                    "ACSL", "ACS-" + ref, "acsl")))));
    assertThat(reversal.status()).isEqualTo(PaymentReversalRequester.Status.DEFERRED);
    assertThat(reversal.message()).contains("hand-off");
  }

  @Test
  void anAcslCorrectionMovesTheInvoiceOnceAndKeepsTheOriginalMovements() {
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    BigDecimal before = invoice.component(LedgerComponent.BASIC).getBalance();
    CorrectionRequest request =
        new CorrectionRequest(
            no,
            Map.of(
                LedgerComponent.BASIC,
                new BigDecimal("-10.00"),
                LedgerComponent.DST,
                BigDecimal.ZERO),
            "ACSL",
            "ACS-" + BookingFixtures.token(),
            LocalDate.of(2026, 9, 25),
            "JV-TEST-1",
            "Wrong amount");
    var result = as.run("acsl", () -> tx.execute(s -> corrections.record(request)));
    var again = as.run("acsl", () -> tx.execute(s -> corrections.record(request)));
    assertThat(result.movements()).isEqualTo(1).isEqualTo(again.movements());
    assertThat(result.rootInvoiceNo()).isEqualTo(no);
    OpsInvoice after = tx.execute(s -> ledger.require(no));
    assertThat(after.component(LedgerComponent.BASIC).getBalance())
        .isEqualByComparingTo(before.subtract(new BigDecimal("10.00")));
    assertThat(ledger.movements(no))
        .extracting(m -> m.getMovementType())
        .contains(MovementType.BOOKED, MovementType.CORRECTION);
  }

  @Test
  void aPaymentRequestCarriesTheDisbursementRoutingAndCanBeCancelled() {
    String ref = "PRQ-" + BookingFixtures.token();
    DisbursementRequest.Spec spec =
        new DisbursementRequest.Spec(
                DisbursementRequest.Type.CASH_ADVANCE,
                SOURCE,
                ref,
                "EMP-001",
                "Employee",
                "PHP",
                new BigDecimal("3000.00"),
                "Cash advance",
                "ATT-1")
            .routed("RFP-2026-000001", "EMPLOYEE", "CASH_ADVANCE", "BI-ROOT-1", false)
            .withReferences(List.of("ATT-1", "ATT-2"), List.of("OI:1", "OI:2"));
    Long id = as.run("mktao", () -> tx.execute(s -> queue.send(fx.company(), spec).getId()));
    DisbursementRequest saved = queue.get(id);
    assertThat(saved.getRfpNo()).isEqualTo("RFP-2026-000001");
    assertThat(saved.getPayeeClass()).isEqualTo("EMPLOYEE");
    assertThat(saved.getDisbursementType()).isEqualTo("CASH_ADVANCE");
    assertThat(saved.getRootInvoiceNo()).isEqualTo("BI-ROOT-1");
    assertThat(saved.getAttachmentRefs()).containsExactly("ATT-1", "ATT-2");
    assertThat(saved.getAccountingRefs()).containsExactly("OI:1", "OI:2");
    assertThat(saved.isStraightToApproval()).isFalse();

    as.run("disb", () -> tx.execute(s -> queue.assignDv(id, "DV-2026-000777")));
    DisbursementRequest tracked =
        as.run("disb", () -> tx.execute(s -> queue.track(id, "APPROVED", "PRINTED")));
    assertThat(tracked.getDvStatus()).isEqualTo("APPROVED");
    assertThat(tracked.getInstrumentStatus()).isEqualTo("PRINTED");
    as.run("disb", () -> tx.execute(s -> queue.cancel(id, "Wrong payee account")));
    assertThat(queue.get(id).getStatus()).isEqualTo(DisbursementRequest.Status.CANCELLED);
    assertThat(queue.get(id).getCancelledAt()).isNotNull();
    assertThat(events.of(DisbursementStatusChanged.class))
        .anySatisfy(
            e -> {
              assertThat(e.sourceRef()).isEqualTo(ref);
              assertThat(e.status()).isEqualTo(DisbursementRequest.Status.CANCELLED);
              assertThat(e.reason()).isEqualTo("Wrong payee account");
              assertThat(e.dvStatus()).isEqualTo("APPROVED");
              assertThat(e.instrumentStatus()).isEqualTo("PRINTED");
            });
    assertThatThrownBy(() -> tx.execute(s -> queue.cancel(id, "Again")))
        .extracting("code")
        .isEqualTo("DISBURSEMENT_STATUS");
    assertThatThrownBy(() -> tx.execute(s -> queue.track(id, "APPROVED", null)))
        .extracting("code")
        .isEqualTo("DISBURSEMENT_STATUS");
  }

  @Test
  void theOperationsSpecKeepsItsDefaults() {
    DisbursementRequest.Spec spec =
        new DisbursementRequest.Spec(
            DisbursementRequest.Type.REFUND,
            SOURCE,
            "R-1",
            "CL-1",
            null,
            "PHP",
            BigDecimal.ONE,
            "Refund",
            null);
    assertThat(spec.rfpNo()).isNull();
    assertThat(spec.attachmentRefs()).isEmpty();
    assertThat(spec.accountingRefs()).isEmpty();
    assertThat(spec.straightToApproval()).isFalse();
    assertThat(spec.routed(null, null, null, null, true).straightToApproval()).isTrue();
    DisbursementStatusChanged event =
        new DisbursementStatusChanged(
            1L,
            "DSQ-1",
            DisbursementRequest.Type.REFUND,
            SOURCE,
            "R-1",
            DisbursementRequest.Status.PAID,
            "DV-1",
            null);
    assertThat(event.dvStatus()).isNull();
    assertThat(event.instrumentStatus()).isNull();
  }
}
