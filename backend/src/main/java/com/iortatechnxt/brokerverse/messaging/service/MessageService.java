package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachment;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachmentRepository;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.Addressing;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessageRepository;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues outbound e-mails and answers "what was sent for this record" (BRNB.008 send log).
 *
 * <p>Business modules call {@link #queueEmail} inside their transaction; the e-mail is delivered by
 * {@link MailDispatcher} after the commit (and retried by the {@code MAIL_DISPATCH} job), so a
 * rolled-back business transaction never sends mail.
 */
@Service
@Transactional
public class MessageService {

  /** Purpose code of the separate password e-mail. */
  public static final String PASSWORD_PURPOSE = "PASSWORD";

  private static final String ENTITY = "OutboundMessage";
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");
  private static final int MAX_ADDRESS_LIST = 1000;

  private final OutboundMessageRepository messages;
  private final OutboundAttachmentRepository attachments;
  private final DocumentProtector protector;
  private final DocumentPasswordPolicy passwords;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param messages messages
   * @param attachments attachments
   * @param protector document protection
   * @param passwords password convention
   * @param events event publisher (dispatch after commit)
   * @param audit audit trail
   */
  public MessageService(
      OutboundMessageRepository messages,
      OutboundAttachmentRepository attachments,
      DocumentProtector protector,
      DocumentPasswordPolicy passwords,
      ApplicationEventPublisher events,
      AuditTrailService audit) {
    this.messages = messages;
    this.attachments = attachments;
    this.protector = protector;
    this.passwords = passwords;
    this.events = events;
    this.audit = audit;
  }

  /**
   * Queues an e-mail; with protection, attachments are encrypted and the password optionally
   * follows in a separate e-mail to the same recipients (BRNB.035).
   *
   * @param email e-mail
   * @return queued message ids
   */
  public QueuedEmail queueEmail(OutboundEmail email) {
    String to = addressList(email.to(), true);
    String cc = addressList(email.cc(), false);
    requireText(email.subject(), "subject");
    requireText(email.body(), "body");
    Protection protection = email.protection();
    String password = null;
    if (protection != null) {
      password = protection.password() == null ? passwords.newPassword() : protection.password();
    }
    OutboundMessage message =
        messages.save(
            new OutboundMessage(
                email.companyId(),
                email.purpose(),
                new Addressing(to, cc, email.subject(), email.body()),
                email.link()));
    for (MessageFile file : email.attachments()) {
      MessageFile sent = password == null ? file : protector.protect(file, password);
      attachments.save(
          new OutboundAttachment(message.getId(), sent, sha256(sent.content()), password != null));
    }
    audit.record(
        ENTITY,
        message.getId(),
        AuditAction.CREATE,
        "Queued e-mail '"
            + email.subject()
            + "' to "
            + to
            + " with "
            + email.attachments().size()
            + (password == null ? " attachment(s)" : " password-protected attachment(s)"));
    events.publishEvent(new MessageQueuedEvent(message.getId()));
    Long passwordMessageId = null;
    if (protection != null && protection.separatePasswordMail()) {
      passwordMessageId = queuePasswordMail(email, to, message, password).getId();
    }
    return new QueuedEmail(message.getId(), passwordMessageId);
  }

  private OutboundMessage queuePasswordMail(
      OutboundEmail email, String to, OutboundMessage documentMail, String password) {
    String hint = email.protection().passwordHint();
    String body =
        "The documents sent to you in the e-mail \""
            + email.subject()
            + "\" are password protected.\n\nPassword: "
            + password
            + (hint == null || hint.isBlank() ? "" : "\n\n" + hint)
            + "\n\nThis e-mail was sent separately for your security. Please do not forward it.";
    OutboundMessage mail =
        new OutboundMessage(
            email.companyId(),
            PASSWORD_PURPOSE,
            new Addressing(to, null, "Password for: " + email.subject(), body),
            email.link());
    mail.carryPasswordFor(documentMail.getId());
    OutboundMessage saved = messages.save(mail);
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "Queued password e-mail for message " + documentMail.getId() + " to " + to);
    events.publishEvent(new MessageQueuedEvent(saved.getId()));
    return saved;
  }

  /**
   * Messages sent (or queued) for a record, newest first.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return messages
   */
  @Transactional(readOnly = true)
  public List<OutboundMessage> forRecord(String entityType, String entityId) {
    return messages.findByEntityTypeAndEntityIdOrderByIdDesc(entityType, entityId);
  }

  /**
   * Searches the outbox.
   *
   * @param criteria filters
   * @param pageable page
   * @return messages, newest first
   */
  @Transactional(readOnly = true)
  public Page<OutboundMessage> search(MessageSearch criteria, Pageable pageable) {
    return messages.findAll(criteria.toSpecification(), pageable);
  }

  /**
   * One message.
   *
   * @param id id
   * @return message
   */
  @Transactional(readOnly = true)
  public OutboundMessage get(Long id) {
    return messages.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Attachments of a message (metadata and content).
   *
   * @param messageId message
   * @return attachments
   */
  @Transactional(readOnly = true)
  public List<OutboundAttachment> attachments(Long messageId) {
    return attachments.findByMessageIdOrderById(messageId);
  }

  /**
   * Queues a failed message for another delivery.
   *
   * @param id message
   * @return the message
   */
  public OutboundMessage retry(Long id) {
    OutboundMessage message = get(id);
    message.requeue();
    audit.record(ENTITY, id, AuditAction.UPDATE, "Queued again for delivery");
    events.publishEvent(new MessageQueuedEvent(id));
    return message;
  }

  private static String addressList(List<String> addresses, boolean required) {
    List<String> clean = cleanAddresses(addresses);
    if (clean.isEmpty()) {
      if (required) {
        throw new BusinessRuleException("EMAIL_RECIPIENT_REQUIRED", "Enter at least one recipient");
      }
      return null;
    }
    String joined = String.join(", ", clean);
    if (joined.length() > MAX_ADDRESS_LIST) {
      throw new BusinessRuleException("EMAIL_TOO_MANY_RECIPIENTS", "Too many recipients");
    }
    return joined;
  }

  private static List<String> cleanAddresses(List<String> addresses) {
    List<String> clean = new ArrayList<>();
    for (String a : addresses) {
      String trimmed = a == null ? "" : a.trim();
      if (!trimmed.isEmpty()) {
        if (!EMAIL.matcher(trimmed).matches()) {
          throw new BusinessRuleException("EMAIL_ADDRESS_INVALID", "Invalid e-mail address: " + a);
        }
        clean.add(trimmed);
      }
    }
    return clean;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("EMAIL_CONTENT_REQUIRED", "Enter the " + field);
    }
  }

  static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * UTF-8 bytes of a text (helper for plain-text attachments).
   *
   * @param text text
   * @return bytes
   */
  public static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
