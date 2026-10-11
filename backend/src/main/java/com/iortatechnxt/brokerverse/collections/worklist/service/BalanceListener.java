package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceFlagChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps a listed item's balance current between the daily refreshes (BRCLXN.015, COLLECTIONS_DESIGN
 * 8): after a movement or a flag change of its invoice is committed, the item is refreshed in a new
 * transaction. A failure is logged only - the next daily refresh repairs the item - so the posting
 * that raised the event is never affected.
 */
@Component
public class BalanceListener {

  private static final Logger LOG = LoggerFactory.getLogger(BalanceListener.class);

  private final WorklistRefreshService refresh;

  /**
   * Creates the listener.
   *
   * @param refresh refresh
   */
  public BalanceListener(WorklistRefreshService refresh) {
    this.refresh = refresh;
  }

  /**
   * A movement was posted (payment applied, reversed, adjusted, written off ...).
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onMovement(InvoiceMovementPosted event) {
    refresh(event.invoiceNo());
  }

  /**
   * A flag changed (e.g. the invoice was cancelled).
   *
   * @param event event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFlag(InvoiceFlagChanged event) {
    refresh(event.invoiceNo());
  }

  private void refresh(String invoiceNo) {
    try {
      refresh.refreshListed(invoiceNo);
    } catch (DataAccessException | IllegalStateException ex) {
      LOG.warn("Collection item of {} not refreshed: {}", invoiceNo, ex.getMessage());
    }
  }
}
