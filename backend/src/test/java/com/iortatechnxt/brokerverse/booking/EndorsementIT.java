package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService.InvoiceItem;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService.EndorsementPreview;
import com.iortatechnxt.brokerverse.booking.service.EndorsementResult;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumCalculator;
import com.iortatechnxt.brokerverse.catalog.service.RateResolver;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Positive, negative and non-financial endorsements and every cancellation kind, with amounts. */
@IntegrationTest
class EndorsementIT {

  private static final LocalDate POSTED_ON = LocalDate.of(2026, 9, 20);
  private static final LocalDate MID_TERM = LocalDate.of(2027, 4, 1);

  @Autowired private BookingFixtures fx;
  @Autowired private BookingService booking;
  @Autowired private EndorsementPostingService endorsements;
  @Autowired private BookingQueryService queries;
  @Autowired private ServiceInvoiceRegister register;
  @Autowired private AccountQueryService accounts;
  @Autowired private RatingService rating;
  @Autowired private RateResolver rates;
  @Autowired private CapturedInvoiceEvents events;
  @Autowired private AsUser as;

  private BookedInvoice booked() {
    Account account = fx.motor();
    return as.run(
        "proc",
        () ->
            booking.book(
                account.getArn(),
                BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                BookingSource.INDIVIDUAL));
  }

  private static EndorsementPosting financial(
      String arn, EndorsementType type, String change, String reference) {
    return new EndorsementPosting(
        arn,
        type,
        null,
        MID_TERM,
        PeriodBasis.PRO_RATA,
        new BigDecimal(change),
        null,
        null,
        null,
        "Sum insured change " + change,
        null,
        POSTED_ON,
        reference);
  }

  private static EndorsementPosting cancellation(
      String arn, CancellationKind kind, PeriodBasis basis, LocalDate effective) {
    return new EndorsementPosting(
        arn,
        EndorsementType.CANCELLATION,
        kind,
        effective,
        basis,
        null,
        null,
        null,
        null,
        "Cancellation " + kind,
        "CLIENT_REQUEST",
        POSTED_ON,
        null);
  }

  private BookedInvoice invoiceOf(EndorsementResult result) {
    return queries.byNo(result.invoiceNo());
  }

  @Test
  void aPositiveEndorsementBooksAdditionalPremiumWithItsServiceInvoice() {
    BookedInvoice original = booked();
    EndorsementPosting posting =
        financial(
            original.getArn(),
            EndorsementType.POSITIVE,
            "200000",
            "TEST:" + BookingFixtures.token());
    EndorsementPreview preview = as.run("proc", () -> endorsements.preview(posting));
    EndorsementResult result = as.run("proc", () -> endorsements.post(posting));

    PremiumBreakdown expected =
        rating
            .rate(
                new RatingQuery(
                    original.getCompanyId(),
                    "MTR10",
                    "INS-MGIC",
                    "MKT",
                    List.of(
                        new RatingQuery.Item(
                            "Endorsement", new BigDecimal("200000"), null, null, null)),
                    false,
                    PeriodBasis.PRO_RATA,
                    MID_TERM,
                    original.getExpiryDate(),
                    original.getCommission().rate(),
                    true,
                    MID_TERM))
            .breakdown();
    BookedInvoice invoice = invoiceOf(result);
    assertThat(result.endorsementNo()).startsWith("EN-2026-");
    assertThat(invoice.getKind()).isEqualTo(InvoiceKind.ENDORSEMENT_PLUS);
    assertThat(invoice.getEndorsementNo()).isEqualTo(result.endorsementNo());
    assertThat(invoice.getParentInvoiceNo()).isEqualTo(original.getInvoiceNo());
    assertThat(invoice.getPremium().basic()).isEqualByComparingTo(expected.netPremium());
    assertThat(invoice.getPremium().total()).isEqualByComparingTo(expected.grossPremium());
    assertThat(invoice.getCommission().commission()).isEqualByComparingTo(expected.commission());
    assertThat(preview.invoice().premium().total()).isEqualByComparingTo(expected.grossPremium());
    assertThat(preview.journal()).isNotEmpty();
    assertThat(result.journalBatches()).hasSize(1);
    assertThat(register.forInvoice(invoice.getInvoiceNo()))
        .singleElement()
        .satisfies(s -> assertThat(s.getTypeCode()).isEqualTo("INSURER_COMMISSION_ENDT"));
    InvoiceBooked event = events.forInvoice(invoice.getInvoiceNo()).orElseThrow();
    assertThat(event.kind()).isEqualTo(InvoiceKind.ENDORSEMENT_PLUS);
    assertThat(event.endorsementNo()).isEqualTo(result.endorsementNo());
    assertThat(original.getRootInvoiceNo()).isEqualTo(original.getInvoiceNo());
    assertThat(invoice.getRootInvoiceNo()).isEqualTo(original.getInvoiceNo());
    assertThat(event.rootInvoiceNo()).isEqualTo(original.getInvoiceNo());

    EndorsementResult replay = as.run("proc", () -> endorsements.post(posting));
    assertThat(replay.endorsementNo()).isEqualTo(result.endorsementNo());
    assertThat(queries.endorsements(original.getArn())).hasSize(1);
  }

  @Test
  void aNegativeEndorsementReturnsPremiumAndCreditsTheServiceInvoice() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "adjust",
            () ->
                endorsements.post(
                    financial(original.getArn(), EndorsementType.NEGATIVE, "-300000", null)));
    BookedInvoice invoice = invoiceOf(result);
    assertThat(invoice.getKind()).isEqualTo(InvoiceKind.ENDORSEMENT_MINUS);
    assertThat(invoice.getPremium().total()).isNegative();
    assertThat(invoice.getCommission().commission()).isNegative();
    List<InvoiceItem> items = queries.openItems(queries.get(invoice.getId()));
    assertThat(items)
        .filteredOn(i -> i.role() == OpenItemRole.CLIENT_PREMIUM)
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.item().getDirection()).isEqualTo(ItemDirection.CREDIT);
              assertThat(i.item().getDocumentType()).isEqualTo("RETURN_PREMIUM");
            });
    List<ServiceInvoice> credits = register.forInvoice(invoice.getInvoiceNo());
    assertThat(credits)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.getKind()).isEqualTo(SiKind.CREDIT);
              assertThat(c.getCreditOf()).isEqualTo(original.getServiceInvoiceNo());
              assertThat(c.getCommission())
                  .isEqualByComparingTo(invoice.getCommission().commission().negate());
            });
    assertThat(events.forInvoice(invoice.getInvoiceNo()).orElseThrow().grossPremium()).isNegative();
  }

  @Test
  void aNonFinancialEndorsementIsRecordedWithoutPosting() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "proc",
            () ->
                endorsements.post(
                    new EndorsementPosting(
                        original.getArn(),
                        EndorsementType.NON_FINANCIAL,
                        null,
                        MID_TERM,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Change of contact number",
                        null,
                        null,
                        null)));
    assertThat(result.invoiceNo()).isNull();
    assertThat(result.journalBatches()).isEmpty();
    assertThat(queries.endorsements(original.getArn()))
        .singleElement()
        .satisfies(e -> assertThat(e.getType()).isEqualTo(EndorsementType.NON_FINANCIAL));
    assertThat(queries.invoicesForArn(original.getArn())).hasSize(1);
  }

  @Test
  void aFlatCancellationReturnsEverythingAndCancelsTheAccount() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "adjust",
            () ->
                endorsements.post(
                    cancellation(
                        original.getArn(),
                        CancellationKind.FLAT,
                        null,
                        original.getInceptionDate())));
    BookedInvoice invoice = invoiceOf(result);
    assertThat(invoice.getKind()).isEqualTo(InvoiceKind.CANCELLATION);
    assertThat(invoice.getPremium()).isEqualTo(original.getPremium().negate());
    assertThat(invoice.getCommission().commission())
        .isEqualByComparingTo(original.getCommission().commission().negate());
    assertThat(accounts.requireByArn(original.getArn()).getStatus())
        .isEqualTo(AccountStatus.CANCELLED);
    assertThatThrownBy(
            () ->
                as.run(
                    "adjust",
                    () ->
                        endorsements.post(
                            financial(original.getArn(), EndorsementType.NEGATIVE, "-1000", null))))
        .extracting("code")
        .isEqualTo("ACCOUNT_NOT_BOOKED");
  }

  @Test
  void aFlatCancellationRetainingDstKeepsTheStampTax() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "adjust",
            () ->
                endorsements.post(
                    cancellation(
                        original.getArn(),
                        CancellationKind.FLAT_RETAIN_DST,
                        null,
                        original.getInceptionDate())));
    PremiumComponents returned = invoiceOf(result).getPremium();
    assertThat(returned.dst()).isZero();
    assertThat(returned.basic()).isEqualByComparingTo(original.getPremium().basic().negate());
  }

  @Test
  void aPartialProRataCancellationReturnsTheUnexpiredPremium() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "adjust",
            () ->
                endorsements.post(
                    cancellation(
                        original.getArn(),
                        CancellationKind.PARTIAL,
                        PeriodBasis.PRO_RATA,
                        MID_TERM)));
    BigDecimal factor = PremiumCalculator.proRataFactor(MID_TERM, original.getExpiryDate());
    PremiumComponents returned = invoiceOf(result).getPremium();
    assertThat(returned.basic())
        .isEqualByComparingTo(
            original
                .getPremium()
                .basic()
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP)
                .negate());
    assertThat(returned.dst()).isZero();
  }

  @Test
  void aPartialShortPeriodCancellationKeepsTheShortPeriodPremium() {
    BookedInvoice original = booked();
    EndorsementResult result =
        as.run(
            "adjust",
            () ->
                endorsements.post(
                    cancellation(
                        original.getArn(),
                        CancellationKind.PARTIAL,
                        PeriodBasis.SHORT_PERIOD,
                        MID_TERM)));
    BigDecimal retained = rates.shortPeriodPercent(6, MID_TERM).orElseThrow();
    BigDecimal factor =
        BigDecimal.ONE.subtract(retained.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
    assertThat(invoiceOf(result).getPremium().basic())
        .isEqualByComparingTo(
            original
                .getPremium()
                .basic()
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP)
                .negate());
  }

  @Test
  void invalidEndorsementsAreRefused() {
    BookedInvoice original = booked();
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        endorsements.post(
                            financial(original.getArn(), EndorsementType.POSITIVE, "-5000", null))))
        .extracting("code")
        .isEqualTo("ENDORSEMENT_SIGN_MISMATCH");
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        endorsements.post(
                            new EndorsementPosting(
                                original.getArn(),
                                EndorsementType.POSITIVE,
                                null,
                                LocalDate.of(2030, 1, 1),
                                null,
                                BigDecimal.TEN,
                                null,
                                null,
                                null,
                                "Too late",
                                null,
                                POSTED_ON,
                                null))))
        .extracting("code")
        .isEqualTo("ENDORSEMENT_DATE_OUTSIDE_TERM");
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> endorsements.post(cancellation(original.getArn(), null, null, MID_TERM))))
        .extracting("code")
        .isEqualTo("CANCELLATION_KIND_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(
                    "adjust",
                    () ->
                        endorsements.post(
                            new EndorsementPosting(
                                original.getArn(),
                                EndorsementType.NEGATIVE,
                                null,
                                MID_TERM,
                                null,
                                null,
                                null,
                                new PremiumComponents(
                                    new BigDecimal("-999999"), null, null, null, null, null),
                                null,
                                "Too much",
                                null,
                                POSTED_ON,
                                null))))
        .extracting("code")
        .isEqualTo("RETURN_EXCEEDS_PREMIUM");
    Account notBooked = fx.motor();
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        endorsements.post(
                            financial(notBooked.getArn(), EndorsementType.POSITIVE, "1000", null))))
        .extracting("code")
        .isEqualTo("ACCOUNT_NOT_BOOKED");
  }

  @Test
  void componentsGivenByTheCallerArePostedAsIs() {
    BookedInvoice original = booked();
    PremiumComponents given =
        new PremiumComponents(
            new BigDecimal("1000"), new BigDecimal("125"), new BigDecimal("120"), null, null, null);
    EndorsementResult result =
        as.run(
            "proc",
            () ->
                endorsements.post(
                    new EndorsementPosting(
                        original.getArn(),
                        EndorsementType.POSITIVE,
                        null,
                        MID_TERM,
                        null,
                        null,
                        null,
                        given,
                        null,
                        "Recomputed by Adjustment",
                        null,
                        POSTED_ON,
                        null)));
    BookedInvoice invoice = invoiceOf(result);
    assertThat(invoice.getPremium()).isEqualTo(given);
    assertThat(invoice.getCommission().commission())
        .isEqualByComparingTo(
            new BigDecimal("1000")
                .multiply(original.getCommission().rate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
  }
}
