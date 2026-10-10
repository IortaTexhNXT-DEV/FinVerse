package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Tells the System Administrators of every refused or failed provisioning event, in the system and
 * by e-mail as each prefers, and raises the alert {@value #CODE} (BDOI FRS FRUM.002.02 and
 * FRUM.003.03: refused events alerted for review).
 */
@Component
public class IdentityAlerts {

  /** Notification event and alert code. */
  public static final String CODE = "UAM_IDENTITY_REFUSED";

  /** Screen of the events. */
  public static final String SCREEN = "/admin/identity-sync";

  private final NoticeDelivery notices;
  private final AlertService alerts;

  /**
   * Creates the alerts.
   *
   * @param notices in-app and e-mail notices
   * @param alerts alert service
   */
  public IdentityAlerts(NoticeDelivery notices, AlertService alerts) {
    this.notices = notices;
    this.alerts = alerts;
  }

  /**
   * Tells the System Administrators of a refused or failed event.
   *
   * @param event the event
   */
  public void refused(IdentityEvent event) {
    String text =
        event.getSource().label()
            + " event "
            + words(event)
            + " of Windows ID "
            + event.getWindowsId()
            + " was "
            + event.getStatus().name().toLowerCase(Locale.ROOT)
            + ": "
            + event.getMessage();
    notices.toPermission(
        "USER_MANAGE",
        new Notice(
            "Identity event " + event.getId() + " needs review",
            text,
            SCREEN,
            IdentitySyncService.ENTITY,
            String.valueOf(event.getId())),
        CODE,
        true);
    alerts.raise(
        CODE,
        new AlertFacts(
            null,
            null,
            IdentitySyncService.ENTITY,
            String.valueOf(event.getId()),
            text,
            null,
            CODE + ":" + event.getId()));
  }

  private static String words(IdentityEvent event) {
    String name = event.getEventType().name();
    return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
  }
}
