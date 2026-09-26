package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPaymentConfirmationSource;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PrebookedService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier.ReapplyRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier.ReapplyResult;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.IssuedReceipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink.UnappliedHandle;
import com.iortatechnxt.brokerverse.placement.service.ConfirmedPayment;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pre-booked payments and the payment gate (CSHID.020, OQ12), and the ledger ports cashiering
 * implements: {@code PaymentReapplier} (ADJID.009/012/013), {@code ReceiptIssuer} (CSHID.002/007)
 * and {@code UnappliedSink} (CSHID.024).
 */
@IntegrationTest
class PrebookedAndPortsIT {

  @Autowired private CashFixtures fx;
  @Autowired private PrebookedService prebooked;
  @Autowired private PrebookedRepository items;
  @Autowired private CashieringPaymentConfirmationSource confirmations;
  @Autowired private PaymentReapplier reapplier;
  @Autowired private ReceiptIssuer issuer;
  @Autowired private UnappliedSink sink;
  @Autowired private InvoiceLedgerService ledger;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;

  @Test
  void aPaymentBeforeBookingConfirmsTheGateAndIsAppliedWhenTheAccountIsBooked() {
    Account account = fx.unbooked();
    IntakeResult paid = fx.pay(account.getArn(), new BigDecimal("1000.00"));
    assertThat(paid.payment().getMatchCategory()).isEqualTo(MatchCategory.PREBOOKED);
    Prebooked waiting = paid.prebooked();
    assertThat(waiting.getArn()).isEqualTo(account.getArn());

    List<ConfirmedPayment> before =
        confirmations.confirmedFor(fx.company(), List.of(account.getArn()));
    assertThat(before)
        .extracting(ConfirmedPayment::reference)
        .containsExactly("PRE:" + waiting.getId());
    assertThat(confirmations.sourceCode()).isEqualTo("CASHIERING");
    assertThat(
            prebooked.list(
                fx.company(), Prebooked.OPEN, org.springframework.data.domain.Pageable.ofSize(500)))
        .extracting(Prebooked::getId)
        .contains(waiting.getId());

    OpsInvoice invoice = fx.book(account, null);

    Prebooked resolved = items.findById(waiting.getId()).orElseThrow();
    assertThat(resolved.getStatus()).isEqualTo(Prebooked.APPLIED);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
    List<ConfirmedPayment> after =
        confirmations.confirmedFor(fx.company(), List.of(account.getArn()));
    assertThat(after).extracting(ConfirmedPayment::reference).allMatch(r -> r.startsWith("APP:"));
    assertThat(confirmations.confirmedFor(fx.company(), List.of())).isEmpty();
  }

  @Test
  void aPrebookedPaymentCanBeReleasedToTheUnappliedWorkbench() {
    Account account = fx.unbooked();
    Prebooked waiting = fx.pay(account.getArn(), new BigDecimal("64.00")).prebooked();
    as.run("cashier", () -> prebooked.rematchNow(waiting.getId()));
    assertThat(items.findById(waiting.getId()).orElseThrow().getRematchCount()).isEqualTo(1);
    as.run("cashier", () -> prebooked.release(waiting.getId(), "Account will not be booked"));
    assertThat(items.findById(waiting.getId()).orElseThrow().getStatus())
        .isEqualTo(Prebooked.RELEASED);
    assertThat(as.run("cashier", () -> prebooked.rematchAll(LocalDate.now().plusDays(30))))
        .isNotNegative();
  }

  @Test
  void aPremiumDecreaseIsReappliedAndTheExcessBecomesUnapplied() {
    OpsInvoice invoice = fx.motorInvoice();
    fx.pay(invoice.getInvoiceNo(), invoice.premiumBalance());
    String ref = "ENR-TEST-" + System.nanoTime();
    ReapplyResult result =
        as.run(
            "adjust",
            () ->
                tx.execute(
                    s -> {
                      ledger.post(
                          new MovementRequest(
                              invoice.getInvoiceNo(),
                              MovementType.ADJUSTED,
                              "ADJUSTMENT",
                              ref,
                              LocalDate.now(),
                              Map.of(LedgerComponent.BASIC, new BigDecimal("-1000.00")),
                              null,
                              "Decrease of TSI"));
                      return reapplier.reapply(
                          new ReapplyRequest(
                              invoice.getInvoiceNo(),
                              "ADJUSTMENT",
                              ref,
                              LocalDate.now(),
                              "Decrease"));
                    }));
    assertThat(result.excess()).isEqualByComparingTo("1000.00");
    assertThat(result.unappliedRef()).startsWith("UNP-");
    assertThat(result.receiptNos()).hasSize(1);
    assertThat(fx.invoice(invoice.getInvoiceNo()).premiumBalance()).isZero();

    ReapplyResult again =
        as.run(
            "adjust",
            () ->
                reapplier.reapply(
                    new ReapplyRequest(
                        invoice.getInvoiceNo(), "ADJUSTMENT", ref, LocalDate.now(), "Decrease")));
    assertThat(again.unappliedRef()).isEqualTo(result.unappliedRef());

    OpsInvoice unpaid = fx.motorInvoice();
    assertThat(
            reapplier
                .reapply(
                    new ReapplyRequest(
                        unpaid.getInvoiceNo(), "ADJUSTMENT", ref, LocalDate.now(), "None"))
                .excess())
        .isZero();
  }

  @Test
  void settlementOrsAndUnappliedItemsAreCreatedForOtherModules() {
    String ref = "RMB-TEST-" + System.nanoTime();
    ReceiptIssuer.ReceiptRequest request =
        new ReceiptIssuer.ReceiptRequest(
            fx.company(),
            "COMMISSION",
            new ReceiptIssuer.Payee("INS-MGIC", "MGIC Insurance"),
            "PHP",
            LocalDate.now(),
            List.of(
                new ReceiptIssuer.ReceiptLine(
                    "BI-X",
                    "INS-MGIC",
                    new BigDecimal("1000.00"),
                    new BigDecimal("120.00"),
                    new BigDecimal("100.00"),
                    "Commission")),
            new ReceiptIssuer.Source("REMITTANCE", ref, "2307-CERT", null));
    IssuedReceipt issued = as.run("remit", () -> issuer.issueOfficialReceipt(request));
    assertThat(issued.status()).isEqualTo(ReceiptIssuer.Status.ISSUED);
    assertThat(issued.receiptNo()).startsWith("OR-HO-");
    assertThat(issued.journalBatchNo()).isNotNull();
    assertThat(as.run("remit", () -> issuer.issueOfficialReceipt(request)).receiptNo())
        .isEqualTo(issued.receiptNo());

    UnappliedHandle handle =
        as.run(
            "adjust",
            () ->
                sink.create(
                    new UnappliedSink.UnappliedRequest(
                        fx.company(),
                        "ADJUSTMENT",
                        new UnappliedSink.Party("CL-2026-000001", "CBG"),
                        "PHP",
                        new BigDecimal("75.00"),
                        null,
                        "REFUND",
                        new UnappliedSink.Source("ADJUSTMENT", ref, "Excess after decrease"))));
    assertThat(handle.status()).isEqualTo(UnappliedSink.Status.CREATED);
    assertThat(handle.reference()).startsWith("UNP-");
  }
}
