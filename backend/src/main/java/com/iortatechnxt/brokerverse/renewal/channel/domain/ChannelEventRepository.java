package com.iortatechnxt.brokerverse.renewal.channel.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Delivery history of the channel messages. */
public interface ChannelEventRepository extends JpaRepository<ChannelEvent, Long> {

  /**
   * The history of a message, oldest first.
   *
   * @param messageId message
   * @return events
   */
  List<ChannelEvent> findByMessageIdOrderByIdAsc(Long messageId);
}
