package com.iortatechnxt.brokerverse.remittance.seed;

import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.service.HoldService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The seeded remittance hold request (seed profile only, idempotent), made after every other seed
 * so that the invoice numbers of the seed storyline stay as they are: the motor account of
 * ARN-2026-940008 (V2003) is booked, and Marketing Collection ({@code mktcoll}) asks to hold its
 * invoice; the request waits for the Marketing Team Leader's approval. No other team works on the
 * invoice, so the hold can be approved (the earlier seed hold sat on an invoice that the seeded
 * flat cancellation locks for Adjustment).
 */
@Component
@Profile("seed")
@Order(190)
public class HoldSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(HoldSeedData.class);
  private static final String HELD_ARN = "ARN-2026-940008";
  private static final String HOLD_REQUESTER = "mktcoll";
  private static final int HOLD_DAYS = 30;

  private final BookingService booking;
  private final HoldService holds;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param booking booking of the seed account
   * @param holds hold requests
   * @param ledger ledger reads (idempotency)
   * @param clock clock
   * @param users seed sign-in
   */
  public HoldSeedData(
      BookingService booking,
      HoldService holds,
      InvoiceLedgerQueryService ledger,
      Clock clock,
      SeedUsers users) {
    this.booking = booking;
    this.holds = holds;
    this.ledger = ledger;
    this.clock = clock;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!ledger.forArn(HELD_ARN).isEmpty()) {
      return;
    }
    try {
      LocalDate today = BusinessClock.today(clock);
      booking.book(HELD_ARN, BookingOptions.of(today, null), BookingSource.INDIVIDUAL);
      List<OpsInvoice> invoices = ledger.forArn(HELD_ARN);
      if (invoices.isEmpty()) {
        return;
      }
      OpsInvoice invoice = invoices.get(0);
      users.as(
          HOLD_REQUESTER,
          () ->
              holds.create(
                  invoice.getCompanyId(),
                  invoice.getInvoiceNo(),
                  new Terms(
                      "OTHERS",
                      "Client disputes the premium; hold until Marketing confirms",
                      today.plusDays(HOLD_DAYS)),
                  true,
                  RequestSource.SCREEN));
    } catch (RuntimeException ex) {
      LOG.warn("Seed hold request skipped: {}", ex.getMessage());
    }
  }
}
