package com.iortatechnxt.brokerverse.collections.feed.service;

import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService.OutboxQueued;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.CollectionFeedReady;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Announces a feed with pending items once the items are committed (COLLECTIONS_DESIGN 2.2 and 9):
 * consumers (Cashiering, Commission) may pull at once instead of waiting for their schedule.
 */
@Component
public class FeedReadyRelay {

  private final ApplicationEventPublisher events;

  /**
   * Creates the relay.
   *
   * @param events event publisher
   */
  public FeedReadyRelay(ApplicationEventPublisher events) {
    this.events = events;
  }

  /**
   * An item was queued and committed.
   *
   * @param queued the feed
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onQueued(OutboxQueued queued) {
    events.publishEvent(new CollectionFeedReady(queued.companyId(), queued.feedCode()));
  }
}
