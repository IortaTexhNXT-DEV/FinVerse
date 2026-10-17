package com.iortatechnxt.brokerverse.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An event users can be notified about, with its default channels (RMTID.034). Seeded by the
 * modules that raise it; users override the defaults with a {@link NotificationPreference}.
 */
@Entity
@Table(name = "msg_notification_event")
public class NotificationEvent {

  @Id
  @Column(length = 40)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 30)
  private String module;

  @Column(length = 300)
  private String description;

  @Column(name = "default_in_app", nullable = false)
  private boolean defaultInApp;

  @Column(name = "default_email", nullable = false)
  private boolean defaultEmail;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected NotificationEvent() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getModule() {
    return module;
  }

  public String getDescription() {
    return description;
  }

  public boolean isDefaultInApp() {
    return defaultInApp;
  }

  public boolean isDefaultEmail() {
    return defaultEmail;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}
