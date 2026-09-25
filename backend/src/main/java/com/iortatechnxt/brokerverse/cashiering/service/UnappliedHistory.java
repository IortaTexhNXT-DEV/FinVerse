package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The history of an unapplied item as the collectors see it (BRCLXN.040): its intake, the collector
 * requests and their decisions, every disposition with its execution (applied, refunded,
 * reclassified, transferred, released), withdrawals and reversals, and the closing. Kept after the
 * item is applied or refunded because nothing is deleted.
 */
@Component
@Transactional(readOnly = true)
public class UnappliedHistory {

  private static final String SYSTEM = "SYSTEM";

  private final UnappliedRepository items;
  private final DispositionRepository dispositions;
  private final CollectorRequestRepository requests;
  private final CashReceiptRepository receipts;

  /**
   * Creates the history reader.
   *
   * @param items unapplied items
   * @param dispositions dispositions
   * @param requests collector requests
   * @param receipts receipts
   */
  public UnappliedHistory(
      UnappliedRepository items,
      DispositionRepository dispositions,
      CollectorRequestRepository requests,
      CashReceiptRepository receipts) {
    this.items = items;
    this.dispositions = dispositions;
    this.requests = requests;
    this.receipts = receipts;
  }

  /**
   * The events of an item, oldest first.
   *
   * @param reference item reference
   * @return events; empty when unknown
   */
  public List<UnappliedEvent> of(String reference) {
    Unapplied item = items.findByReference(reference == null ? "" : reference).orElse(null);
    if (item == null) {
      return List.of();
    }
    List<UnappliedEvent> events = new ArrayList<>();
    String receiptNo =
        item.getReceiptId() == null
            ? null
            : receipts.findById(item.getReceiptId()).map(Receipt::getReceiptNo).orElse(null);
    events.add(
        new UnappliedEvent(
            item.getCreatedAt(),
            "RECEIVED",
            "Unapplied " + item.getOrigin() + " from " + text(item.getPayorName()),
            item.getAmount(),
            item.getCreatedBy(),
            receiptNo));
    requests.findByUnappliedIdOrderByIdDesc(item.getId()).forEach(r -> request(events, r));
    dispositions
        .findByUnappliedIdOrderByIdAsc(item.getId())
        .forEach(d -> disposition(events, item, d));
    if ("CLOSED".equals(item.getStage())) {
      events.add(
          new UnappliedEvent(
              item.getUpdatedAt(), "CLOSED", "Item closed", null, by(item.getUpdatedBy()), null));
    }
    events.sort(
        Comparator.comparing(UnappliedEvent::at, Comparator.nullsLast(Comparator.naturalOrder())));
    return events;
  }

  private static void request(List<UnappliedEvent> events, CollectorRequest r) {
    events.add(
        new UnappliedEvent(
            r.getCreatedAt(),
            "REQUESTED",
            "Collector request "
                + r.getAction()
                + (r.getInvoiceNo() == null ? "" : " to invoice " + r.getInvoiceNo()),
            r.getAmount(),
            r.getRequestedBy(),
            r.getRequestNo()));
    if (r.getDecidedAt() != null) {
      events.add(
          new UnappliedEvent(
              r.getDecidedAt(),
              "REQUEST_" + r.getStatus(),
              "Request "
                  + r.getStatus().name().toLowerCase(Locale.ROOT)
                  + (r.getDecisionNote() == null ? "" : ": " + r.getDecisionNote()),
              null,
              by(r.getDecidedBy()),
              r.getRequestNo()));
    }
  }

  private static void disposition(List<UnappliedEvent> events, Unapplied item, Disposition d) {
    events.add(
        new UnappliedEvent(
            d.getCreatedAt(),
            "DISPOSITION",
            "Disposition " + d.getDispositionType() + " assigned",
            d.getAmount(),
            d.getCreatedBy(),
            "DSP:" + d.getId()));
    if (d.getCompletedAt() != null) {
      events.add(
          new UnappliedEvent(
              d.getCompletedAt(),
              executed(d),
              executedText(item, d),
              d.getAmount(),
              by(d.getApprovedBy() == null ? d.getUpdatedBy() : d.getApprovedBy()),
              d.getDisbursementRequestNo() == null
                  ? d.getTargetInvoiceNo()
                  : d.getDisbursementRequestNo()));
    }
    if (d.getStatus() == DispositionStatus.REVERSED
        || d.getStatus() == DispositionStatus.WITHDRAWN) {
      events.add(
          new UnappliedEvent(
              d.getUpdatedAt(),
              d.getStatus().name(),
              "Disposition "
                  + d.getDispositionType()
                  + " "
                  + d.getStatus().name().toLowerCase(Locale.ROOT),
              d.getAmount(),
              by(d.getUpdatedBy()),
              "DSP:" + d.getId()));
    }
  }

  private static String executed(Disposition d) {
    return switch (d.getAction()) {
      case APPLY, DST_APPLY -> "APPLIED";
      case REFUND -> "REFUNDED";
      case RECLASS -> "RECLASSIFIED";
      case TRANSFER -> "TRANSFERRED";
      case MANUAL -> "RELEASED";
    };
  }

  private static String executedText(Unapplied item, Disposition d) {
    return switch (d.getAction()) {
      case APPLY, DST_APPLY -> "Applied to invoice " + d.getTargetInvoiceNo();
      case REFUND -> "Refund of " + item.getCurrency() + " sent to Disbursement";
      case RECLASS -> "Reclassified to client " + d.getTargetClientCode();
      case TRANSFER -> "Transferred to unit " + d.getTargetUnit();
      case MANUAL -> "Released (settled outside the system)";
    };
  }

  private static String by(String user) {
    return user == null ? SYSTEM : user;
  }

  private static String text(String value) {
    return value == null ? "unknown payor" : value;
  }
}
