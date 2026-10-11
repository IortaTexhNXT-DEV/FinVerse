package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Sends the e-mails of the EB steps (FR-EB-004): the outbox of the messaging module, with the
 * attached files password-protected and the password in a second e-mail. A file that cannot be
 * protected is refused by the messaging module ({@code DOCUMENT_NOT_PROTECTABLE}).
 */
@Component
public class EbMailer {

  private final MessageService messages;
  private final DocumentService documents;

  /**
   * Creates the mailer.
   *
   * @param messages outbox
   * @param documents stored files
   */
  public EbMailer(MessageService messages, DocumentService documents) {
    this.messages = messages;
    this.documents = documents;
  }

  /**
   * Queues an e-mail.
   *
   * @param companyId company
   * @param purpose outbox purpose
   * @param mail recipients, subject, body and files
   * @param link record the e-mail belongs to
   * @return queued message
   */
  public QueuedEmail send(Long companyId, String purpose, Mail mail, RecordLink link) {
    return messages.queueEmail(
        new OutboundEmail(
            companyId,
            purpose,
            mail.to(),
            mail.cc(),
            mail.subject(),
            mail.body(),
            mail.files(),
            mail.files().isEmpty() ? null : new OutboundEmail.Protection(null, true, null),
            link));
  }

  /**
   * The content of stored files, to attach to an e-mail.
   *
   * @param attachmentIds stored files
   * @return files
   */
  public List<MessageFile> files(List<Long> attachmentIds) {
    List<MessageFile> files = new ArrayList<>();
    for (Long id : attachmentIds) {
      AttachmentFile file = documents.download(id);
      files.add(
          new MessageFile(
              file.metadata().getFileName(), file.metadata().getContentType(), file.content()));
    }
    return files;
  }

  /**
   * An e-mail.
   *
   * @param to recipients
   * @param cc copies
   * @param subject subject
   * @param body body
   * @param files attachments (protected), may be empty
   */
  public record Mail(
      List<String> to, List<String> cc, String subject, String body, List<MessageFile> files) {

    /** Defensive copies. */
    public Mail {
      to = List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
      files = files == null ? List.of() : List.copyOf(files);
    }
  }
}
