package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import java.time.Instant;

/**
 * A line of the e-policy dispatch report (BRNB.077/078), from the messaging log.
 *
 * @param id message
 * @param reference ARN
 * @param recipients recipients
 * @param subject subject
 * @param status QUEUED, SENT, FAILED or CANCELLED
 * @param attempts delivery attempts
 * @param lastError reason of the last failure
 * @param sentAt delivery time
 * @param simulated delivered without a mail server
 * @param createdAt queued at
 * @param createdBy queued by
 */
public record DispatchLogResponse(
    Long id,
    String reference,
    String recipients,
    String subject,
    String status,
    int attempts,
    String lastError,
    Instant sentAt,
    boolean simulated,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps a message.
   *
   * @param m message
   * @return response
   */
  public static DispatchLogResponse from(OutboundMessage m) {
    return new DispatchLogResponse(
        m.getId(),
        m.getReference(),
        m.getRecipients(),
        m.getSubject(),
        m.getStatus().name(),
        m.getAttempts(),
        m.getLastError(),
        m.getSentAt(),
        m.isSimulated(),
        m.getCreatedAt(),
        m.getCreatedBy());
  }
}
