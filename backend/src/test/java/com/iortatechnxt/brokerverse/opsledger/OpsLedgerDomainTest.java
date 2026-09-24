package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RecordStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InsurerShareAllocator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Plain tests of the ledger's calculations and state rules. */
class OpsLedgerDomainTest {

  private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

  private static OpsInvoice invoice(InvoiceKind kind, boolean directPayment) {
    OpsInvoice i =
        OpsInvoice.of(
            new OpsInvoiceData(
                new OpsInvoiceData.Keys(
                    1L, 1L, "BI-T-1", "ARN-T-1", 9L, kind, null, null, "POL-1", 1),
                new OpsInvoiceData.Parties("CL-1", "Client", "Client", "INS-A"),
                new OpsInvoiceData.Classification(
                    "PHP", DAY, DAY, DAY.plusYears(1), "MTR10", "MTR", "CBG", "ao", "MKT", "CC"),
                new OpsInvoiceData.Amounts(
                    new BigDecimal("1150.00"),
                    new BigDecimal("100.00"),
                    new BigDecimal("12.00"),
                    BigDecimal.TEN),
                new OpsInvoiceData.Flags(directPayment, false, false)),
            List.of(new OpsInvoiceShare("INS-A", new BigDecimal("100"), true)),
            FeedSource.EVENT);
    i.move(MovementType.BOOKED, LedgerComponent.BASIC, new BigDecimal("1000.00"));
    i.move(MovementType.BOOKED, LedgerComponent.DST, new BigDecimal("150.00"));
    i.move(MovementType.BOOKED, LedgerComponent.DTIP, new BigDecimal("1150.00"));
    return i;
  }

  @Test
  void paymentStatusFollowsThePremiumBalance() {
    OpsInvoice i = invoice(InvoiceKind.BOOKING, false);
    assertThat(i.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    assertThat(i.getRemittanceStatus()).isEqualTo(RemittanceStatus.WITH_OUTSTANDING_BALANCE);
    i.move(MovementType.APPLIED, LedgerComponent.DST, new BigDecimal("150"));
    assertThat(i.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    i.move(MovementType.APPLIED, LedgerComponent.BASIC, new BigDecimal("1000"));
    assertThat(i.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(i.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    i.move(MovementType.UNAPPLIED, LedgerComponent.BASIC, new BigDecimal("1000"));
    assertThat(i.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(i.getRemittanceStatus()).isEqualTo(RemittanceStatus.WITH_OUTSTANDING_BALANCE);
    assertThat(i.premiumBalance()).isEqualByComparingTo("1000");
    assertThat(i.component(LedgerComponent.BASIC).netApplied()).isZero();

    i.changeRemittanceStatus(RemittanceStatus.REVIEW_IN_PROCESS);
    i.move(MovementType.APPLIED, LedgerComponent.BASIC, new BigDecimal("1000"));
    assertThat(i.getRemittanceStatus()).isEqualTo(RemittanceStatus.REVIEW_IN_PROCESS);
    i.move(MovementType.REMITTED, LedgerComponent.DTIP, new BigDecimal("1150"));
    i.move(MovementType.CWT_RECLASS, LedgerComponent.PR2307, new BigDecimal("20"));
    i.move(MovementType.WRITE_OFF, LedgerComponent.PR2307, new BigDecimal("20"));
    assertThat(i.balances()).containsEntry(LedgerComponent.DTIP, new BigDecimal("0.00"));
    assertThat(i.component(LedgerComponent.PR2307).getAdjusted()).isEqualByComparingTo("20");
    assertThat(i.component(LedgerComponent.PR2307).getBalance()).isZero();
  }

  @Test
  void directPaymentAndReturnInvoicesHaveNoReceivable() {
    OpsInvoice dp = invoice(InvoiceKind.BOOKING, true);
    assertThat(dp.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_APPLICABLE);
    assertThat(dp.getRemittanceStatus()).isEqualTo(RemittanceStatus.NOT_APPLICABLE);
    OpsInvoice credit = invoice(InvoiceKind.CANCELLATION, false);
    assertThat(credit.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_APPLICABLE);
    assertThat(credit.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    assertThat(credit.leadShare().insurerCode()).isEqualTo("INS-A");
  }

  @Test
  void flagsAndLocksKeepTheirRules() {
    OpsInvoice i = invoice(InvoiceKind.BOOKING, false);
    for (InvoiceFlag flag : InvoiceFlag.values()) {
      assertThat(i.setFlag(flag, true)).isFalse();
      assertThat(i.flag(flag)).isTrue();
    }
    assertThat(i.unlock("REMITTANCE")).isFalse();
    i.lock("REMITTANCE", "Batch", "remit", Instant.now());
    i.lock("REMITTANCE", "Batch again", "remit", Instant.now());
    assertThat(i.getLockReason()).isEqualTo("Batch again");
    assertThatThrownBy(() -> i.lock("ADJUSTMENT", "Posting", "adjust", Instant.now()))
        .hasMessageContaining("locked by REMITTANCE");
    i.requireNotLockedByOther("REMITTANCE");
    assertThat(i.unlock("REMITTANCE")).isTrue();
    assertThat(i.getLockOwner()).isNull();
    i.updateReferences("POL-2", "PN-1,PN-2");
    assertThat(i.getPnNos()).isEqualTo("PN-1,PN-2");
  }

  @Test
  void sharesSplitAmountsWithTheRemainderOnTheLastInsurer() {
    Map<String, BigDecimal> parts =
        InsurerShareAllocator.allocate(
            new BigDecimal("100.00"),
            List.of(
                new OpsInvoiceShare("A", new BigDecimal("33.3333"), true),
                new OpsInvoiceShare("B", new BigDecimal("33.3333"), false),
                new OpsInvoiceShare("C", new BigDecimal("33.3334"), false)));
    assertThat(parts)
        .containsEntry("A", new BigDecimal("33.33"))
        .containsEntry("B", new BigDecimal("33.33"))
        .containsEntry("C", new BigDecimal("33.34"));
    assertThat(
            InsurerShareAllocator.allocate(
                new BigDecimal("-10"),
                List.of(new OpsInvoiceShare("A", new BigDecimal("100"), true))))
        .containsEntry("A", new BigDecimal("-10.00"));
  }

  @Test
  void componentsMapFromBookingInHierarchyOrder() {
    assertThat(LedgerComponent.applicationHierarchy())
        .containsExactly(
            LedgerComponent.DST,
            LedgerComponent.PREMIUM_TAX_VAT,
            LedgerComponent.LGT,
            LedgerComponent.FST,
            LedgerComponent.OTHER,
            LedgerComponent.BASIC);
    for (PremiumComponent c : PremiumComponent.values()) {
      assertThat(LedgerComponent.of(c).isPremiumReceivable()).isTrue();
    }
    assertThat(LedgerComponent.DTIP.isPremiumReceivable()).isFalse();
    assertThat(MovementType.DP_REVERSAL.bucket()).isEqualTo(MovementType.Bucket.WRITTEN_OFF);
    assertThat(RemittanceStatus.APPROVED.isDerived()).isFalse();
  }

  @Test
  void bookRatesKeepTwoDecimals() {
    assertThat(RateType.BOOK.normalize(new BigDecimal("57.8561"))).isEqualByComparingTo("57.86");
    assertThat(RateType.BOOK.normalize(new BigDecimal("57.8561")).scale()).isEqualTo(2);
    assertThat(RateType.SPOT.normalize(new BigDecimal("57.8561")).scale()).isEqualTo(4);
    assertThat(RateType.BOOK.normalize(null)).isNull();
  }

  @Test
  void aRunEndsSucceededPartialOrFailed() {
    FlowInRun ok = new FlowInRun("F", "R1", Trigger.MANUAL, FlowInRun.FileRef.NONE, Instant.now());
    ok.count(RecordStatus.ACCEPTED);
    ok.count(null);
    ok.finish("done", null, Instant.now());
    assertThat(ok.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    assertThat(ok.getDuplicateCount()).isEqualTo(1);

    FlowInRun partial =
        new FlowInRun("F", "R2", Trigger.UPLOAD, FlowInRun.FileRef.NONE, Instant.now());
    partial.count(RecordStatus.ACCEPTED);
    partial.count(RecordStatus.FAILED);
    partial.finish("x".repeat(1200), null, Instant.now());
    assertThat(partial.getStatus()).isEqualTo(RunStatus.PARTIAL);
    assertThat(partial.getMessage()).hasSize(1000);

    FlowInRun failed =
        new FlowInRun("F", "R3", Trigger.EVENT, FlowInRun.FileRef.NONE, Instant.now());
    failed.count(RecordStatus.FAILED);
    failed.finish("failed", null, Instant.now());
    assertThat(failed.getStatus()).isEqualTo(RunStatus.FAILED);
  }
}
