package com.iortatechnxt.brokerverse.messaging.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A user's choice of channels for one notification event (RMTID.034). */
@Entity
@Table(name = "msg_notification_preference")
public class NotificationPreference extends BaseEntity {

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "event_code", nullable = false, length = 40, updatable = false)
  private String eventCode;

  @Column(name = "in_app", nullable = false)
  private boolean inApp;

  @Column(nullable = false)
  private boolean email;

  protected NotificationPreference() {}

  /**
   * Creates a preference.
   *
   * @param username user
   * @param eventCode event
   * @param channels in-app and e-mail choice
   */
  public NotificationPreference(String username, String eventCode, Channels channels) {
    this.username = username;
    this.eventCode = eventCode;
    change(channels);
  }

  /**
   * Changes the channels.
   *
   * @param channels in-app and e-mail choice
   */
  public final void change(Channels channels) {
    this.inApp = channels.inApp();
    this.email = channels.email();
  }

  public String getUsername() {
    return username;
  }

  public String getEventCode() {
    return eventCode;
  }

  public boolean isInApp() {
    return inApp;
  }

  public boolean isEmail() {
    return email;
  }

  /**
   * Channels of a notification.
   *
   * @param inApp in-app notification (header bell)
   * @param email e-mail
   */
  public record Channels(boolean inApp, boolean email) {}
}
