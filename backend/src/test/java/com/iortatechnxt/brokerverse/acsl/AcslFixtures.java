package com.iortatechnxt.brokerverse.acsl;

import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals.OriginalLine;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * ACSL test data: booked invoices of the Operations ledger, their booking journal lines, and
 * insurer SOA files in the standard ACSL template. Users: {@code acsl} (processor), {@code acsltl}
 * (team leader), {@code acslhead} (approver).
 */
@Component
public class AcslFixtures {

  /** Standard template header. */
  public static final String HEADER =
      "Invoice No,Policy No,Assured,Inception Date,Expiry Date,Gross Premium,Balance,Payments\n";

  private final OpsLedgerFixtures ledgerFx;
  private final InvoiceLedgerQueryService ledger;
  private final CorrectionJournals journals;

  AcslFixtures(
      OpsLedgerFixtures ledgerFx, InvoiceLedgerQueryService ledger, CorrectionJournals journals) {
    this.ledgerFx = ledgerFx;
    this.ledger = ledger;
    this.journals = journals;
  }

  /** The seed company. */
  public Long company() {
    return ledgerFx.company();
  }

  /** A booked motor invoice (unpaid). */
  public OpsInvoice invoice() {
    return ledgerFx.motorInvoice();
  }

  /** A booked direct-payment invoice. */
  public OpsInvoice directInvoice() {
    return ledgerFx.ledgerOf(ledgerFx.bookDirectPayment());
  }

  /** The invoice as it is now. */
  public OpsInvoice reload(OpsInvoice invoice) {
    return ledger.require(invoice.getInvoiceNo());
  }

  /** The booking journal line of an invoice on an account. */
  public OriginalLine bookingLine(OpsInvoice invoice, String accountCode) {
    return journals.familyLines(company(), invoice.getInvoiceNo(), null).stream()
        .filter(l -> l.accountCode().equals(accountCode))
        .findFirst()
        .orElseThrow();
  }

  /** One SOA row in the standard template. */
  public static String row(String invoiceNo, String gross, String balance) {
    return invoiceNo
        + ",POL-"
        + BookingFixtures.token()
        + ",Test Assured,2026-01-01,2026-12-31,"
        + gross
        + ","
        + balance
        + ",0\n";
  }

  /** A CSV file of rows. */
  public static byte[] file(String... rows) {
    return (HEADER + String.join("", rows)).getBytes(StandardCharsets.UTF_8);
  }

  /** A unique insurer-period insurer code of an invoice's lead insurer. */
  public static String insurer(OpsInvoice invoice) {
    return invoice.getInsurerCode();
  }
}
