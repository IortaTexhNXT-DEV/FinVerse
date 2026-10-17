package com.iortatechnxt.brokerverse.messaging.api.dto;

import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundAttachment;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import java.time.Instant;
import java.util.List;

/**
 * An outbound e-mail and its delivery outcome (send log).
 *
 * @param id id
 * @param purpose purpose code
 * @param recipients recipients
 * @param cc copy recipients
 * @param subject subject
 * @param body body; hidden for password e-mails
 * @param status status
 * @param attempts delivery attempts
 * @param lastError reason of the last failure
 * @param sentAt delivery time
 * @param simulated delivered without a mail server
 * @param entityType related record type
 * @param entityId related record id
 * @param reference business reference
 * @param passwordForId message whose password this e-mail carries
 * @param createdAt queued at
 * @param createdBy queued by
 * @param attachments attachments (metadata)
 */
public record MessageResponse(
    Long id,
    String purpose,
    String recipients,
    String cc,
    String subject,
    String body,
    MessageStatus status,
    int attempts,
    String lastError,
    Instant sentAt,
    boolean simulated,
    String entityType,
    String entityId,
    String reference,
    Long passwordForId,
    Instant createdAt,
    String createdBy,
    List<AttachmentInfo> attachments) {

  private static final String HIDDEN = "(password e-mail - content hidden)";

  /**
   * Maps a message; the body of a password e-mail is never returned.
   *
   * @param m message
   * @param files attachments
   * @return response
   */
  public static MessageResponse from(OutboundMessage m, List<OutboundAttachment> files) {
    return new MessageResponse(
        m.getId(),
        m.getPurpose(),
        m.getRecipients(),
        m.getCc(),
        m.getSubject(),
        m.getPasswordForId() == null ? m.getBody() : HIDDEN,
        m.getStatus(),
        m.getAttempts(),
        m.getLastError(),
        m.getSentAt(),
        m.isSimulated(),
        m.getEntityType(),
        m.getEntityId(),
        m.getReference(),
        m.getPasswordForId(),
        m.getCreatedAt(),
        m.getCreatedBy(),
        files.stream().map(AttachmentInfo::from).toList());
  }

  /**
   * Attachment metadata.
   *
   * @param id id
   * @param fileName file name
   * @param sizeBytes size
   * @param sha256 checksum of the file sent
   * @param passwordProtected encrypted
   */
  public record AttachmentInfo(
      Long id, String fileName, long sizeBytes, String sha256, boolean passwordProtected) {

    /**
     * Maps an attachment.
     *
     * @param a attachment
     * @return info
     */
    public static AttachmentInfo from(OutboundAttachment a) {
      return new AttachmentInfo(
          a.getId(), a.getFileName(), a.getSizeBytes(), a.getSha256(), a.isPasswordProtected());
    }
  }
}
