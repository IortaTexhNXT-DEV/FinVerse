package com.iortatechnxt.brokerverse.messaging.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Attachments of outbound messages. */
public interface OutboundAttachmentRepository extends JpaRepository<OutboundAttachment, Long> {

  /**
   * Attachments of a message.
   *
   * @param messageId message
   * @return attachments
   */
  List<OutboundAttachment> findByMessageIdOrderById(Long messageId);
}
