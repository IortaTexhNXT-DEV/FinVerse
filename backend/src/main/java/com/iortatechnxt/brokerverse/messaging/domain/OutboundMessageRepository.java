package com.iortatechnxt.brokerverse.messaging.domain;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Outbound messages. */
public interface OutboundMessageRepository
    extends JpaRepository<OutboundMessage, Long>, JpaSpecificationExecutor<OutboundMessage> {

  /**
   * Messages waiting for delivery, oldest first.
   *
   * @param status status
   * @param pageable limit
   * @return ids
   */
  List<OutboundMessage> findByStatusOrderByIdAsc(MessageStatus status, Pageable pageable);

  /**
   * Messages about a record, newest first.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return messages
   */
  List<OutboundMessage> findByEntityTypeAndEntityIdOrderByIdDesc(
      String entityType, String entityId);
}
