package com.iortatechnxt.brokerverse.opsledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveEvaluation;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveStatus;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveTrigger;
import com.iortatechnxt.brokerverse.booking.service.IncentiveEvaluationService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The incentive indicator of a booked transaction is set only when it is booked and fully paid (New
 * Business BRNB.107, FR-NB-118; Renewal BRRN.041, FR-RN-087), every evaluation is kept with its
 * trigger, and the ledger carries the indicator.
 */
@IntegrationTest
class IncentiveAtFullPaymentIT {

  private static final LocalDate PAID_ON = LocalDate.of(2026, 9, 18);

  @Autowired private OpsLedgerFixtures fx;
  @Autowired private InvoiceLedgerQueryService queries;
  @Autowired private InvoiceLedgerService ledger;
  @Autowired private IncentiveEvaluationService incentives;
  @Autowired private BookedInvoiceRepository invoices;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;

  private void pay(OpsInvoice invoice, Map<LedgerComponent, BigDecimal> amounts) {
    as.run(
        "proc",
        () ->
            tx.execute(
                s ->
                    ledger.post(
                        new MovementRequest(
                            invoice.getInvoiceNo(),
                            MovementType.APPLIED,
                            "CASHIERING",
                            "APP:" + BookingFixtures.token(),
                            PAID_ON,
                            amounts,
                            new MovementRequest.DocumentRefs(
                                "AR-TEST-" + BookingFixtures.token(), null, null, null),
                            "Test payment"))));
  }

  private BookedInvoice reload(BookedInvoice i) {
    return tx.execute(s -> invoices.findByInvoiceNo(i.getInvoiceNo()).orElseThrow());
  }

  @Test
  void theIndicatorIsPendingUntilTheInvoiceIsFullyPaid() {
    BookedInvoice booked = fx.bookMotor();
    assertThat(booked.getIncentive().status()).isEqualTo(IncentiveStatus.PENDING);
    OpsInvoice invoice = fx.ledgerOf(booked);
    assertThat(invoice.isIncentiveEligible()).isFalse();

    BigDecimal dst = invoice.component(LedgerComponent.DST).getBooked();
    pay(invoice, Map.of(LedgerComponent.DST, dst));
    assertThat(queries.require(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
    assertThat(reload(booked).getIncentive().status()).isEqualTo(IncentiveStatus.PENDING);

    OpsInvoice partial = queries.require(invoice.getInvoiceNo());
    Map<LedgerComponent, BigDecimal> rest = new EnumMap<>(LedgerComponent.class);
    partial.getComponents().stream()
        .filter(c -> c.getComponent().isPremiumReceivable() && c.getBalance().signum() > 0)
        .forEach(c -> rest.put(c.getComponent(), c.getBalance()));
    pay(invoice, rest);
    assertThat(queries.require(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);

    BookedInvoice evaluated = reload(booked);
    assertThat(evaluated.getIncentive().status()).isNotEqualTo(IncentiveStatus.PENDING);
    boolean eligible = evaluated.getIncentive().status() == IncentiveStatus.ELIGIBLE;
    assertThat(evaluated.getFlags().incentiveEligible()).isEqualTo(eligible);
    assertThat(queries.require(invoice.getInvoiceNo()).isIncentiveEligible()).isEqualTo(eligible);
    List<IncentiveEvaluation> history =
        as.run("proc", () -> incentives.history(booked.getInvoiceNo()));
    assertThat(history)
        .extracting(IncentiveEvaluation::getTrigger)
        .containsSubsequence(IncentiveTrigger.BOOKING, IncentiveTrigger.FULL_PAYMENT);
    assertThat(history.get(0).getResult()).isEqualTo(IncentiveStatus.PENDING);
  }

  @Test
  void aCancellationInvalidatesTheIndicator() {
    BookedInvoice booked = fx.bookMotor();
    as.run(
        "proc",
        () ->
            tx.execute(
                s -> incentives.evaluate(booked.getInvoiceNo(), IncentiveTrigger.CANCELLATION)));
    BookedInvoice cancelled = reload(booked);
    assertThat(cancelled.getIncentive().status()).isEqualTo(IncentiveStatus.NOT_ELIGIBLE);
    assertThat(cancelled.getIncentive().reason()).contains("cancelled");
    assertThat(cancelled.getFlags().incentiveEligible()).isFalse();
  }
}
