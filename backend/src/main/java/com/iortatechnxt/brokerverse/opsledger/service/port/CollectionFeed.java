package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.util.Collection;
import java.util.List;

/**
 * Port to the Collection / Marketing Collection system (BRQID.004, OQ01, OQ45): check pick-up
 * requests (CSHID.009), BIR 2307 tags (CSHID.026, MKTID.013), holds (RMTID.021), special
 * remittances (RMTID.030), DP lists (CMRID.001) inbound; refunds and returned DP accounts
 * (CMRID.009) outbound. Feeds are the {@code COLLECTION_*} rows of {@code ops_flow_in_feed}.
 *
 * <p>Default adapter (manual transport): nothing is pulled - the data arrives through the
 * Operations modules' upload and entry screens - and outbound items are written as a CSV file to
 * the extract repository folder {@code COLLECTION/<feed>} with a logged flow-in run.
 *
 * <p>BRD-4 makes Collections a BrokerVerse module (OQ01 answered): its in-app adapter ({@code
 * collections} {@code InAppCollectionFeed}, transport IN_APP) replaces the default, serves pending
 * items from its outbox, receives outbound items in its inbox and marks acknowledged items TAKEN
 * (COLLECTIONS_DESIGN 2.2).
 */
public interface CollectionFeed {

  /**
   * Transport of the adapter (MANUAL_UPLOAD by default).
   *
   * @return transport name
   */
  String transport();

  /**
   * Items waiting in the Collection system for a feed.
   *
   * @param companyId company
   * @param feedCode inbound feed
   * @return items; always empty with the manual transport
   */
  List<FeedItem> pending(Long companyId, String feedCode);

  /**
   * Sends items to the Collection system.
   *
   * @param companyId company
   * @param feedCode outbound feed
   * @param items items
   * @return reference of the transmission (run number)
   */
  String send(Long companyId, String feedCode, List<FeedItem> items);

  /**
   * Confirms that a consumer took pending items (COLLECTIONS_DESIGN section 9): the consumer calls
   * it after {@code FlowInContext.accept} succeeded for each key, so the in-app adapter of
   * Collections marks its outbox rows TAKEN. The manual transport has no outbox: nothing to do.
   *
   * @param companyId company
   * @param feedCode inbound feed
   * @param keys idempotency keys of the items taken
   */
  default void acknowledge(Long companyId, String feedCode, Collection<String> keys) {
    // Manual transport: items are uploaded, there is nothing to acknowledge.
  }
}
