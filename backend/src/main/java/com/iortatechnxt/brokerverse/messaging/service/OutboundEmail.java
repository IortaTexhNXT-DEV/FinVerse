package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import java.util.List;

/**
 * An e-mail to queue.
 *
 * @param companyId company (may be null)
 * @param purpose purpose code, e.g. {@code QUOTATION}, {@code EPOLICY}, {@code SERVICE_INVOICE}
 * @param to recipients (at least one)
 * @param cc copy recipients
 * @param subject subject
 * @param body plain-text body
 * @param attachments files
 * @param protection password protection of the attachments, null for none
 * @param link business record the e-mail concerns
 */
public record OutboundEmail(
    Long companyId,
    String purpose,
    List<String> to,
    List<String> cc,
    String subject,
    String body,
    List<MessageFile> attachments,
    Protection protection,
    RecordLink link) {

  /** Defensive copies; null lists become empty. */
  public OutboundEmail {
    to = to == null ? List.of() : List.copyOf(to);
    cc = cc == null ? List.of() : List.copyOf(cc);
    attachments = attachments == null ? List.of() : List.copyOf(attachments);
  }

  /**
   * Password protection of attachments (BRNB.013/035).
   *
   * @param password password to use; null to generate one with the {@link DocumentPasswordPolicy}
   * @param separatePasswordMail send the password in a second e-mail to the same recipients
   * @param passwordHint text telling the recipient how the password is built (BRNB.077), may be
   *     null
   */
  public record Protection(String password, boolean separatePasswordMail, String passwordHint) {

    @Override
    public String toString() {
      return "Protection[separatePasswordMail=" + separatePasswordMail + "]";
    }
  }
}
