package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.security.service.SecurityStoreUnavailable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Raises SECURITY_STORE_UNAVAILABLE when a store of the sign-in security cannot be read (one live
 * alert per store): the operators see that sign-ins are refused or checked against the database.
 */
@Component
public class SecurityStoreAlertListener {

  /** Exception code. */
  public static final String CODE = "SECURITY_STORE_UNAVAILABLE";

  private final AlertService alerts;

  /**
   * Creates the listener.
   *
   * @param alerts alert service
   */
  public SecurityStoreAlertListener(AlertService alerts) {
    this.alerts = alerts;
  }

  /**
   * Raises the alert of a store.
   *
   * @param event the unavailable store
   */
  @EventListener
  public void onUnavailable(SecurityStoreUnavailable event) {
    alerts.raise(
        CODE,
        new AlertFacts(
            null,
            null,
            "Security store",
            event.store(),
            "The sign-in security store '"
                + event.store()
                + "' cannot be read; "
                + event.consequence()
                + ". Check Redis and the database.",
            null,
            CODE + ":" + event.store()));
  }
}
