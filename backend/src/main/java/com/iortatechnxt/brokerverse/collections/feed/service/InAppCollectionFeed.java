package com.iortatechnxt.brokerverse.collections.feed.service;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Transport;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The in-app adapter of the {@link CollectionFeed} port (COLLECTIONS_DESIGN 2.2, OQ01 answered):
 * Collections is a BrokerVerse module, so the {@code COLLECTION_*} feeds are served from its outbox
 * and received in its inbox instead of files. It replaces the default manual transport.
 *
 * <ul>
 *   <li>{@link #pending} - delivers the PENDING outbox items of an inbound feed (2307 tags, DP
 *       accounts, check pick-ups) to Cashiering and Commission once and marks them TAKEN.
 *   <li>{@link #send} - an outbound feed from Operations (DP returned, refunds) as one flow-in run
 *       whose records become inbox items, once per idempotency key.
 *   <li>{@link #acknowledge} - marks the outbox items the consumer took TAKEN.
 * </ul>
 */
@Service
public class InAppCollectionFeed implements CollectionFeed {

  private final OutboxService outbox;
  private final InboxService inbox;
  private final FlowInService flowIn;

  /**
   * Creates the adapter.
   *
   * @param outbox outbox
   * @param inbox inbox
   * @param flowIn flow-in runs
   */
  public InAppCollectionFeed(OutboxService outbox, InboxService inbox, FlowInService flowIn) {
    this.outbox = outbox;
    this.inbox = inbox;
    this.flowIn = flowIn;
  }

  @Override
  public String transport() {
    return Transport.IN_APP.name();
  }

  @Override
  public List<FeedItem> pending(Long companyId, String feedCode) {
    return outbox.deliver(companyId, feedCode);
  }

  @Override
  public String send(Long companyId, String feedCode, List<FeedItem> items) {
    FlowInRun run =
        flowIn.run(
            feedCode,
            Trigger.EVENT,
            FileRef.NONE,
            ctx -> {
              for (FeedItem item : items) {
                ctx.accept(
                    item.key(),
                    item.fields().toString(),
                    () -> inbox.receive(companyId, feedCode, item, ctx.runNo()));
              }
            });
    return run.getRunNo();
  }

  @Override
  public void acknowledge(Long companyId, String feedCode, Collection<String> keys) {
    outbox.acknowledge(companyId, feedCode, keys);
  }
}
