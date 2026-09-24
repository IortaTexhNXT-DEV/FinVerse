package com.iortatechnxt.brokerverse.opsledger;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import org.springframework.stereotype.Component;

/**
 * Operations ledger test data: accounts booked through the real booking service, whose invoices
 * reach the ledger through the feed listener after commit.
 */
@Component
public class OpsLedgerFixtures {

  private final BookingFixtures booking;
  private final BookingService bookings;
  private final InvoiceLedgerQueryService ledger;
  private final AsUser as;

  OpsLedgerFixtures(
      BookingFixtures booking,
      BookingService bookings,
      InvoiceLedgerQueryService ledger,
      AsUser as) {
    this.booking = booking;
    this.bookings = bookings;
    this.ledger = ledger;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return booking.company();
  }

  /** Books a new motor account (paid via BDOI) and returns its booked invoice. */
  public BookedInvoice bookMotor() {
    return book(booking.motor());
  }

  /** Books a new direct payment motor account. */
  public BookedInvoice bookDirectPayment() {
    return book(booking.directPaymentMotor());
  }

  private BookedInvoice book(Account account) {
    return as.run(
        "proc",
        () ->
            bookings.book(
                account.getArn(),
                BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                BookingSource.INDIVIDUAL));
  }

  /** The ledger invoice of a booked invoice. */
  public OpsInvoice ledgerOf(BookedInvoice invoice) {
    return ledger.require(invoice.getInvoiceNo());
  }

  /** A motor invoice in the ledger. */
  public OpsInvoice motorInvoice() {
    return ledgerOf(bookMotor());
  }
}
