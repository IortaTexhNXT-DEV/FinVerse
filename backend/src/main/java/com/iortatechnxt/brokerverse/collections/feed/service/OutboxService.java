package com.iortatechnxt.brokerverse.collections.feed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OutboxStatus;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem.FeedKey;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItemRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The outbox of the in-app Collection feeds (COLLECTIONS_DESIGN 2.2): dispositions with an
 * Operations action queue their item here (BIR 2307 tag, DP account, check pick-up), consumers pull
 * the pending items through {@code CollectionFeed.pending} and acknowledge them, and an item whose
 * disposition is superseded before it is taken is withdrawn. Queuing announces the feed after
 * commit ({@code CollectionFeedReady}).
 */
@Service
@Transactional
public class OutboxService {

  /** BIR 2307 tags for Cashiering (CSHID.026, MKTID.013). */
  public static final String CWT2307 = "COLLECTION_CWT2307";

  /** Direct payment accounts for Commission (CMRID.001, MKTID.012). */
  public static final String DP_LIST = "COLLECTION_DP_LIST";

  /** Check pick-up requests for Cashiering (CSHID.009). */
  public static final String CHECK_PICKUP = "COLLECTION_CHECK_PICKUP";

  private static final String ENTITY = "CollectionOutbox";
  private static final TypeReference<Map<String, String>> FIELDS = new TypeReference<>() {};

  private final OutboxItemRepository outbox;
  private final ObjectMapper json;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param outbox outbox rows
   * @param json JSON mapper
   * @param events event publisher
   * @param audit audit trail
   * @param clock clock
   */
  public OutboxService(
      OutboxItemRepository outbox,
      ObjectMapper json,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      Clock clock) {
    this.outbox = outbox;
    this.json = json;
    this.events = events;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Queues an item for a consumer.
   *
   * @param companyId company
   * @param key feed and idempotency key
   * @param invoiceNo invoice
   * @param fields fields in the consumer's layout
   * @param dispositionId disposition that created it
   * @return the outbox row
   */
  public OutboxItem queue(
      Long companyId,
      FeedKey key,
      String invoiceNo,
      Map<String, String> fields,
      Long dispositionId) {
    OutboxItem saved =
        outbox.save(new OutboxItem(companyId, key, invoiceNo, write(fields), dispositionId));
    audit.record(ENTITY, key.key(), AuditAction.CREATE, "Queued for " + key.feedCode());
    events.publishEvent(new OutboxQueued(companyId, key.feedCode()));
    return saved;
  }

  /**
   * Withdraws a pending item.
   *
   * @param id outbox row
   * @return true when it was pending
   */
  public boolean cancel(Long id) {
    return outbox
        .findById(id)
        .map(
            o -> {
              boolean cancelled = o.cancel();
              if (cancelled) {
                audit.record(ENTITY, o.getIdempotencyKey(), AuditAction.UPDATE, "Withdrawn");
              }
              return cancelled;
            })
        .orElse(false);
  }

  /**
   * The pending items of a feed, oldest first.
   *
   * @param companyId company
   * @param feedCode feed
   * @return items
   */
  @Transactional(readOnly = true)
  public List<FeedItem> pending(Long companyId, String feedCode) {
    return outbox
        .findByCompanyIdAndFeedCodeAndStatusOrderByIdAsc(companyId, feedCode, OutboxStatus.PENDING)
        .stream()
        .map(o -> new FeedItem(o.getIdempotencyKey(), read(o.getFields())))
        .toList();
  }

  /**
   * Delivers the pending items of a feed to its consumer, oldest first, and marks them TAKEN: the
   * in-app consumers (Cashiering pick-up import, Commission DP pull) take every item they are given
   * in the same call and record each one in their own flow-in run, so an item is delivered once
   * even while a consumer does not acknowledge.
   *
   * @param companyId company
   * @param feedCode feed
   * @return items delivered
   */
  public List<FeedItem> deliver(Long companyId, String feedCode) {
    List<FeedItem> out = new ArrayList<>();
    for (OutboxItem o :
        outbox.findByCompanyIdAndFeedCodeAndStatusOrderByIdAsc(
            companyId, feedCode, OutboxStatus.PENDING)) {
      o.taken(clock.instant());
      out.add(new FeedItem(o.getIdempotencyKey(), read(o.getFields())));
    }
    if (!out.isEmpty()) {
      audit.record(ENTITY, feedCode, AuditAction.UPDATE, out.size() + " item(s) delivered");
    }
    return out;
  }

  /**
   * Marks items taken by their consumer.
   *
   * @param companyId company
   * @param feedCode feed
   * @param keys idempotency keys
   * @return items marked
   */
  public int acknowledge(Long companyId, String feedCode, Collection<String> keys) {
    int taken = 0;
    if (keys.isEmpty()) {
      return taken;
    }
    for (OutboxItem o :
        outbox.findByCompanyIdAndFeedCodeAndIdempotencyKeyIn(companyId, feedCode, keys)) {
      if (o.taken(clock.instant())) {
        audit.record(ENTITY, o.getIdempotencyKey(), AuditAction.UPDATE, "Taken by the consumer");
        taken++;
      }
    }
    return taken;
  }

  /**
   * The items of an invoice (account page).
   *
   * @param invoiceNo invoice
   * @return items, newest first
   */
  @Transactional(readOnly = true)
  public List<OutboxItem> ofInvoice(String invoiceNo) {
    return outbox.findByInvoiceNoOrderByIdDesc(invoiceNo);
  }

  /**
   * The fields of a row.
   *
   * @param fields JSON
   * @return fields by name, in order
   */
  public Map<String, String> read(String fields) {
    try {
      return json.readValue(fields, FIELDS);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unreadable outbox fields", ex);
    }
  }

  private String write(Map<String, String> fields) {
    try {
      return json.writeValueAsString(fields);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Fields cannot be written", ex);
    }
  }

  /**
   * An item was queued (announced as {@code CollectionFeedReady} after commit).
   *
   * @param companyId company
   * @param feedCode feed
   */
  public record OutboxQueued(Long companyId, String feedCode) {}
}
