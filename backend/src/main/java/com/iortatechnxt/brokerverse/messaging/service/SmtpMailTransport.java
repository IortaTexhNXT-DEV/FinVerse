package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Delivers e-mails through the SMTP server configured with {@code spring.mail.*}. Active when
 * {@code brokerverse.mail.enabled=true} (see docs/operations/CONFIGURATION.md).
 */
@Component
@ConditionalOnProperty(name = "brokerverse.mail.enabled", havingValue = "true")
public class SmtpMailTransport implements MailTransport {

  private final JavaMailSender sender;

  /**
   * Creates the transport.
   *
   * @param sender Spring mail sender
   */
  public SmtpMailTransport(JavaMailSender sender) {
    this.sender = sender;
  }

  @Override
  public boolean send(MailEnvelope envelope) throws MailDeliveryException {
    try {
      MimeMessage mime = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mime, !envelope.attachments().isEmpty());
      helper.setFrom(envelope.from());
      helper.setTo(envelope.to().toArray(String[]::new));
      if (!envelope.cc().isEmpty()) {
        helper.setCc(envelope.cc().toArray(String[]::new));
      }
      helper.setSubject(envelope.subject());
      helper.setText(envelope.body(), false);
      for (MessageFile file : envelope.attachments()) {
        helper.addAttachment(
            file.fileName(), new ByteArrayResource(file.content()), file.mimeType());
      }
      sender.send(mime);
      return false;
    } catch (MessagingException | MailException e) {
      throw new MailDeliveryException(e.getMessage(), e);
    }
  }
}
