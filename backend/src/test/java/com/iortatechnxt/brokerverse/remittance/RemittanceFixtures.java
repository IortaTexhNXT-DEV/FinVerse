package com.iortatechnxt.brokerverse.remittance;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Remittance test data: motor invoices booked through the real booking service, payments applied on
 * the ledger as cashiering would post them, and batches extracted per invoice so each test works on
 * its own invoices.
 */
@Component
public class RemittanceFixtures {

  /** Value date of the test payments (well past the check holding period). */
  public static final LocalDate PAID_ON = LocalDate.of(2026, 9, 15);

  private final OpsLedgerFixtures ledgerFixtures;
  private final BookingFixtures booking;
  private final BookingService bookings;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final ExtractionService extraction;
  private final BatchService batches;
  private final BatchLineRepository lines;
  private final TransactionTemplate tx;
  private final AsUser as;

  RemittanceFixtures(
      OpsLedgerFixtures ledgerFixtures,
      BookingFixtures booking,
      BookingService bookings,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      ExtractionService extraction,
      BatchService batches,
      BatchLineRepository lines,
      TransactionTemplate tx,
      AsUser as) {
    this.ledgerFixtures = ledgerFixtures;
    this.booking = booking;
    this.bookings = bookings;
    this.ledger = ledger;
    this.writer = writer;
    this.extraction = extraction;
    this.batches = batches;
    this.lines = lines;
    this.tx = tx;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return ledgerFixtures.company();
  }

  /** A new unpaid motor invoice (CBG). */
  public OpsInvoice invoice() {
    return ledgerFixtures.motorInvoice();
  }

  /** A new unpaid motor invoice of a segment. */
  public OpsInvoice invoice(String segment) {
    Account account =
        booking.issued(BookingFixtures.spec("MTR10", segment, PaymentArrangement.VIA_BDOI));
    var booked =
        as.run(
            "proc",
            () ->
                bookings.book(
                    account.getArn(),
                    BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                    BookingSource.INDIVIDUAL));
    return ledger.require(booked.getInvoiceNo());
  }

  /** A new motor invoice fully paid on {@link #PAID_ON}. */
  public OpsInvoice paidInvoice() {
    OpsInvoice invoice = invoice();
    payInFull(invoice, PAID_ON);
    return ledger.require(invoice.getInvoiceNo());
  }

  /** Applies the whole premium of an invoice as cashiering would. */
  public void payInFull(OpsInvoice invoice, LocalDate paidOn) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      OpsInvoiceComponent row = invoice.component(c);
      amounts.put(c, row.getBalance());
    }
    apply(invoice.getInvoiceNo(), amounts, paidOn);
  }

  /** Applies part of the basic premium. */
  public void payBasic(OpsInvoice invoice, BigDecimal amount, LocalDate paidOn) {
    apply(invoice.getInvoiceNo(), Map.of(LedgerComponent.BASIC, amount), paidOn);
  }

  private void apply(String invoiceNo, Map<LedgerComponent, BigDecimal> amounts, LocalDate paidOn) {
    String ref = "APP:" + BookingFixtures.token();
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
                            ref,
                            paidOn,
                            amounts,
                            new MovementRequest.DocumentRefs("AR-" + ref, null, null, null),
                            "Test payment"))));
  }

  /** Extracts one invoice into its own batch as the processor (RMTID.004). */
  public ExtractionRun extract(String invoiceNo) {
    return as.run(
        "remit",
        () ->
            extraction.run(
                company(),
                new Scope(ExtractionTrigger.MANUAL_INVOICE, null, null, invoiceNo),
                LocalDate.now()));
  }

  /** The latest batch line of an invoice. */
  public BatchLine lineOf(String invoiceNo) {
    List<BatchLine> found = lines.findByInvoiceNo(invoiceNo);
    return found.isEmpty() ? null : found.get(0);
  }

  /** The batch an invoice was extracted into last. */
  public RemittanceBatch batchOf(String invoiceNo) {
    BatchLine line = lineOf(invoiceNo);
    return line == null ? null : as.run("remit", () -> batches.get(line.getBatch().getId()));
  }

  /** Extracts a paid invoice, submits its batch as remit and approves it as remittl. */
  public RemittanceBatch approvedBatch(OpsInvoice invoice) {
    extract(invoice.getInvoiceNo());
    Long id = batchOf(invoice.getInvoiceNo()).getId();
    as.run("remit", () -> batches.submit(id, "Checked"));
    as.run("remittl", () -> batches.approve(id, "Approved"));
    return as.run("remit", () -> batches.get(id));
  }

  /** The ledger invoice now. */
  public OpsInvoice reload(String invoiceNo) {
    return ledger.require(invoiceNo);
  }
}
