package com.iortatechnxt.brokerverse.collections;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.CollectionFeedReady;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Records the {@code CollectionFeedReady} announcements, as a consumer would receive them. */
@Component
public class FeedReadyEvents {

  private final List<CollectionFeedReady> received = new CopyOnWriteArrayList<>();

  @EventListener
  void ready(CollectionFeedReady event) {
    received.add(event);
  }

  /** Feeds announced so far. */
  public List<String> feeds() {
    return received.stream().map(CollectionFeedReady::feedCode).toList();
  }
}
