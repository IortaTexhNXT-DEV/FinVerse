package com.iortatechnxt.brokerverse.booking.seed;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPostingService;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Books the seed accounts of V988 through the real booking service (seed profile only, idempotent):
 * three accounts via BDOI and one direct-payment account, a positive endorsement and a partial
 * cancellation - with their journals, open items, service invoices and events exactly as in
 * production.
 */
@Component
@Profile("seed")
@Order(80)
public class BookingSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(BookingSeedData.class);
  private static final String FIRST = "ARN-2026-940001";

  private final BookingService booking;
  private final EndorsementPostingService endorsements;
  private final BookedInvoiceRepository invoices;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param booking booking service
   * @param endorsements endorsement posting
   * @param invoices booked invoices (idempotency)
   * @param clock clock
   */
  public BookingSeedData(
      BookingService booking,
      EndorsementPostingService endorsements,
      BookedInvoiceRepository invoices,
      Clock clock) {
    this.booking = booking;
    this.endorsements = endorsements;
    this.invoices = invoices;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!invoices.findByArnOrderByPolicyYearAscIdAsc(FIRST).isEmpty()) {
      return;
    }
    int booked = 0;
    booked += book(FIRST, LocalDate.parse("2026-09-15"));
    booked += book("ARN-2026-940002", LocalDate.parse("2026-09-15"));
    booked += book("ARN-2026-940003", LocalDate.parse("2026-09-16"));
    booked += book("ARN-2026-940004", LocalDate.parse("2026-09-17"));
    post(
        new EndorsementPosting(
            FIRST,
            EndorsementType.POSITIVE,
            null,
            LocalDate.parse("2026-10-01"),
            PeriodBasis.PRO_RATA,
            new BigDecimal("200000"),
            null,
            null,
            null,
            "Accessories added: sum insured increased by PHP 200,000",
            null,
            date(LocalDate.parse("2026-09-20")),
            "SEED:ENDT:940001"));
    post(
        new EndorsementPosting(
            "ARN-2026-940002",
            EndorsementType.CANCELLATION,
            CancellationKind.PARTIAL,
            LocalDate.parse("2027-03-01"),
            PeriodBasis.PRO_RATA,
            null,
            null,
            null,
            null,
            "Property sold; cover cancelled mid-term at the client's request",
            "CLIENT_REQUEST",
            date(LocalDate.parse("2026-09-22")),
            "SEED:CANCEL:940002"));
    LOG.info("Booking seed data: {} accounts booked, one endorsement and one cancellation", booked);
  }

  private int book(String arn, LocalDate date) {
    try {
      booking.book(arn, BookingOptions.of(date(date), null), BookingSource.INDIVIDUAL);
      return 1;
    } catch (RuntimeException ex) {
      LOG.warn("Seed booking of {} skipped: {}", arn, ex.getMessage());
      return 0;
    }
  }

  private void post(EndorsementPosting posting) {
    try {
      endorsements.post(posting);
    } catch (RuntimeException ex) {
      LOG.warn("Seed endorsement of {} skipped: {}", posting.arn(), ex.getMessage());
    }
  }

  private LocalDate date(LocalDate wanted) {
    LocalDate today = LocalDate.now(clock);
    return wanted.isAfter(today) ? today : wanted;
  }
}
