package com.iortatechnxt.brokerverse.collections;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Collections test data: invoices booked through the real booking service (single year or a
 * three-year account), and payments applied on the ledger as Cashiering would.
 */
@Component
public class CollectionsFixtures {

  /** Start of the three-year test accounts. */
  public static final LocalDate MULTI_YEAR_FROM = LocalDate.of(2026, 10, 1);

  private final OpsLedgerFixtures ledgerFx;
  private final BookingFixtures bookingFx;
  private final BookingService booking;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final AsUser as;
  private final TransactionTemplate tx;

  CollectionsFixtures(
      OpsLedgerFixtures ledgerFx,
      BookingFixtures bookingFx,
      BookingService booking,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      AsUser as,
      TransactionTemplate tx) {
    this.ledgerFx = ledgerFx;
    this.bookingFx = bookingFx;
    this.booking = booking;
    this.ledger = ledger;
    this.writer = writer;
    this.as = as;
    this.tx = tx;
  }

  /** The demo company. */
  public Long company() {
    return ledgerFx.company();
  }

  /** A new unpaid motor invoice in the ledger. */
  public OpsInvoice motorInvoice() {
    return ledgerFx.motorInvoice();
  }

  /** A new direct payment invoice in the ledger (no premium to collect). */
  public OpsInvoice directPaymentInvoice() {
    return ledgerFx.ledgerOf(ledgerFx.bookDirectPayment());
  }

  /** A three-year property account from 2026-10-01, booked: year 1 in the ledger. */
  public BookedInvoice threeYearAccount() {
    Account account = bookingFx.multiYear(MULTI_YEAR_FROM, 3);
    return as.run(
        "proc",
        () ->
            booking.book(
                account.getArn(),
                BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                BookingSource.INDIVIDUAL));
  }

  /** The ledger invoice. */
  public OpsInvoice invoice(String invoiceNo) {
    return ledger.require(invoiceNo);
  }

  /** Applies a payment to the basic premium of an invoice. */
  public void pay(String invoiceNo, String amount, LocalDate valueDate) {
    as.run(
        "cashier",
        () ->
            tx.execute(
                s ->
                    writer.post(
                        new MovementRequest(
                            invoiceNo,
                            MovementType.APPLIED,
                            "CASHIERING",
                            "APP:CLX-" + BookingFixtures.token(),
                            valueDate,
                            Map.of(LedgerComponent.BASIC, new BigDecimal(amount)),
                            null,
                            "Collections test payment"))));
  }

  /** Applies payments to every premium component so nothing is left to collect. */
  public void payInFull(String invoiceNo, LocalDate valueDate) {
    OpsInvoice invoice = as.run("cashier", () -> tx.execute(s -> loaded(invoiceNo)));
    as.run(
        "cashier",
        () ->
            tx.execute(
                s ->
                    writer.post(
                        new MovementRequest(
                            invoiceNo,
                            MovementType.APPLIED,
                            "CASHIERING",
                            "APP:CLX-FULL-" + BookingFixtures.token(),
                            valueDate,
                            invoice.balances().entrySet().stream()
                                .filter(e -> e.getKey().isPremiumReceivable())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                            null,
                            "Collections test payment in full"))));
  }

  private OpsInvoice loaded(String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    invoice.loadCollections();
    return invoice;
  }

  /** A unique token. */
  public static String token() {
    return BookingFixtures.token();
  }
}
