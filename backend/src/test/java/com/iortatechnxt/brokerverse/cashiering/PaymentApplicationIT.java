package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentPreviewService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentPreviewService.Preview;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Payment acceptance end to end (CSHID.001/008/014/020/022): AR, matching, application per premium
 * component in the hierarchy, OPS_AR_RECEIPT and OPS_PAYMENT_APPLY journals, ledger movements and
 * payment status; excess, no match, 98% cap of 2% CWT accounts and the live preview.
 */
@IntegrationTest
class PaymentApplicationIT {

  @Autowired private CashFixtures fx;
  @Autowired private CashReceiptRepository receipts;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private PaymentPreviewService preview;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private long journals(String sourceRef) {
    Long n =
        jdbc.queryForObject(
            "select count(*) from jnl_batch where source_module = 'CASHIERING' and source_reference = ?",
            Long.class,
            sourceRef);
    return n == null ? 0 : n;
  }

  @Test
  void aFullPaymentIsReceiptedMatchedAppliedByComponentAndPosted() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal due = invoice.premiumBalance();

    IntakeResult result = fx.pay(invoice.getInvoiceNo(), due);

    assertThat(result.payment().getMatchCategory()).isEqualTo(MatchCategory.APPLIED);
    Receipt ar = result.receipt();
    assertThat(ar.getKind()).isEqualTo(ReceiptKind.AR);
    assertThat(ar.getReceiptNo()).startsWith("AR-HO-");
    assertThat(ar.getJournalBatchNo()).isNotNull();
    assertThat(journals("AR:" + ar.getReceiptNo())).isEqualTo(1);

    assertThat(result.applications()).hasSize(1);
    Application app = result.applications().get(0);
    assertThat(app.getAmount()).isEqualByComparingTo(due);
    assertThat(app.allocation().keySet())
        .containsSubsequence(LedgerComponent.DST, LedgerComponent.BASIC);
    assertThat(app.getRealizedCommission()).isPositive();
    assertThat(journals(app.reference())).isEqualTo(1);

    OpsInvoice after = fx.invoice(invoice.getInvoiceNo());
    assertThat(after.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(after.premiumBalance()).isZero();
    List<OpsInvoiceMovement> moves = ledger.movementsOf("CASHIERING", app.reference());
    assertThat(moves).isNotEmpty().allMatch(m -> m.getMovementType() == MovementType.APPLIED);
    assertThat(receipts.findById(ar.getId()).orElseThrow().getAppliedAmount())
        .isEqualByComparingTo(due);
  }

  @Test
  void aSmallPaymentGoesToDstFirstAndLeavesTheInvoicePartiallyPaid() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal dst = invoice.component(LedgerComponent.DST).getBalance();
    BigDecimal part = dst.min(new BigDecimal("100.00"));

    IntakeResult result = fx.pay(invoice.getArn(), part);

    Application app = result.applications().get(0);
    assertThat(app.allocation()).containsOnlyKeys(LedgerComponent.DST);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(result.payment().getMatchedRef()).isEqualTo(invoice.getInvoiceNo());
  }

  @Test
  void theExcessOfAPaymentAndAPaymentWithoutAMatchBecomeUnappliedItems() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal due = invoice.premiumBalance();

    IntakeResult over = fx.pay(invoice.getInvoiceNo(), due.add(new BigDecimal("250.00")));
    assertThat(over.payment().getMatchCategory()).isEqualTo(MatchCategory.EXCESS);
    assertThat(over.unapplied().getBalance()).isEqualByComparingTo("250.00");
    assertThat(over.unapplied().getStage()).isEqualTo("UNAPPLIED");

    IntakeResult again = fx.pay(invoice.getInvoiceNo(), new BigDecimal("80.00"));
    assertThat(again.payment().getMatchCategory()).isEqualTo(MatchCategory.EXCESS);
    assertThat(again.applications()).isEmpty();

    IntakeResult none = fx.pay("NO-SUCH-REF-" + System.nanoTime(), new BigDecimal("75.00"));
    assertThat(none.payment().getMatchCategory()).isEqualTo(MatchCategory.UNAPPLIED_NO_MATCH);
    assertThat(none.unapplied().getOrigin().name()).isEqualTo("NO_MATCH");
    assertThat(none.receipt().getAmount()).isEqualByComparingTo("75.00");
  }

  @Test
  void aTwoPercentCwtAccountIsAppliedUpToNinetyEightPercent() {
    OpsInvoice invoice = fx.cwtInvoice();
    assertThat(invoice.isCwtFlag()).isTrue();
    BigDecimal due = invoice.premiumBalance();

    Preview p =
        as.run(
            "cashier",
            () ->
                preview.preview(
                    fx.company(), List.of(invoice.getInvoiceNo()), due, null, LocalDate.now()));
    assertThat(p.match()).isEqualTo("BOOKED");
    assertThat(p.cwtWithheld()).isPositive();
    assertThat(p.excess()).isEqualByComparingTo(p.cwtWithheld());
    assertThat(p.bookRate()).isEqualByComparingTo(BigDecimal.ONE);

    IntakeResult result = fx.pay(invoice.getInvoiceNo(), due);
    assertThat(result.payment().getMatchCategory()).isEqualTo(MatchCategory.EXCESS);
    BigDecimal applied = result.payment().getAppliedAmount();
    assertThat(applied.add(p.cwtWithheld())).isEqualByComparingTo(due);
    assertThat(fx.invoice(invoice.getInvoiceNo()).premiumBalance())
        .isEqualByComparingTo(p.cwtWithheld());
  }

  @Test
  void aNonPremiumInsurerPaymentPostsToArInsuranceAndNeedsThePayor() {
    Receipt ar =
        as.run(
            "cashier",
            () ->
                fx.receiptService().issueAr(fx.nonPremium("INS-MGIC", new BigDecimal("1200.00"))));
    assertThat(ar.getReceiptClass()).isEqualTo("AR_INSURANCE");
    assertThat(journals("AR:" + ar.getReceiptNo())).isEqualTo(1);
    assertThatThrownBy(
            () ->
                as.run(
                    "cashier",
                    () -> fx.receiptService().issueAr(fx.nonPremium(null, BigDecimal.TEN))))
        .extracting("code")
        .isEqualTo("AR_INSURANCE_PAYOR_REQUIRED");
  }
}
