package com.iortatechnxt.brokerverse.messaging.domain;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** In-app notifications. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /**
   * A user's notifications, newest first.
   *
   * @param recipient user
   * @param pageable page
   * @return notifications
   */
  Page<Notification> findByRecipientIgnoreCaseOrderByIdDesc(String recipient, Pageable pageable);

  /**
   * A user's unread notifications, newest first.
   *
   * @param recipient user
   * @param pageable page
   * @return notifications
   */
  Page<Notification> findByRecipientIgnoreCaseAndReadAtIsNullOrderByIdDesc(
      String recipient, Pageable pageable);

  /**
   * Unread count.
   *
   * @param recipient user
   * @return count
   */
  long countByRecipientIgnoreCaseAndReadAtIsNull(String recipient);

  /**
   * Marks all of a user's notifications read.
   *
   * @param recipient user
   * @param when time
   * @return rows updated
   */
  @Modifying
  @Query(
      "update Notification n set n.readAt = :when"
          + " where lower(n.recipient) = lower(:recipient) and n.readAt is null")
  int markAllRead(@Param("recipient") String recipient, @Param("when") Instant when);
}
