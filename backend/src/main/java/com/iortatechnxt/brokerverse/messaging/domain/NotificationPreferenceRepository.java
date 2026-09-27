package com.iortatechnxt.brokerverse.messaging.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification preferences. */
public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, Long> {

  /**
   * A user's preferences.
   *
   * @param username user
   * @return preferences
   */
  List<NotificationPreference> findByUsernameIgnoreCase(String username);

  /**
   * A user's preference for one event.
   *
   * @param username user
   * @param eventCode event
   * @return preference
   */
  Optional<NotificationPreference> findByUsernameIgnoreCaseAndEventCode(
      String username, String eventCode);
}
