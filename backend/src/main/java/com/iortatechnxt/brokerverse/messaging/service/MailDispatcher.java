package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachmentRepository;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessageRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Delivers queued e-mails, one message per transaction: right after the business transaction
 * commits and, for anything left (server down, retries), in the {@code MAIL_DISPATCH} job. When
 * Kafka is enabled ({@code brokerverse.kafka.enabled}) the immediate delivery is done by the
 * consumer of {@code bibs.messaging.notification-requested.v1} instead (asynchronous, see {@code
 * integration.service.NotificationDeliveryConsumer}); the job stays the safety net.
 */
@Service
public class MailDispatcher {

  private static final int DEFAULT_ATTEMPTS = 3;

  private final OutboundMessageRepository messages;
  private final OutboundAttachmentRepository attachments;
  private final MailTransport transport;
  private final SystemParameterService parameters;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final boolean dispatchOnCommit;

  /**
   * Creates the dispatcher.
   *
   * @param messages messages
   * @param attachments attachments
   * @param transport mail transport
   * @param parameters business parameters (sender, attempts)
   * @param txManager transaction manager
   * @param clock clock
   * @param dispatchOnCommit deliver right after the commit ({@code
   *     brokerverse.mail.dispatch-on-commit}); when false only the job delivers
   * @param kafkaDelivers true when the Kafka consumer delivers queued messages ({@code
   *     brokerverse.kafka.enabled}); the delivery after commit is then skipped
   */
  public MailDispatcher(
      OutboundMessageRepository messages,
      OutboundAttachmentRepository attachments,
      MailTransport transport,
      SystemParameterService parameters,
      PlatformTransactionManager txManager,
      Clock clock,
      @Value("${brokerverse.mail.dispatch-on-commit:true}") boolean dispatchOnCommit,
      @Value("${brokerverse.kafka.enabled:false}") boolean kafkaDelivers) {
    this.messages = messages;
    this.attachments = attachments;
    this.transport = transport;
    this.parameters = parameters;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
    this.dispatchOnCommit = dispatchOnCommit && !kafkaDelivers;
  }

  /**
   * Delivers a message queued by a transaction that just committed.
   *
   * @param event queued message
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQueued(MessageQueuedEvent event) {
    if (dispatchOnCommit) {
      dispatch(event.messageId());
    }
  }

  /**
   * Delivers every queued message (oldest first).
   *
   * @param limit maximum number of messages
   * @return number of messages attempted
   */
  public int dispatchPending(int limit) {
    List<Long> ids =
        tx.execute(
            s ->
                messages
                    .findByStatusOrderByIdAsc(MessageStatus.QUEUED, PageRequest.of(0, limit))
                    .stream()
                    .map(OutboundMessage::getId)
                    .toList());
    if (ids == null) {
      return 0;
    }
    ids.forEach(this::dispatch);
    return ids.size();
  }

  /**
   * One delivery attempt of one message in its own transaction.
   *
   * @param messageId message
   */
  public void dispatch(Long messageId) {
    tx.executeWithoutResult(
        s ->
            messages
                .findById(messageId)
                .filter(m -> m.getStatus() == MessageStatus.QUEUED)
                .ifPresent(this::attempt));
  }

  private void attempt(OutboundMessage message) {
    List<MessageFile> files =
        attachments.findByMessageIdOrderById(message.getId()).stream()
            .map(a -> new MessageFile(a.getFileName(), a.getMimeType(), a.getContent()))
            .toList();
    MailEnvelope envelope =
        new MailEnvelope(
            parameters.text("MAIL_FROM_ADDRESS", "no-reply@localhost"),
            split(message.getRecipients()),
            split(message.getCc()),
            message.getSubject(),
            message.getBody(),
            files);
    try {
      boolean simulated = transport.send(envelope);
      message.markSent(clock.instant(), simulated);
    } catch (MailDeliveryException e) {
      message.markAttemptFailed(
          e.getMessage(), parameters.intValue("MAIL_MAX_ATTEMPTS", DEFAULT_ATTEMPTS));
    }
  }

  private static List<String> split(String addresses) {
    if (addresses == null || addresses.isBlank()) {
      return List.of();
    }
    return Arrays.stream(addresses.split(",")).map(String::trim).filter(a -> !a.isEmpty()).toList();
  }
}
