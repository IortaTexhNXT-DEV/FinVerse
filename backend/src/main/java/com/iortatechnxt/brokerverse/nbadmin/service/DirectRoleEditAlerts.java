package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.security.service.DirectRoleEditUsed;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Raises the alert {@code UAM_DIRECT_ROLE_EDIT} when a role was created or changed through the
 * emergency path instead of an implemented group-profile request (PQ17; USER_ACCESS_DESIGN section
 * 8). One live alert per role: later edits of the same role join it until it is resolved.
 */
@Component
public class DirectRoleEditAlerts {

  /** Exception code. */
  public static final String CODE = "UAM_DIRECT_ROLE_EDIT";

  private final AlertService alerts;

  /**
   * Creates the listener.
   *
   * @param alerts alert service
   */
  public DirectRoleEditAlerts(AlertService alerts) {
    this.alerts = alerts;
  }

  /**
   * Raises the alert.
   *
   * @param event the direct edit
   */
  @EventListener
  public void on(DirectRoleEditUsed event) {
    alerts.raise(
        CODE,
        new AlertFacts(
            null,
            null,
            "Role",
            event.roleCode(),
            "Role "
                + event.roleCode()
                + " "
                + event.change()
                + " directly by "
                + event.actor()
                + " (emergency path UAM_DIRECT_ROLE_EDIT)",
            null,
            CODE + ":" + event.roleCode()));
  }
}
