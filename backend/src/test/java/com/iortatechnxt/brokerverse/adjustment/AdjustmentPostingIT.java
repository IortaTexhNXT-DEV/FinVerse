package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.BatchOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentPostingService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.catalog.service.PremiumCalculator;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.CapturedLedgerEvents;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Posting of endorsement requests through booking and the Operations ledger (ADJID.001/009/011-014,
 * OPERATIONS_DESIGN section 5 rows 16-19): financial plus and minus, flat, flat retain-DST and
 * partial cancellation with their amounts, the return invoice kept out of the balances, the
 * re-application waiting for cashiering and the AR Insurer of a remitted decrease.
 */
@IntegrationTest
class AdjustmentPostingIT {

  @Autowired private AdjustmentFixtures fx;
  @Autowired private AdjustmentPostingService posting;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private CapturedLedgerEvents events;
  @Autowired private AsUser as;

  private static BigDecimal change(EndorsementRequest r, LedgerComponent component) {
    return r.getChanges().stream()
        .filter(c -> c.component() == component)
        .map(ComponentChange::delta)
        .findFirst()
        .orElseThrow();
  }

  private static BigDecimal booked(OpsInvoice invoice, LedgerComponent component) {
    return invoice.component(component).getBooked();
  }

  private static PremiumComponents premiumOf(OpsInvoice i) {
    return new PremiumComponents(
        booked(i, LedgerComponent.BASIC),
        booked(i, LedgerComponent.DST),
        booked(i, LedgerComponent.PREMIUM_TAX_VAT),
        booked(i, LedgerComponent.LGT),
        booked(i, LedgerComponent.FST),
        booked(i, LedgerComponent.OTHER));
  }

  @Test
  void aPositiveFinancialEndorsementBooksAnAdditionalPremiumInvoice() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice,
            AdjustmentFixtures.terms("FIN_TSI", "TSI_CHANGE", null, FROM, new BigDecimal("200000")),
            AmountInput.NONE);

    assertThat(request.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(request.isNegative()).isFalse();
    BigDecimal premium = change(request, LedgerComponent.DTIP);
    assertThat(premium).isPositive();
    assertThat(request.getShares()).singleElement().satisfies(s -> assertThat(s.lead()).isTrue());
    assertThat(request.outcome().endorsementNo()).startsWith("EN-");
    OpsInvoice added = ledger.require(request.outcome().newInvoiceNo());
    assertThat(added.getKind()).isEqualTo(InvoiceKind.ENDORSEMENT_PLUS);
    assertThat(added.getGrossPremium()).isEqualByComparingTo(premium);
    assertThat(added.premiumBalance()).isEqualByComparingTo(premium);
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.premiumBalance()).isEqualByComparingTo(invoice.premiumBalance());
    assertThat(original.getLockOwner()).isNull();
    assertThat(ledger.adjustmentTotal(invoice.getInvoiceNo()))
        .hasValueSatisfying(t -> assertThat(t.getAdjustedPremium()).isEqualByComparingTo(premium));
    assertThat(request.getJournals()).isNotEmpty();
    assertThat(request.trail().postedBy()).isEqualTo(AdjustmentFixtures.PROCESSOR);
    assertThat(request.outcome().batchNo()).startsWith("VB-");
  }

  @Test
  void aNegativeFinancialEndorsementAdjustsTheOriginalInvoice() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice,
            AdjustmentFixtures.terms("FIN_PREMIUM_RATE", "PREMIUM_RATE_CHANGE", null, FROM, null),
            AdjustmentFixtures.basic("-1000"));

    assertThat(request.isNegative()).isTrue();
    assertThat(change(request, LedgerComponent.BASIC)).isEqualByComparingTo("-1000");
    BigDecimal commission = change(request, LedgerComponent.COMMISSION);
    assertThat(commission).isNegative();
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.component(LedgerComponent.BASIC).getBalance())
        .isEqualByComparingTo(
            booked(invoice, LedgerComponent.BASIC).subtract(new BigDecimal("1000")));
    assertThat(original.component(LedgerComponent.COMMISSION).getBalance())
        .isEqualByComparingTo(booked(invoice, LedgerComponent.COMMISSION).add(commission));
    assertThat(original.component(LedgerComponent.DTIP).getAdjusted())
        .isEqualByComparingTo("-1000");
    OpsInvoice returned = ledger.require(request.outcome().newInvoiceNo());
    assertThat(returned.getKind()).isEqualTo(InvoiceKind.ENDORSEMENT_MINUS);
    assertThat(returned.balances().values()).allSatisfy(b -> assertThat(b).isZero());
    assertThat(original.isPendingNegAdj()).isFalse();
  }

  @Test
  void aFlatCancellationReturnsEverythingAndFlagsTheInvoice() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);

    assertThat(change(request, LedgerComponent.DTIP))
        .isEqualByComparingTo(invoice.getGrossPremium().negate());
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.balances().values()).allSatisfy(b -> assertThat(b).isZero());
    assertThat(original.isCancelled()).isTrue();
    assertThat(original.isPendingNegAdj()).isFalse();
    assertThat(original.getLockOwner()).isNull();
    assertThat(ledger.require(request.outcome().newInvoiceNo()).getKind())
        .isEqualTo(InvoiceKind.CANCELLATION);
    assertThat(events.of(InvoiceFlagChanged.class))
        .anySatisfy(
            e -> {
              assertThat(e.invoiceNo()).isEqualTo(invoice.getInvoiceNo());
              assertThat(e.flag()).isEqualTo(InvoiceFlag.PENDING_NEG_ADJ);
              assertThat(e.value()).isTrue();
            });
  }

  @Test
  void aFlatCancellationRetainingDstKeepsTheStampTax() {
    OpsInvoice invoice = fx.invoice();
    BigDecimal dst = booked(invoice, LedgerComponent.DST);
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice,
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION_RETAIN_DST", FROM),
            AmountInput.NONE);

    assertThat(change(request, LedgerComponent.DST)).isZero();
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.component(LedgerComponent.DST).getBalance()).isEqualByComparingTo(dst);
    assertThat(original.component(LedgerComponent.BASIC).getBalance()).isZero();
    assertThat(original.component(LedgerComponent.DTIP).getBalance()).isEqualByComparingTo(dst);
    assertThat(original.premiumBalance()).isEqualByComparingTo(dst);
  }

  @Test
  void aPartialCancellationReturnsTheUnexpiredTermProRata() {
    OpsInvoice invoice = fx.invoice();
    LocalDate effective = FROM.plusMonths(6);
    BigDecimal factor =
        PremiumCalculator.proRataFactor(effective, invoice.getClassification().expiryDate());
    PremiumComponents expected = premiumOf(invoice).withoutDst().times(factor);
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice,
            AdjustmentFixtures.cancellation("PARTIAL_CANCELLATION", effective),
            AmountInput.NONE);

    assertThat(change(request, LedgerComponent.BASIC))
        .isEqualByComparingTo(expected.basic().negate());
    assertThat(change(request, LedgerComponent.DST)).isZero();
    assertThat(change(request, LedgerComponent.DTIP))
        .isEqualByComparingTo(expected.total().negate());
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.component(LedgerComponent.BASIC).getBalance())
        .isEqualByComparingTo(booked(invoice, LedgerComponent.BASIC).subtract(expected.basic()));
    assertThat(original.isCancelled()).isTrue();
  }

  @Test
  void aDecreaseOfAPaidInvoiceWaitsForCashieringToReapplyThePayments() {
    OpsInvoice invoice = fx.invoice();
    fx.payInFull(invoice);
    EndorsementRequest request =
        fx.toPosting(
            fx.raise(
                invoice,
                AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                AmountInput.NONE));
    PostingBatch batch = fx.post(request);

    assertThat(batch.getLines())
        .singleElement()
        .satisfies(l -> assertThat(l.outcome()).isEqualTo(BatchOutcome.AWAITING_REAPPLICATION));
    assertThat(batch.getPendingCount()).isEqualTo(1);
    EndorsementRequest pending = fx.reload(request);
    assertThat(pending.getStage()).isEqualTo(RequestStage.AWAITING_REAPPLICATION);
    assertThat(pending.trail().completedAt()).isNull();
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.isPendingNegAdj()).isTrue();
    assertThat(original.getLockOwner()).isNull();
    assertThat(original.premiumBalance()).isEqualByComparingTo(invoice.getGrossPremium().negate());
    assertThatThrownBy(
            () -> as.run(AdjustmentFixtures.PROCESSOR, () -> posting.reapply(request.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "PAYMENT_REAPPLIER_UNAVAILABLE");
  }

  @Test
  void aDecreaseAlreadyRemittedSetsUpTheArInsurer() {
    OpsInvoice invoice = fx.invoice();
    fx.payInFull(invoice);
    fx.remitInFull(invoice);
    BigDecimal dtip = booked(invoice, LedgerComponent.DTIP);
    EndorsementRequest request =
        fx.toPosting(
            fx.raise(
                invoice,
                AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                AmountInput.NONE));
    fx.post(request);

    EndorsementRequest posted = fx.reload(request);
    assertThat(posted.outcome().arInsurerAmount()).isEqualByComparingTo(dtip);
    assertThat(posted.getJournals()).hasSizeGreaterThanOrEqualTo(2);
    OpsInvoice original = fx.reload(invoice);
    assertThat(original.component(LedgerComponent.DTIP).getBalance()).isZero();
    assertThat(original.isPendingNegAdj()).isTrue();
  }

  @Test
  void aBatchPostsWhatQualifiesAndReportsTheFailures() {
    EndorsementRequest good =
        fx.toPosting(
            fx.raise(
                fx.invoice(),
                AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                AmountInput.NONE));
    EndorsementRequest draft =
        fx.raise(
            fx.invoice(),
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
            AmountInput.NONE);
    PostingBatch batch = fx.post(good, draft);

    assertThat(batch.getPostedCount()).isEqualTo(1);
    assertThat(batch.getFailedCount()).isEqualTo(1);
    assertThat(batch.getLines())
        .anySatisfy(
            l -> {
              assertThat(l.requestNo()).isEqualTo(draft.getRequestNo());
              assertThat(l.outcome()).isEqualTo(BatchOutcome.FAILED);
              assertThat(l.message()).contains("DRAFT");
            });
    assertThat(fx.reload(good).outcome().batchNo()).isEqualTo(batch.getBatchNo());
    assertThat(fx.reload(draft).getStage()).isEqualTo(RequestStage.DRAFT);
  }
}
