package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPaymentReversals;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringRefundValidationSource;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService.Acceptance;
import com.iortatechnxt.brokerverse.cashiering.service.DispositionService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.PaymentReversalCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RefundValidationCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester.ReversalRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource.ValidationRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedEvent;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionTicket;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Cashiering's adapters of the Operations ports (wave C1-C): the unapplied directory read by
 * Collections (BRCLXN.034-036, 040), collector disposition requests queued, accepted, applied,
 * rejected and withdrawn (BRCLXN.030-033), the CASHIERING refund validation (MKT 1.11.0) and the
 * payment reversal requested by ACSL (ACSL 2.6.0-2.6.1).
 */
@IntegrationTest
class CashieringAdaptersIT {

  private static final String SOURCE = "TESTCLX";

  @Autowired private CashFixtures fx;
  @Autowired private UnappliedDirectory directory;
  @Autowired private UnappliedDispositionRequests requests;
  @Autowired private CollectorRequestService collectorRequests;
  @Autowired private DispositionService dispositions;
  @Autowired private UnappliedService unapplied;
  @Autowired private CashieringRefundValidationSource validations;
  @Autowired private PaymentReversalRequester reversalPort;
  @Autowired private CashieringPaymentReversals reversals;
  @Autowired private ApplicationRepository applications;
  @Autowired private CapturedCashieringAnswers events;
  @Autowired private AsUser as;

  private Unapplied unmatched(String amount) {
    return fx.pay("UNKNOWN-" + System.nanoTime(), new BigDecimal(amount)).unapplied();
  }

  private DispositionTicket ask(Unapplied item, Action action, String invoiceNo, String amount) {
    return as.run(
        "clxhandler",
        () ->
            requests.request(
                new DispositionRequest(
                    fx.company(),
                    item.getReference(),
                    action,
                    invoiceNo,
                    amount == null ? null : new BigDecimal(amount),
                    "clxhandler",
                    SOURCE,
                    "REF-" + System.nanoTime(),
                    "Collector remarks")));
  }

  private CollectorRequest queued(String requestNo) {
    return collectorRequests
        .list(
            fx.company(), List.of(CollectorRequest.Status.QUEUED), requestNo, PageRequest.of(0, 5))
        .getContent()
        .get(0);
  }

  @Test
  void theDirectoryListsOpenItemsWithTheirPaymentAndHistory() {
    Unapplied item = unmatched("275.00");
    UnappliedView view = directory.find(item.getReference()).orElseThrow();
    assertThat(view.companyId()).isEqualTo(fx.company());
    assertThat(view.balance()).isEqualByComparingTo("275.00");
    assertThat(view.paymentDate()).isEqualTo(LocalDate.now());
    assertThat(view.paymentType()).isEqualTo("CASH");
    assertThat(view.transactionNo()).isNotBlank();
    assertThat(view.cashieringTab()).isEqualTo("UNAPPLIED");
    assertThat(view.dispositionStatus()).isNull();

    UnappliedFilter byText =
        new UnappliedFilter(item.getReference(), null, null, "UNAPPLIED", LocalDate.now(), null);
    assertThat(directory.open(fx.company(), byText, PageRequest.of(0, 5)))
        .extracting(UnappliedView::unappliedRef)
        .containsExactly(item.getReference());
    UnappliedFilter otherTab =
        new UnappliedFilter(item.getReference(), null, null, "MONITORING", null, null);
    assertThat(directory.open(fx.company(), otherTab, PageRequest.of(0, 5))).isEmpty();
    assertThat(directory.open(fx.company(), UnappliedFilter.all(), PageRequest.of(0, 1)))
        .hasSize(1);

    assertThat(directory.history(item.getReference()))
        .extracting(UnappliedEvent::event)
        .containsExactly("RECEIVED");
  }

  @Test
  void aCollectorApplicationIsAcceptedAppliedAndPaysTheInvoice() {
    OpsInvoice target = fx.motorInvoice();
    Unapplied item = unmatched("180.00");
    DispositionTicket ticket = ask(item, Action.APPLY_TO_INVOICE, target.getInvoiceNo(), null);
    assertThat(ticket.status()).isEqualTo(UnappliedDispositionRequests.Status.SUBMITTED);
    assertThat(ticket.reference()).startsWith("CRQ-");
    CollectorRequest request = queued(ticket.reference());
    assertThat(requests.status(SOURCE, request.getSourceRef())).contains(ticket);
    assertThat(directory.find(item.getReference()).orElseThrow().dispositionStatus())
        .isEqualTo("REQUEST_QUEUED");

    Disposition d =
        as.run(
            "cashier",
            () ->
                collectorRequests.accept(
                    request.getId(), new Acceptance(null, null, null, null, null, null, true)));
    assertThat(d.getStatus()).isEqualTo(DispositionStatus.COMPLETED);
    assertThat(d.getTargetInvoiceNo()).isEqualTo(target.getInvoiceNo());
    assertThat(unapplied.get(item.getId()).getBalance()).isZero();
    assertThat(fx.invoice(target.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(collectorRequests.get(request.getId()).getStatus())
        .isEqualTo(CollectorRequest.Status.APPLIED);
    assertThat(
            events.of(UnappliedDispositionChanged.class).stream()
                .filter(e -> e.sourceRef().equals(request.getSourceRef()))
                .map(UnappliedDispositionChanged::status))
        .containsExactly("ACCEPTED", "APPLIED");
    assertThat(requests.status(SOURCE, request.getSourceRef()).orElseThrow().message())
        .contains("executed");
    assertThat(directory.history(item.getReference()))
        .extracting(UnappliedEvent::event)
        .contains("REQUESTED", "DISPOSITION", "APPLIED", "REQUEST_APPLIED");
    assertThat(collectorRequests.forItem(item.getId())).hasSize(1);
    assertThat(collectorRequests.itemsOf(List.of(request))).containsKey(item.getId());
  }

  @Test
  void requestsAreRefusedCheckedAndRejected() {
    Unapplied item = unmatched("60.00");
    DispositionRequest unknown =
        new DispositionRequest(
            fx.company(),
            "UNP-NONE-1",
            Action.REFUND,
            null,
            null,
            "clxhandler",
            SOURCE,
            "X-1",
            null);
    assertThat(as.run("clxhandler", () -> requests.request(unknown)).status())
        .isEqualTo(UnappliedDispositionRequests.Status.REJECTED);
    assertThat(ask(item, Action.REFUND, null, "70.00").status())
        .isEqualTo(UnappliedDispositionRequests.Status.REJECTED);
    assertThat(ask(item, Action.APPLY_TO_INVOICE, " ", null).status())
        .isEqualTo(UnappliedDispositionRequests.Status.REJECTED);

    DispositionTicket reclass = ask(item, Action.RECLASS, null, null);
    CollectorRequest request = queued(reclass.reference());
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () ->
                        collectorRequests.accept(
                            request.getId(),
                            new Acceptance("REFUND", null, null, null, null, null, false))))
        .extracting("code")
        .isEqualTo("COLLECTOR_REQUEST_TYPE_MISMATCH");
    assertThatThrownBy(
            () -> as.run("cashier", () -> collectorRequests.reject(request.getId(), " ")))
        .isInstanceOf(BusinessRuleException.class);
    CollectorRequest rejected =
        as.run("cashier", () -> collectorRequests.reject(request.getId(), "Payor not identified"));
    assertThat(rejected.getStatus()).isEqualTo(CollectorRequest.Status.REJECTED);
    DispositionTicket after = requests.status(SOURCE, request.getSourceRef()).orElseThrow();
    assertThat(after.status()).isEqualTo(UnappliedDispositionRequests.Status.REJECTED);
    assertThat(after.message()).contains("Payor not identified");
    assertThatThrownBy(
            () -> as.run("cashier", () -> collectorRequests.reject(request.getId(), "again")))
        .extracting("code")
        .isEqualTo("COLLECTOR_REQUEST_DECIDED");
    assertThat(collectorRequests.queuedCount(fx.company())).isNotNegative();
  }

  @Test
  void aReclassRequestIsAcceptedWithTheTargetAndAWithdrawalRejectsIt() {
    Unapplied item = unmatched("95.00");
    CollectorRequest request = queued(ask(item, Action.RECLASS, null, null).reference());
    Disposition d =
        as.run(
            "cashier",
            () ->
                collectorRequests.accept(
                    request.getId(),
                    new Acceptance(
                        null, null, "CL-2026-000001", null, null, "Client found", false)));
    assertThat(d.getStatus()).isEqualTo(DispositionStatus.MONITORING);
    assertThat(unapplied.get(item.getId()).getStage()).isEqualTo("MONITORING");
    assertThat(collectorRequests.get(request.getId()).getStatus())
        .isEqualTo(CollectorRequest.Status.ACCEPTED);
    assertThat(directory.find(item.getReference()).orElseThrow().cashieringTab())
        .isEqualTo("MONITORING");

    as.run("cashier", () -> dispositions.withdraw(item.getId()));
    assertThat(collectorRequests.get(request.getId()).getStatus())
        .isEqualTo(CollectorRequest.Status.REJECTED);
    assertThat(directory.history(item.getReference()))
        .extracting(UnappliedEvent::event)
        .contains("WITHDRAWN", "REQUEST_REJECTED");
  }

  @Test
  void cashieringConfirmsOrRejectsARefundValidation() {
    assertThat(validations.validator()).isEqualTo(RefundValidationSource.CASHIERING);
    Unapplied premium = unmatched("300.00");
    String ref = "RRF-" + System.nanoTime();
    ValidationRequest request =
        new ValidationRequest(
            fx.company(),
            RefundValidationSource.CASHIERING,
            "BI-HO-2026-000077",
            "AR-OLD-1",
            null,
            "PHP",
            new BigDecimal("300.00"),
            new RefundValidationSource.Source("PAYREQUEST", ref, "mktao", "Cancelled policy"));
    var ticket = as.run("mktao", () -> validations.open(request));
    var again = as.run("mktao", () -> validations.open(request));
    assertThat(ticket.status()).isEqualTo(RefundValidationSource.Status.OPENED);
    assertThat(ticket.reference()).startsWith("RVL-").isEqualTo(again.reference());
    RefundCheck task =
        validations
            .list(fx.company(), List.of(RefundCheck.Status.OPEN), PageRequest.of(0, 200))
            .stream()
            .filter(t -> t.getTaskNo().equals(ticket.reference()))
            .findFirst()
            .orElseThrow();
    assertThat(validations.openCount(fx.company())).isPositive();
    assertThat(validations.candidates(task.getId())).isNotNull();
    assertThatThrownBy(
            () -> as.run("cashier", () -> validations.confirm(task.getId(), null, null, null)))
        .extracting("code")
        .isEqualTo("REFUND_VALIDATION_ITEM_REQUIRED");

    RefundCheck done =
        as.run("cashier", () -> validations.confirm(task.getId(), premium.getId(), null, null));
    assertThat(done.getStatus()).isEqualTo(RefundCheck.Status.CONFIRMED);
    assertThat(done.getNewArNo()).isNotBlank();
    assertThat(done.getResultRemarks()).contains(premium.getReference());
    RefundValidationCompleted answer =
        events.of(RefundValidationCompleted.class).stream()
            .filter(e -> e.sourceRef().equals(ref))
            .findFirst()
            .orElseThrow();
    assertThat(answer.validator()).isEqualTo("CASHIERING");
    assertThat(answer.confirmed()).isTrue();
    assertThatThrownBy(() -> as.run("cashier", () -> validations.reject(task.getId(), "late")))
        .extracting("code")
        .isEqualTo("REFUND_VALIDATION_DONE");

    String other = "RRF-" + System.nanoTime();
    var second =
        as.run(
            "mktao",
            () ->
                validations.open(
                    new ValidationRequest(
                        fx.company(),
                        RefundValidationSource.CASHIERING,
                        null,
                        "AR-OLD-2",
                        null,
                        "PHP",
                        BigDecimal.TEN,
                        new RefundValidationSource.Source("PAYREQUEST", other, "mktao", null))));
    RefundCheck secondTask =
        validations
            .list(fx.company(), List.of(RefundCheck.Status.OPEN), PageRequest.of(0, 200))
            .stream()
            .filter(t -> t.getTaskNo().equals(second.reference()))
            .findFirst()
            .orElseThrow();
    assertThat(as.run("cashier", () -> validations.reject(secondTask.getId(), "Not reinstated")))
        .extracting(RefundCheck::getStatus)
        .isEqualTo(RefundCheck.Status.REJECTED);
  }

  @Test
  void anAcslReversalIsApprovedWithFourEyesAndTheMoneyGoesBackToUnapplied() {
    OpsInvoice invoice = fx.motorInvoice();
    IntakeResult paid = fx.pay(invoice.getInvoiceNo(), new BigDecimal("400.00"));
    Application applied =
        applications.findByReceiptIdOrderByIdAsc(paid.payment().getReceiptId()).get(0);
    String receiptNo = fx.receiptService().get(paid.payment().getReceiptId()).getReceiptNo();
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);

    String caseNo = "ACS-" + System.nanoTime();
    ReversalRequest request =
        new ReversalRequest(
            fx.company(),
            invoice.getInvoiceNo(),
            receiptNo,
            "PHP",
            new BigDecimal("150.00"),
            LocalDate.now(),
            "Applied to the wrong invoice",
            new PaymentReversalRequester.Source("ACSL", caseNo, "acsl"));
    var ticket = as.run("acsl", () -> reversalPort.request(request));
    assertThat(as.run("acsl", () -> reversalPort.request(request))).isEqualTo(ticket);
    assertThat(ticket.status()).isEqualTo(PaymentReversalRequester.Status.SUBMITTED);
    assertThat(ticket.reference()).startsWith("PRV-");
    PaymentReversal pending =
        reversals
            .list(fx.company(), List.of(PaymentReversal.Status.SUBMITTED), PageRequest.of(0, 200))
            .stream()
            .filter(r -> r.getSourceRef().equals(caseNo))
            .findFirst()
            .orElseThrow();
    assertThat(reversals.submittedCount(fx.company())).isPositive();

    PaymentReversal approved = as.run("cashtl", () -> reversals.approve(pending.getId()));
    assertThat(approved.getStatus()).isEqualTo(PaymentReversal.Status.APPROVED);
    assertThat(approved.getReversedAmount()).isEqualByComparingTo("150.00");
    Unapplied back = unapplied.get(approved.getUnappliedId());
    assertThat(back.getBalance()).isEqualByComparingTo("150.00");
    assertThat(back.getReceiptId()).isEqualTo(applied.getReceiptId());
    assertThat(
            applications.findByReceiptIdOrderByIdAsc(applied.getReceiptId()).stream()
                .filter(Application::isActive)
                .map(Application::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("250.00");
    PaymentReversalCompleted answer =
        events.of(PaymentReversalCompleted.class).stream()
            .filter(e -> e.sourceRef().equals(caseNo))
            .findFirst()
            .orElseThrow();
    assertThat(answer.approved()).isTrue();
    assertThat(answer.reference()).isEqualTo(approved.getRequestNo());
    assertThatThrownBy(() -> as.run("cashtl", () -> reversals.approve(pending.getId())))
        .extracting("code")
        .isEqualTo("PAYMENT_REVERSAL_DECIDED");
  }

  @Test
  void aReversalIsRefusedToItsRequesterAndCanBeRejected() {
    String caseNo = "ACS-" + System.nanoTime();
    var ticket =
        as.run(
            "cashtl",
            () ->
                reversalPort.request(
                    new ReversalRequest(
                        fx.company(),
                        "BI-HO-2026-NOPE",
                        "AR-NOPE",
                        "PHP",
                        null,
                        null,
                        null,
                        new PaymentReversalRequester.Source("ACSL", caseNo, "cashtl"))));
    PaymentReversal pending =
        reversals
            .list(fx.company(), List.of(PaymentReversal.Status.SUBMITTED), PageRequest.of(0, 200))
            .stream()
            .filter(r -> r.getRequestNo().equals(ticket.reference()))
            .findFirst()
            .orElseThrow();
    assertThatThrownBy(() -> as.run("cashtl", () -> reversals.approve(pending.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    assertThatThrownBy(() -> as.run("cashier", () -> reversals.reject(pending.getId(), "")))
        .extracting("code")
        .isEqualTo("REJECT_REASON_REQUIRED");
    PaymentReversal rejected =
        as.run("cashier", () -> reversals.reject(pending.getId(), "Receipt not found"));
    assertThat(rejected.getStatus()).isEqualTo(PaymentReversal.Status.REJECTED);
    assertThat(
            events.of(PaymentReversalCompleted.class).stream()
                .filter(e -> e.sourceRef().equals(caseNo))
                .map(PaymentReversalCompleted::approved))
        .containsExactly(false);
  }
}
