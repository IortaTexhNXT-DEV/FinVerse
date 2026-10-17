package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.service.DocumentProtector;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelEvent;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelEventRepository;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateway.Reply;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbound messages of the delivery channels (FRRN.017.02, FRRN.022.02, FRRN.023.02, FRRN.032.02,
 * FRRN.033.04, FRRN.029.04): a document is queued Pending Transmission, protected with the password
 * of its client, handed to CCM (or MFT for the insurers) with its recipients, and Submitted to CCM
 * with the transaction reference returned; the status job reads the delivery status (Sent,
 * Delivered, Failed); a failed message is resent as a new transmission of the same document, or
 * cancelled. Every status is kept in the delivery history and the audit trail.
 */
@Service
@Transactional
public class ChannelService {

  /** Parameter: CCM or EMAIL for the client documents. */
  public static final String DELIVERY_CHANNEL = "RNW_DELIVERY_CHANNEL";

  private static final String PDF = "application/pdf";
  private static final int MAX_ATTEMPTS = 3;
  private static final Map<String, String> ERRORS =
      Map.of(
          ChannelGateway.UNAVAILABLE,
              "Unable to send the file. CCM service is currently unavailable.",
          ChannelGateway.INVALID_RECIPIENT,
              "Unable to send the file. One or more recipient email addresses are invalid.",
          ChannelGateway.REJECTED,
              "File transmission failed. Please review the error details and try again.");

  private final ChannelMessageRepository messages;
  private final ChannelEventRepository events;
  private final ChannelGateways gateways;
  private final AttachmentService attachments;
  private final DocumentProtector protector;
  private final PasswordConvention passwords;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param messages messages
   * @param events delivery history
   * @param gateways channel connections
   * @param attachments stored documents
   * @param protector file protection
   * @param passwords password convention
   * @param numbers message numbers
   * @param parameters settings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ChannelService(
      ChannelMessageRepository messages,
      ChannelEventRepository events,
      ChannelGateways gateways,
      AttachmentService attachments,
      DocumentProtector protector,
      PasswordConvention passwords,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.messages = messages;
    this.events = events;
    this.gateways = gateways;
    this.attachments = attachments;
    this.protector = protector;
    this.passwords = passwords;
    this.numbers = numbers;
    this.parameters = parameters;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Whether the client documents go through CCM.
   *
   * @return true for CCM, false for the protected e-mail
   */
  @Transactional(readOnly = true)
  public boolean ccm() {
    return ChannelGateways.CCM.equals(
        parameters.text(DELIVERY_CHANNEL, ChannelGateways.CCM).strip());
  }

  /**
   * Queues a document and transmits it at once.
   *
   * @param companyId company
   * @param outbound channel, document, recipients and the client of the password
   * @return the message with its status and the outcome message
   */
  public Sent send(Long companyId, Outbound outbound) {
    String no = numbers.next(outbound.channel() + "-" + BusinessClock.today(clock).getYear());
    ChannelMessage m =
        messages.save(
            new ChannelMessage(
                companyId,
                no,
                new ChannelMessage.Route(outbound.channel(), "OUTBOUND"),
                outbound.document(),
                outbound.address()));
    event(m, ChannelStatus.PENDING_TRANSMISSION, outbound.document().fileName());
    return new Sent(m, transmit(m));
  }

  /**
   * Transmits a pending message; a refusal keeps it pending for the next attempt until the attempts
   * run out, an invalid recipient fails it at once.
   *
   * @param m message
   * @return null when accepted, else BDOI's error message
   */
  public String transmit(ChannelMessage m) {
    Optional<PasswordConvention.Password> password =
        m.getClientId() == null ? Optional.empty() : passwords.of(m.getClientId());
    byte[] content = content(m, password);
    ChannelGateway gateway = gateways.of(m.getChannel());
    Reply reply =
        gateway.transmit(
            new ChannelGateway.Transmission(
                m.getMessageNo(),
                split(m.getRecipientsTo()),
                split(m.getRecipientsCc()),
                m.getSubject(),
                m.getBody(),
                m.getFileName(),
                content,
                password.map(PasswordConvention.Password::value).orElse(null)));
    m.attempted(reply.accepted(), reply.reference(), reply.error(), clock.instant());
    if (reply.accepted()) {
      event(m, ChannelStatus.SUBMITTED, "Transaction reference " + reply.reference());
      audit(m, "submitted to " + m.getChannel() + " (" + reply.reference() + ")");
      return null;
    }
    boolean last =
        ChannelGateway.INVALID_RECIPIENT.equals(reply.errorCode())
            || m.getAttempts() >= parameters.intValue("RNW_CHANNEL_MAX_ATTEMPTS", MAX_ATTEMPTS);
    if (last) {
      m.moveTo(ChannelStatus.FAILED, reply.error(), clock.instant());
      event(m, ChannelStatus.FAILED, reply.error());
      audit(m, "failed: " + reply.error());
    }
    return ERRORS.getOrDefault(reply.errorCode(), ERRORS.get(ChannelGateway.REJECTED));
  }

  private byte[] content(ChannelMessage m, Optional<PasswordConvention.Password> password) {
    if (m.getAttachmentId() == null) {
      return new byte[0];
    }
    byte[] original = attachments.download(m.getAttachmentId()).content();
    if (m.getFileName() == null || !m.getFileName().endsWith(".pdf")) {
      return original;
    }
    return password
        .map(p -> protector.protect(new MessageFile(m.getFileName(), PDF, original), p.value()))
        .map(MessageFile::content)
        .orElse(original);
  }

  /**
   * Reads the delivery status of the messages on their way.
   *
   * @return messages that failed
   */
  public int refresh() {
    int failed = 0;
    for (ChannelMessage m :
        messages.findByStatusIn(EnumSet.of(ChannelStatus.SUBMITTED, ChannelStatus.SENT))) {
      var report = gateways.of(m.getChannel()).status(m);
      if (report.isPresent()) {
        m.moveTo(report.get().status(), failedDetail(report.get()), clock.instant());
        event(m, report.get().status(), report.get().detail());
        if (report.get().status() == ChannelStatus.FAILED) {
          failed++;
          audit(m, "failed: " + report.get().detail());
        }
      }
    }
    return failed;
  }

  /**
   * Transmits again the messages pending after a refused attempt (the status job).
   *
   * @return messages still pending
   */
  public int retry() {
    int pending = 0;
    for (ChannelMessage m :
        messages.findByStatusIn(EnumSet.of(ChannelStatus.PENDING_TRANSMISSION))) {
      if (m.getAttempts() > 0 && transmit(m) != null && m.getStatus() != ChannelStatus.FAILED) {
        pending++;
      }
    }
    return pending;
  }

  /**
   * Resends a message: a new transmission of the same document (FRRN.022.01, FRRN.023.02).
   *
   * @param companyId company
   * @param messageNo message
   * @return the new message and the outcome message
   */
  public Sent resend(Long companyId, String messageNo) {
    ChannelMessage old = get(companyId, messageNo);
    audit(old, "resent");
    return send(
        companyId,
        new Outbound(
            old.getChannel(),
            new ChannelMessage.Document(
                old.getDocKind(),
                old.getDocRef(),
                old.getCandidateId(),
                old.getRenewalRef(),
                old.getFileName(),
                old.getAttachmentId()),
            new ChannelMessage.Address(
                old.getRecipientsTo(),
                old.getRecipientsCc(),
                old.getSubject(),
                old.getBody(),
                old.getClientId())));
  }

  /**
   * Cancels a message not transmitted.
   *
   * @param companyId company
   * @param messageNo message
   * @return the message
   */
  public ChannelMessage cancel(Long companyId, String messageNo) {
    ChannelMessage m = get(companyId, messageNo);
    m.cancel();
    event(m, ChannelStatus.CANCELLED, "Cancelled by " + currentUser.username());
    audit(m, "cancelled");
    return m;
  }

  /**
   * A message.
   *
   * @param companyId company
   * @param messageNo number
   * @return message
   */
  @Transactional(readOnly = true)
  public ChannelMessage get(Long companyId, String messageNo) {
    return messages
        .findByCompanyIdAndMessageNo(companyId, messageNo)
        .orElseThrow(() -> new ResourceNotFoundException("Channel message", messageNo));
  }

  /**
   * The delivery history of a message.
   *
   * @param companyId company
   * @param messageNo message
   * @return events, oldest first
   */
  @Transactional(readOnly = true)
  public List<ChannelEvent> history(Long companyId, String messageNo) {
    return events.findByMessageIdOrderByIdAsc(get(companyId, messageNo).getId());
  }

  /**
   * Checks the connection of a channel.
   *
   * @param channel CCM or MFT
   * @return the answer and whether it is the live interface
   */
  @Transactional(readOnly = true)
  public Check check(String channel) {
    ChannelGateway g = gateways.of(channel);
    Reply r = g.check();
    return new Check(channel, g.live(), r.accepted(), r.accepted() ? r.reference() : r.error());
  }

  private void event(ChannelMessage m, ChannelStatus status, String detail) {
    events.save(
        new ChannelEvent(
            m.getId(),
            status,
            detail,
            clock.instant(),
            currentUser.optionalUsername().orElse("SYSTEM")));
  }

  private void audit(ChannelMessage m, String what) {
    audit.record(
        m.getRenewalRef() == null ? "ChannelMessage" : RenewalCodes.ENTITY,
        m.getRenewalRef() == null ? m.getMessageNo() : m.getRenewalRef(),
        AuditAction.UPDATE,
        m.getDocKind() + " " + m.getMessageNo() + " " + what);
  }

  private static String failedDetail(ChannelGateway.Report r) {
    return r.status() == ChannelStatus.FAILED ? r.detail() : null;
  }

  private static List<String> split(String recipients) {
    if (recipients == null || recipients.isBlank()) {
      return List.of();
    }
    return Arrays.stream(recipients.split("[,;]"))
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  /**
   * A document to send.
   *
   * @param channel CCM or MFT
   * @param document what the message carries
   * @param address recipients, subject, text and the client of the password
   */
  public record Outbound(
      String channel, ChannelMessage.Document document, ChannelMessage.Address address) {}

  /**
   * A message queued.
   *
   * @param message the message
   * @param error BDOI's error message, null when submitted
   */
  public record Sent(ChannelMessage message, String error) {}

  /**
   * The check of a connection.
   *
   * @param channel channel
   * @param live live interface or simulator
   * @param reachable whether it answered
   * @param detail answer
   */
  public record Check(String channel, boolean live, boolean reachable, String detail) {}
}
