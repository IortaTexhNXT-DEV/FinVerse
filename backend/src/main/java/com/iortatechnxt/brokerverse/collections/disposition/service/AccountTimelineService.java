package com.iortatechnxt.brokerverse.collections.disposition.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.disposition.domain.Effort;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition;
import com.iortatechnxt.brokerverse.collections.feed.domain.InboxItem;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem;
import com.iortatechnxt.brokerverse.collections.feed.service.InboxService;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.TimelineEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The timeline of a collection account (BRCLXN.057): ledger movements (payments, reversals,
 * adjustments, write-offs, DP and 2307 reversals) merged with the Collections actions -
 * assignments, dispositions, efforts, hand-offs to Operations and items received from Operations -
 * newest first; kept for audit.
 */
@Service
@Transactional(readOnly = true)
public class AccountTimelineService {

  private final CollectionItems items;
  private final AccountViewService accounts;
  private final PrDispositionService dispositions;
  private final EffortService efforts;
  private final OutboxService outbox;
  private final InboxService inbox;

  /**
   * Creates the service.
   *
   * @param items collection items
   * @param accounts ledger part of the timeline
   * @param dispositions dispositions
   * @param efforts efforts
   * @param outbox hand-offs
   * @param inbox items received
   */
  public AccountTimelineService(
      CollectionItems items,
      AccountViewService accounts,
      PrDispositionService dispositions,
      EffortService efforts,
      OutboxService outbox,
      InboxService inbox) {
    this.items = items;
    this.accounts = accounts;
    this.dispositions = dispositions;
    this.efforts = efforts;
    this.outbox = outbox;
    this.inbox = inbox;
  }

  /**
   * The timeline of an account, newest first.
   *
   * @param invoiceNo invoice
   * @return entries
   */
  public List<TimelineEntry> timeline(String invoiceNo) {
    CollectionItem item = items.require(invoiceNo);
    List<TimelineEntry> out = new ArrayList<>(accounts.ledgerTimeline(item));
    for (PrDisposition d : dispositions.ofItem(item.getId())) {
      out.add(
          new TimelineEntry(
              d.getCreatedAt(),
              "DISPOSITION",
              "Disposition " + d.getDispositionCode(),
              d.getRemarks(),
              d.getCreatedBy(),
              null));
    }
    for (Effort e : efforts.ofItem(item.getId())) {
      out.add(
          new TimelineEntry(
              e.getEffortAt(),
              "EFFORT",
              "Effort " + e.getEffortCode(),
              e.getRemarks(),
              e.getCreatedBy(),
              null));
    }
    for (OutboxItem o : outbox.ofInvoice(invoiceNo)) {
      out.add(
          new TimelineEntry(
              o.getCreatedAt(),
              "HANDOFF",
              "Sent to " + o.getFeedCode() + " (" + o.getStatus() + ")",
              o.getIdempotencyKey(),
              o.getCreatedBy(),
              null));
    }
    for (InboxItem i : inbox.ofInvoice(invoiceNo)) {
      out.add(
          new TimelineEntry(
              i.getReceivedAt(),
              "INBOX",
              "Received " + i.getFeedCode(),
              i.getMessage(),
              i.getCreatedBy(),
              null));
    }
    out.sort(Comparator.comparing(TimelineEntry::at).reversed());
    return out;
  }
}
