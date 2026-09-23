package com.iortatechnxt.finverse.accounting.service;

import com.iortatechnxt.finverse.journal.domain.JournalBatch;

/**
 * Entry point for operational modules to account for a business transaction.
 *
 * <p>Publishing is synchronous and joins the caller's transaction (on-line transaction posting):
 * either the business record and its journal are both committed, or neither is.
 */
public interface AccountingEventPublisher {

  /**
   * Accounts for an event.
   *
   * @param event business event
   * @return the posted journal (the existing one when the event was already accounted for)
   */
  JournalBatch publish(BusinessEvent event);
}
