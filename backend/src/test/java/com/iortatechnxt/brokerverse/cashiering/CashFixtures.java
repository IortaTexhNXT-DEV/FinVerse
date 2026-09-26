package com.iortatechnxt.brokerverse.cashiering;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.ArIssue;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cashiering test data: invoices booked through the real booking service (they reach the Operations
 * ledger after commit) and payments received over the counter.
 */
@Component
public class CashFixtures {

  private final BookingFixtures booking;
  private final BookingService bookings;
  private final InvoiceLedgerQueryService ledger;
  private final PaymentIntakeService intake;
  private final CashReceiptService receipts;
  private final TestData data;
  private final AsUser as;
  private final TransactionTemplate tx;

  CashFixtures(
      BookingFixtures booking,
      BookingService bookings,
      InvoiceLedgerQueryService ledger,
      PaymentIntakeService intake,
      CashReceiptService receipts,
      TestData data,
      AsUser as,
      TransactionTemplate tx) {
    this.booking = booking;
    this.bookings = bookings;
    this.ledger = ledger;
    this.intake = intake;
    this.receipts = receipts;
    this.data = data;
    this.as = as;
    this.tx = tx;
  }

  /** The seed company. */
  public Long company() {
    return booking.company();
  }

  /** Head Office. */
  public Long ho() {
    return data.branch("HO").getId();
  }

  /** A branch by code. */
  public Long branch(String code) {
    return data.branch(code).getId();
  }

  /** A new motor account, issued and not booked (pre-booked). */
  public Account unbooked() {
    return booking.motor();
  }

  /** A new booked motor invoice (paid via BDOI). */
  public OpsInvoice motorInvoice() {
    return book(booking.motor(), null);
  }

  /** A new booked motor invoice of a client withholding 2% CWT. */
  public OpsInvoice cwtInvoice() {
    return book(booking.motor(), Boolean.TRUE);
  }

  /** Books an account. */
  public OpsInvoice book(Account account, Boolean cwt) {
    BookedInvoice booked =
        as.run(
            "proc",
            () ->
                bookings.book(
                    account.getArn(),
                    new BookingOptions(BookingFixtures.BOOKED_ON, null, cwt, null),
                    BookingSource.INDIVIDUAL));
    return invoice(booked.getInvoiceNo());
  }

  /** The ledger invoice with its components. */
  public OpsInvoice invoice(String invoiceNo) {
    return tx.execute(
        s -> {
          OpsInvoice i = ledger.require(invoiceNo);
          i.loadCollections();
          return i;
        });
  }

  /** Receives an over-the-counter cash payment as a user. */
  public IntakeResult pay(String user, String reference, BigDecimal amount) {
    return as.run(
        user,
        () ->
            intake.receive(
                new IntakeTarget(company(), ho(), "OTC", ReceiptSource.OTC),
                new PaymentIntake(
                    PaymentChannel.OTC,
                    null,
                    "T:" + UUID.randomUUID(),
                    null,
                    reference,
                    List.of(),
                    new PaymentIntake.Payor(null, "Test Payor " + reference),
                    null,
                    new PaymentIntake.Money(amount, "PHP", LocalDate.now()),
                    PaymentIntake.Tender.of(PaymentMode.CASH))));
  }

  /** The receipt service. */
  public CashReceiptService receiptService() {
    return receipts;
  }

  /** A non-premium AR (AR Insurance) of an insurer. */
  public ArIssue nonPremium(String insurer, BigDecimal amount) {
    return new ArIssue(
        company(),
        ho(),
        "AR_INSURANCE",
        LocalDate.now(),
        insurer,
        "Insurer payment",
        null,
        null,
        "PHP",
        amount,
        new ReceiptTender(
            PaymentMode.CHECK,
            "CHK-" + UUID.randomUUID().toString().substring(0, 8),
            "BDO",
            null,
            null,
            ReceiptSource.OTC,
            null,
            null,
            "Refund of remitted premium"));
  }

  /** Receives a payment as the cashier. */
  public IntakeResult pay(String reference, BigDecimal amount) {
    return pay("cashier", reference, amount);
  }
}
