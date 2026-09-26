package com.iortatechnxt.brokerverse.collections;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Collections test data: motor accounts booked through the real booking service, listed in the
 * worklist by the refresh of one invoice (as the collection team lead), and payments posted on the
 * ledger as Cashiering would.
 */
@Component
public class CollectionsFixtures {

  /** Collection handler (MKT_COLLECTION). */
  public static final String HANDLER = "clxhandler";

  /** Collection team lead (CLX_TL). */
  public static final String LEAD = "clxtl";

  /** Section head (CLX_SETUP, CLX_AUDIT_VIEW). */
  public static final String HEAD = "clxuh";

  private final OpsLedgerFixtures ops;
  private final WorklistRefreshService refresh;
  private final InvoiceLedgerService ledgerWriter;
  private final InvoiceLedgerQueryService ledger;
  private final AsUser as;
  private final TransactionTemplate tx;
  private final CollectionFeed feed;

  CollectionsFixtures(
      OpsLedgerFixtures ops,
      WorklistRefreshService refresh,
      InvoiceLedgerService ledgerWriter,
      InvoiceLedgerQueryService ledger,
      AsUser as,
      TransactionTemplate tx,
      CollectionFeed feed) {
    this.ops = ops;
    this.refresh = refresh;
    this.ledgerWriter = ledgerWriter;
    this.ledger = ledger;
    this.as = as;
    this.tx = tx;
    this.feed = feed;
  }

  /**
   * Delivers the items this test queued for Operations, as the consumers would, so later tests of
   * Cashiering and Commission find no stray pending items.
   */
  public void drainOutbox() {
    for (String feedCode :
        List.of(OutboxService.CHECK_PICKUP, OutboxService.CWT2307, OutboxService.DP_LIST)) {
      feed.pending(company(), feedCode);
    }
  }

  /** The seed company. */
  public Long company() {
    return ops.company();
  }

  /** Books a motor account and lists its invoice in the worklist. */
  public CollectionItem listedMotor() {
    OpsInvoice invoice = ops.motorInvoice();
    return as.run(LEAD, () -> refresh.refreshInvoice(invoice.getInvoiceNo()).orElseThrow());
  }

  /** Books a direct payment motor account (no premium receivable) and refreshes it. */
  public String directPaymentInvoice() {
    String no = ops.ledgerOf(ops.bookDirectPayment()).getInvoiceNo();
    as.run(LEAD, () -> refresh.refreshInvoice(no));
    return no;
  }

  /**
   * Pays a share of every premium component of an invoice (Cashiering application).
   *
   * @param invoiceNo invoice
   * @param ref transaction reference
   * @param share share of each balance (1 = in full)
   */
  public void pay(String invoiceNo, String ref, BigDecimal share) {
    as.run(
        "cashier",
        () ->
            tx.execute(
                s ->
                    ledgerWriter.post(
                        new MovementRequest(
                            invoiceNo,
                            MovementType.APPLIED,
                            "CASHIERING",
                            ref,
                            LocalDate.of(2026, 9, 20),
                            shares(ledger.require(invoiceNo), share),
                            new MovementRequest.DocumentRefs("AR-" + ref, null, null, null),
                            "Test payment"))));
  }

  private static Map<LedgerComponent, BigDecimal> shares(OpsInvoice invoice, BigDecimal share) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      if (c.getComponent().isPremiumReceivable() && c.getBalance().signum() > 0) {
        amounts.put(
            c.getComponent(), c.getBalance().multiply(share).setScale(2, RoundingMode.HALF_UP));
      }
    }
    return amounts;
  }
}
