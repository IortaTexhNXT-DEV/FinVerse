package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessageRepository;
import com.iortatechnxt.brokerverse.messaging.service.MessageQueuedEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes {@code bibs.messaging.notification-requested.v1} for every queued e-mail. The payload
 * carries identifiers and the purpose only: recipients, subject and body stay in the database (the
 * dispatcher reads them), so no personal data or document content travels on the topic.
 */
@Component
public class MessagingEventAdapter {

  private final IntegrationEventPublisher publisher;
  private final OutboundMessageRepository messages;

  /**
   * Creates the adapter.
   *
   * @param publisher integration event publisher
   * @param messages outbound messages (company, purpose, record)
   */
  public MessagingEventAdapter(
      IntegrationEventPublisher publisher, OutboundMessageRepository messages) {
    this.publisher = publisher;
    this.messages = messages;
  }

  /**
   * Stores the request in the outbox of the queuing transaction.
   *
   * @param event queued message
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(MessageQueuedEvent event) {
    Optional<OutboundMessage> message = messages.findById(event.messageId());
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.NOTIFICATION_REQUESTED,
            IntegrationTopics.TYPE_NOTIFICATION_REQUESTED,
            String.valueOf(event.messageId()),
            message.map(OutboundMessage::getCompanyId).orElse(null),
            new NotificationRequestedPayload(
                event.messageId(),
                message.map(OutboundMessage::getPurpose).orElse(null),
                message.map(OutboundMessage::getEntityType).orElse(null),
                message.map(OutboundMessage::getEntityId).orElse(null),
                message.map(OutboundMessage::getReference).orElse(null))));
  }

  /**
   * Payload of {@code messaging.notification.requested}.
   *
   * @param messageId outbound message id (the dispatcher delivers it)
   * @param purpose purpose code
   * @param entityType record the message is about, may be null
   * @param entityId its id, may be null
   * @param reference business reference, may be null
   */
  public record NotificationRequestedPayload(
      long messageId, String purpose, String entityType, String entityId, String reference) {}
}
