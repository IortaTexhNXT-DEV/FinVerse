package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Feed from booking (OPERATIONS_DESIGN 2.2): copies every committed booked invoice, endorsement and
 * cancellation into the Operations ledger. It listens after commit and writes in a new transaction,
 * so booking never waits for or fails because of Operations; a copy that fails is logged as a
 * failed {@code OPS_INVOICE_FEED} record (alert {@code OPS_FLOW_IN_FAILED}) and is recovered by the
 * replay ({@code OPS_INVOICE_FEED_REPLAY}).
 */
@Component
public class InvoiceLedgerFeed {

  /** Feed of booked invoices ({@code ops_flow_in_feed}). */
  public static final String FEED = "OPS_INVOICE_FEED";

  private static final Logger LOG = LoggerFactory.getLogger(InvoiceLedgerFeed.class);

  private final InvoiceLedgerWriter writer;
  private final FlowInService flowIn;
  private final TransactionTemplate tx;

  /**
   * Creates the feed.
   *
   * @param writer ledger writer
   * @param flowIn flow-in log (failures)
   * @param txManager transaction manager
   */
  public InvoiceLedgerFeed(
      InvoiceLedgerWriter writer, FlowInService flowIn, PlatformTransactionManager txManager) {
    this.writer = writer;
    this.flowIn = flowIn;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Copies a committed booked invoice.
   *
   * @param event booked invoice
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(InvoiceBooked event) {
    try {
      tx.executeWithoutResult(s -> writer.record(event, FeedSource.EVENT));
    } catch (BusinessRuleException | ResourceNotFoundException | DataAccessException ex) {
      LOG.warn(
          "Invoice {} not copied to the Operations ledger: {}", event.invoiceNo(), ex.getMessage());
      flowIn.failure(FEED, event.invoiceNo(), ex.getMessage());
    }
  }
}
