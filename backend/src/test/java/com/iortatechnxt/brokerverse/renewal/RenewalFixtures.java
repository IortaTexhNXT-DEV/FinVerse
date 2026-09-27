package com.iortatechnxt.brokerverse.renewal;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Renewal test data: accounts booked through the real booking service with an expiry of their own,
 * then extracted as renewals through the extraction service.
 */
@Component
public class RenewalFixtures {

  /** Marketing Team Leader (T-CBG1). */
  public static final String TL = "mkttl";

  /** Marketing AO (T-CBG1). */
  public static final String AO = "ao";

  /** Processing Team Leader. */
  public static final String PROC_TL = "proctl";

  /** Processing Officer. */
  public static final String PO = "proc";

  /** Business administrator (Renewal Setup). */
  public static final String ADMIN = "badmin";

  private static final LocalDate FIRST_START = LocalDate.of(2026, 10, 2);
  private static final int SPREAD = 85;
  private static final AtomicInteger NEXT = new AtomicInteger((int) (System.nanoTime() % SPREAD));

  private final BookingFixtures booking;
  private final BookingService bookings;
  private final ExtractionService extraction;
  private final RenewalCandidateRepository candidates;
  private final AsUser as;

  RenewalFixtures(
      BookingFixtures booking,
      BookingService bookings,
      ExtractionService extraction,
      RenewalCandidateRepository candidates,
      AsUser as) {
    this.booking = booking;
    this.bookings = bookings;
    this.extraction = extraction;
    this.candidates = candidates;
    this.as = as;
  }

  /** The seed company. */
  public Long company() {
    return booking.company();
  }

  /**
   * Books a one-year account of a product with an expiry of its own.
   *
   * @param product product
   * @param segment segment
   * @return booked invoice
   */
  public BookedInvoice book(String product, String segment) {
    LocalDate from = FIRST_START.plusDays(NEXT.getAndIncrement() % SPREAD);
    Account account =
        booking.issued(
            new BookingFixtures.Spec(
                product, segment, PaymentArrangement.VIA_BDOI, from, from.plusYears(1), 1));
    return as.run(
        PO,
        () ->
            bookings.book(
                account.getArn(),
                BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                BookingSource.INDIVIDUAL));
  }

  /**
   * Books a CBG motor account and extracts it as a renewal.
   *
   * @return the renewal
   */
  public RenewalCandidate extractedMotor() {
    return extracted(book("MTR10", "CBG"));
  }

  /**
   * Extracts a booked invoice as a renewal (Generate Expiry List on its expiry date).
   *
   * @param invoice booked invoice
   * @return the renewal
   */
  public RenewalCandidate extracted(BookedInvoice invoice) {
    LocalDate expiry = invoice.getExpiryDate();
    as.run(TL, () -> extraction.extractRange(company(), expiry, expiry));
    return candidates
        .findByCompanyIdAndExpiringInvoiceNo(company(), invoice.getInvoiceNo())
        .orElseThrow();
  }

  /**
   * Reloads a renewal.
   *
   * @param c renewal
   * @return fresh copy
   */
  public RenewalCandidate reload(RenewalCandidate c) {
    return candidates.findById(c.getId()).orElseThrow();
  }
}
