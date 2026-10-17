package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.CollectionFeedReady;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes the Operations ledger facts: payments applied to invoices ({@code
 * bibs.cashiering.payment-applied.v1}), payment request / disbursement status ({@code
 * bibs.disbursement.status-changed.v1}) and collection feeds ready ({@code
 * bibs.collections.feed-ready.v1}).
 */
@Component
public class OperationsEventAdapter {

  private final IntegrationEventPublisher publisher;

  /**
   * Creates the adapter.
   *
   * @param publisher integration event publisher
   */
  public OperationsEventAdapter(IntegrationEventPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * A payment applied to an invoice, or an application taken off it.
   *
   * @param event invoice ledger movement
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(InvoiceMovementPosted event) {
    if (event.type() != MovementType.APPLIED && event.type() != MovementType.UNAPPLIED) {
      return;
    }
    Map<String, BigDecimal> amounts = new TreeMap<>();
    event.amounts().forEach((k, v) -> amounts.put(k.name(), v));
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.PAYMENT_APPLIED,
            event.type() == MovementType.APPLIED
                ? IntegrationTopics.TYPE_PAYMENT_APPLIED
                : IntegrationTopics.TYPE_PAYMENT_UNAPPLIED,
            event.invoiceNo(),
            event.companyId(),
            new PaymentAppliedPayload(
                event.invoiceNo(),
                event.type().name(),
                event.sourceModule(),
                event.sourceRef(),
                amounts,
                amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))));
  }

  /**
   * A payment request changed status.
   *
   * @param event status change
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(DisbursementStatusChanged event) {
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.DISBURSEMENT_STATUS,
            IntegrationTopics.TYPE_DISBURSEMENT_STATUS,
            event.requestNo(),
            event.companyId(),
            new DisbursementStatusPayload(
                event.requestNo(),
                event.type() == null ? null : event.type().name(),
                event.sourceModule(),
                event.sourceRef(),
                event.status() == null ? null : event.status().name(),
                event.dvNo(),
                event.dvStatus(),
                event.instrumentStatus(),
                event.reason())));
  }

  /**
   * Collections has items ready in a feed.
   *
   * @param event feed ready
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(CollectionFeedReady event) {
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.COLLECTION_FEED_READY,
            IntegrationTopics.TYPE_COLLECTION_FEED_READY,
            event.feedCode(),
            event.companyId(),
            new FeedReadyPayload(event.feedCode())));
  }

  /**
   * Payload of {@code cashiering.payment.applied} / {@code cashiering.payment.unapplied}.
   *
   * @param invoiceNo invoice
   * @param movementType APPLIED or UNAPPLIED
   * @param sourceModule module that applied it (CASHIERING …)
   * @param sourceRef receipt or application reference
   * @param amounts signed amount per ledger component
   * @param total sum of the amounts
   */
  public record PaymentAppliedPayload(
      String invoiceNo,
      String movementType,
      String sourceModule,
      String sourceRef,
      Map<String, BigDecimal> amounts,
      BigDecimal total) {}

  /**
   * Payload of {@code disbursement.request.status-changed}.
   *
   * @param requestNo payment request number
   * @param requestType request type
   * @param sourceModule module that raised the request
   * @param sourceRef its reference
   * @param status new gateway status
   * @param dvNo disbursement voucher, when assigned
   * @param dvStatus DV stage, may be null
   * @param instrumentStatus instrument status, may be null
   * @param reason return or cancellation reason, may be null
   */
  public record DisbursementStatusPayload(
      String requestNo,
      String requestType,
      String sourceModule,
      String sourceRef,
      String status,
      String dvNo,
      String dvStatus,
      String instrumentStatus,
      String reason) {}

  /**
   * Payload of {@code collections.feed.ready}.
   *
   * @param feedCode feed with pending items
   */
  public record FeedReadyPayload(String feedCode) {}
}
