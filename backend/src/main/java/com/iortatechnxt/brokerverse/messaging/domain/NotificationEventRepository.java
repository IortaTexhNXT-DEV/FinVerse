package com.iortatechnxt.brokerverse.messaging.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification events. */
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, String> {

  /**
   * Every event in display order.
   *
   * @return events
   */
  List<NotificationEvent> findAllByOrderBySortOrderAscCodeAsc();
}
