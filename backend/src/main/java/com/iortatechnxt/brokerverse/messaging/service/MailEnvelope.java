package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import java.util.List;

/**
 * What a {@link MailTransport} delivers.
 *
 * @param from sender address
 * @param to recipients
 * @param cc copy recipients
 * @param subject subject
 * @param body plain-text body
 * @param attachments files as stored
 */
public record MailEnvelope(
    String from,
    List<String> to,
    List<String> cc,
    String subject,
    String body,
    List<MessageFile> attachments) {

  /** Defensive copies. */
  public MailEnvelope {
    to = List.copyOf(to);
    cc = List.copyOf(cc);
    attachments = List.copyOf(attachments);
  }
}
