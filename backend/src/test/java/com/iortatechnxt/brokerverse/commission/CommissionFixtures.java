package com.iortatechnxt.brokerverse.commission;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.service.DpBillingSender;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.commission.service.DpListService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Commission receivables test data: direct payment invoices booked through the real booking
 * service, DP lists in the branch layout, and billings brought to "awaiting insurer".
 */
@Component
public class CommissionFixtures {

  /** The commission handler. */
  public static final String HANDLER = "commrec";

  /** Header of the DP lists. */
  public static final String HEADER = "Invoice No.,Policy No.,Insurer,Premium,Remarks";

  private final OpsLedgerFixtures ledger;
  private final DpListService lists;
  private final DpIntakeService intake;
  private final DpBillingService billings;
  private final DpBillingSender sender;
  private final DpItemRepository items;
  private final AsUser as;

  CommissionFixtures(
      OpsLedgerFixtures ledger,
      DpListService lists,
      DpIntakeService intake,
      DpBillingService billings,
      DpBillingSender sender,
      DpItemRepository items,
      AsUser as) {
    this.ledger = ledger;
    this.lists = lists;
    this.intake = intake;
    this.billings = billings;
    this.sender = sender;
    this.items = items;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return ledger.company();
  }

  /** A newly booked direct payment invoice. */
  public OpsInvoice directPayment() {
    return ledger.ledgerOf(ledger.bookDirectPayment());
  }

  /** A newly booked invoice paid via BDOI. */
  public OpsInvoice regular() {
    return ledger.motorInvoice();
  }

  /** A list row agreeing with an invoice. */
  public static String row(OpsInvoice i) {
    return String.join(
        ",",
        i.getInvoiceNo(),
        i.getPolicyNo() == null ? "" : i.getPolicyNo(),
        i.getInsurerCode(),
        i.getGrossPremium().toPlainString(),
        "Paid to the insurer " + BookingFixtures.token());
  }

  /** The bytes of a list. */
  public static byte[] file(List<String> rows) {
    return (HEADER + "\n" + String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  /** Uploads a Head Office list. */
  public DpList upload(List<String> rows) {
    return as.run(HANDLER, () -> lists.upload(company(), "HO_DP_20260930.csv", file(rows)));
  }

  /** The accounts of a list. */
  public List<DpItem> itemsOf(DpList list) {
    return items.findByListIdOrderByRowNoAscIdAsc(list.getId());
  }

  /** The account of an invoice on a list. */
  public DpItem itemOf(DpList list, OpsInvoice invoice) {
    return itemsOf(list).stream()
        .filter(i -> i.getInvoiceNo().equals(invoice.getInvoiceNo()))
        .findFirst()
        .orElseThrow();
  }

  /** An account read again. */
  public DpItem reload(DpItem item) {
    return items.findById(item.getId()).orElseThrow();
  }

  /** Lists, confirms, bills and sends direct payment invoices; returns the billing. */
  public DpBilling billed(List<OpsInvoice> invoices) {
    DpList list = upload(invoices.stream().map(CommissionFixtures::row).toList());
    List<Long> ids = itemsOf(list).stream().map(DpItem::getId).toList();
    as.run(HANDLER, () -> intake.confirm(ids));
    DpBilling billing = as.run(HANDLER, () -> billings.prepare(company(), ids)).get(0);
    return as.run(
        HANDLER, () -> sender.send(billing.getId(), List.of("billing@insurer-demo.ph"), null));
  }
}
