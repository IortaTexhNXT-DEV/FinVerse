package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Sends a service invoice to its recipient (BRNB.100b): the PDF is e-mailed to the insurer's
 * billing address (the insurer party's e-mail) through the outbox, which logs every attempt. The
 * owner of the type (a user, or the holders of a permission) is notified of the outcome with the
 * reason of a failure; delivery results arrive after the commit ({@link
 * ServiceInvoiceDispatchWatcher}).
 */
@Component
public class ServiceInvoiceDispatch {

  /** Entity type of service invoices (attachments, messages, audit). */
  public static final String ENTITY = "ServiceInvoice";

  /** Outbox purpose. */
  public static final String PURPOSE = "SERVICE_INVOICE";

  private static final String PDF = "application/pdf";

  private final MessageService messages;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;

  /**
   * Creates the dispatch.
   *
   * @param messages outbox
   * @param notifications in-app notifications
   * @param events event publisher
   */
  public ServiceInvoiceDispatch(
      MessageService messages,
      NotificationService notifications,
      ApplicationEventPublisher events) {
    this.messages = messages;
    this.notifications = notifications;
    this.events = events;
  }

  /**
   * Queues the e-mail of a service invoice, or records why it is not sent.
   *
   * @param si service invoice (saved, with its document)
   * @param email billing address, null or blank when none
   */
  public void send(ServiceInvoice si, String email) {
    if (email == null || email.isBlank()) {
      String reason = "No billing e-mail address for " + si.getRecipientCode();
      si.notSent(reason);
      notifyOwner(si, "Service invoice " + si.getSiNo() + " not sent", reason);
      return;
    }
    String label = si.getKind() == SiKind.CREDIT ? "Service invoice credit " : "Service invoice ";
    Long messageId =
        messages
            .queueEmail(
                new OutboundEmail(
                    si.getCompanyId(),
                    PURPOSE,
                    List.of(email.strip()),
                    null,
                    label + si.getSiNo(),
                    "Dear "
                        + si.getRecipientName()
                        + ",\n\nPlease find attached our "
                        + label.toLowerCase(Locale.ROOT)
                        + si.getSiNo()
                        + (si.getInvoiceNo() == null ? "" : " for invoice " + si.getInvoiceNo())
                        + ".\n\nBDO Insurance and Reinsurance Brokers, Inc.",
                    List.of(new MessageFile(si.getSiNo() + ".pdf", PDF, si.getDocument())),
                    null,
                    new RecordLink(ENTITY, String.valueOf(si.getId()), si.getSiNo())))
            .messageId();
    si.queued(messageId, email.strip());
    events.publishEvent(new ServiceInvoiceQueued(si.getId(), messageId));
  }

  /**
   * Notifies the owner of a service invoice (user and / or team permission).
   *
   * @param si service invoice
   * @param title title
   * @param body detail
   */
  public void notifyOwner(ServiceInvoice si, String title, String body) {
    Notice notice =
        new Notice(
            title,
            body,
            "/booking/service-invoices/" + si.getId(),
            ENTITY,
            String.valueOf(si.getId()));
    if (si.getOwnerUsername() != null) {
      notifications.notifyUser(si.getOwnerUsername(), notice);
    }
    if (si.getOwnerPermission() != null) {
      notifications.notifyPermission(si.getOwnerPermission(), notice);
    }
  }

  /**
   * A service invoice e-mail was queued (delivered after the commit).
   *
   * @param serviceInvoiceId service invoice
   * @param messageId outbox message
   */
  public record ServiceInvoiceQueued(Long serviceInvoiceId, Long messageId) {}
}
