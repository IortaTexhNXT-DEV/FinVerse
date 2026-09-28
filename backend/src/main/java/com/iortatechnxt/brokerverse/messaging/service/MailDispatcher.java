package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachment;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachmentRepository;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessageRepository;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
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

  /** Sender on a developer's machine when nothing is configured. */
  static final String LOCAL_SENDER = "no-reply@localhost";

  private final OutboundMessageRepository messages;
  private final OutboundAttachmentRepository attachments;
  private final MailTransport transport;
  private final SystemParameterService parameters;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final boolean dispatchOnCommit;
  private final StoredFileService storedFiles;
  private final String configuredSender;
  private final boolean local;

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
   * @param storedFiles file store (attachments as sent)
   * @param configuredSender sender of the deployment ({@code brokerverse.mail.from-address}); it
   *     takes precedence over the parameter {@code MAIL_FROM_ADDRESS}
   * @param environment kind of environment; only {@code local} falls back to {@value #LOCAL_SENDER}
   *     when no sender is configured
   */
  public MailDispatcher(
      OutboundMessageRepository messages,
      OutboundAttachmentRepository attachments,
      MailTransport transport,
      SystemParameterService parameters,
      PlatformTransactionManager txManager,
      Clock clock,
      @Value("${brokerverse.mail.dispatch-on-commit:true}") boolean dispatchOnCommit,
      @Value("${brokerverse.kafka.enabled:false}") boolean kafkaDelivers,
      StoredFileService storedFiles,
      @Value("${brokerverse.mail.from-address:}") String configuredSender,
      @Value("${brokerverse.environment:local}") String environment) {
    this.storedFiles = storedFiles;
    this.configuredSender = configuredSender == null ? "" : configuredSender.trim();
    this.local = "local".equalsIgnoreCase(environment == null ? "" : environment.trim());
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
            .map(a -> new MessageFile(a.getFileName(), a.getMimeType(), content(a)))
            .toList();
    String sender = sender();
    if (sender.isEmpty()) {
      message.markAttemptFailed(
          "No sender address is configured (BROKERVERSE_MAIL_FROM)",
          parameters.intValue("MAIL_MAX_ATTEMPTS", DEFAULT_ATTEMPTS));
      return;
    }
    MailEnvelope envelope =
        new MailEnvelope(
            sender,
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

  /**
   * The sender: the deployment's address, else the parameter {@code MAIL_FROM_ADDRESS}, else (on a
   * developer's machine only) {@value #LOCAL_SENDER}; empty when none applies.
   */
  private String sender() {
    if (!configuredSender.isEmpty()) {
      return configuredSender;
    }
    String parameter = parameters.text("MAIL_FROM_ADDRESS", "").trim();
    if (!parameter.isEmpty()) {
      return parameter;
    }
    return local ? LOCAL_SENDER : "";
  }

  /**
   * The content of an attachment as sent: read from the file store (SHA-256 re-checked) or, for an
   * attachment queued before ST1 and not yet copied, from its row.
   */
  private byte[] content(OutboundAttachment attachment) {
    return attachment.getStoredFileId() == null
        ? attachment.getContent()
        : storedFiles.read(attachment.getStoredFileId());
  }

  private static List<String> split(String addresses) {
    if (addresses == null || addresses.isBlank()) {
      return List.of();
    }
    return Arrays.stream(addresses.split(",")).map(String::trim).filter(a -> !a.isEmpty()).toList();
  }
}
