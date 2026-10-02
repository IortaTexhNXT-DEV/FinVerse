package com.iortatechnxt.brokerverse.collections.feed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.feed.domain.InboxItem;
import com.iortatechnxt.brokerverse.collections.feed.domain.InboxItem.Arrival;
import com.iortatechnxt.brokerverse.collections.feed.domain.InboxItemRepository;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem.FeedKey;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inbox of the in-app Collection feeds (COLLECTIONS_DESIGN 2.2): items Operations sends to
 * Collections. A direct payment account returned by the insurer ({@code COLLECTION_DP_RETURNED},
 * CMRID.009) reopens its item with the disposition "DP returned by insurer", the reason and the
 * time, and notifies the handler ({@code CLX_DP_RETURNED}). Refunds ({@code COLLECTION_REFUND},
 * BRCLXN.040) are kept for the collector's unapplied-payment history.
 */
@Service
@Transactional
public class InboxService {

  /** Direct payment accounts returned by the insurer. */
  public static final String DP_RETURNED = "COLLECTION_DP_RETURNED";

  /** Disposition set on a returned direct payment account. */
  public static final String DP_RETURNED_DISPOSITION = "DP_RETURNED";

  private static final List<String> INVOICE_FIELDS =
      List.of("Invoice No.", "invoiceNo", "invoice", "Invoice no");

  private final InboxItemRepository inbox;
  private final CollectionItems items;
  private final InboxDispositions dispositions;
  private final NotificationService notifications;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;
  private final ObjectMapper json;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param inbox inbox rows
   * @param items collection items
   * @param dispositions disposition recorder
   * @param notifications notifications
   * @param changes change recorder
   * @param audit audit trail
   * @param json JSON mapper
   * @param clock clock
   */
  public InboxService(
      InboxItemRepository inbox,
      CollectionItems items,
      InboxDispositions dispositions,
      NotificationService notifications,
      ChangeRecorder changes,
      AuditTrailService audit,
      ObjectMapper json,
      Clock clock) {
    this.inbox = inbox;
    this.items = items;
    this.dispositions = dispositions;
    this.notifications = notifications;
    this.changes = changes;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  /**
   * Receives one item of an outbound feed (one flow-in record).
   *
   * @param companyId company
   * @param feedCode feed
   * @param item the item
   * @param runNo flow-in run
   * @return what was done
   */
  public String receive(Long companyId, String feedCode, FeedItem item, String runNo) {
    String invoiceNo = invoiceOf(item.fields());
    InboxItem received =
        inbox.save(
            new InboxItem(
                companyId,
                new FeedKey(feedCode, item.key()),
                invoiceNo,
                write(item.fields()),
                new Arrival(runNo, clock.instant())));
    String outcome;
    Long itemId = null;
    Optional<CollectionItem> listed = invoiceNo == null ? Optional.empty() : items.find(invoiceNo);
    if (DP_RETURNED.equals(feedCode) && listed.isPresent()) {
      CollectionItem target = items.requireForUpdate(invoiceNo);
      itemId = target.getId();
      outcome = dpReturned(target, item.fields());
    } else if (DP_RETURNED.equals(feedCode)) {
      outcome = "Invoice " + invoiceNo + " is not in the collection worklist";
    } else {
      itemId = listed.map(CollectionItem::getId).orElse(null);
      outcome = "Received";
    }
    received.processed(itemId, outcome, clock.instant());
    audit.record("CollectionInbox", item.key(), AuditAction.CREATE, feedCode + ": " + outcome);
    return outcome;
  }

  private String dpReturned(CollectionItem item, Map<String, String> fields) {
    String reason = fields.getOrDefault("Reason", "");
    String comment = fields.getOrDefault("Comment", "");
    String remarks =
        "Returned by the insurer"
            + (reason.isBlank() ? "" : ": " + reason)
            + (comment.isBlank() ? "" : " - " + comment)
            + " at "
            + fields.getOrDefault("Returned At", clock.instant().toString());
    Object before = item.getStatus();
    if (item.reopen()) {
      changes.record(
          new Target(
              item.getCompanyId(), CollectionItems.ENTITY, item.getInvoiceNo(), item.getId()),
          "status",
          before,
          item.getStatus(),
          null);
    }
    dispositions.record(item, DP_RETURNED_DISPOSITION, remarks);
    if (item.getCurrentHandler() != null) {
      notifications.notifyUser(
          item.getCurrentHandler(),
          new Notice(
              "Direct payment returned: " + item.getInvoiceNo(),
              remarks,
              "/collections/items/" + item.getInvoiceNo(),
              CollectionItems.ENTITY,
              item.getInvoiceNo()),
          "CLX_DP_RETURNED");
    }
    return "Reopened with disposition " + DP_RETURNED_DISPOSITION;
  }

  /**
   * Items of an invoice (account page).
   *
   * @param invoiceNo invoice
   * @return items, newest first
   */
  @Transactional(readOnly = true)
  public List<InboxItem> ofInvoice(String invoiceNo) {
    return inbox.findByInvoiceNoOrderByIdDesc(invoiceNo);
  }

  private static String invoiceOf(Map<String, String> fields) {
    return INVOICE_FIELDS.stream()
        .map(fields::get)
        .filter(v -> v != null && !v.isBlank())
        .map(String::strip)
        .findFirst()
        .orElse(null);
  }

  private String write(Map<String, String> fields) {
    try {
      return json.writeValueAsString(fields);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Fields cannot be written", ex);
    }
  }

  /**
   * Port of the disposition recorder (implemented by the disposition service), so the inbox does
   * not depend on how dispositions are recorded.
   */
  public interface InboxDispositions {

    /**
     * Records a system disposition on an item.
     *
     * @param item item, locked
     * @param code disposition code
     * @param remarks remarks
     */
    void record(CollectionItem item, String code, String remarks);
  }
}
