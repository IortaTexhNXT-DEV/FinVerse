package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raises {@code COST_CENTER_MISSING} (FRBS 3.1.1) in its own transaction, so the alert survives the
 * rollback of the business transaction whose event could not be posted.
 */
@Component
public class CostCenterAlerts {

  private final AlertService alerts;

  /**
   * Creates the component.
   *
   * @param alerts alert service
   */
  public CostCenterAlerts(AlertService alerts) {
    this.alerts = alerts;
  }

  /**
   * Raises the alert of an event (de-duplicated on the event's source reference).
   *
   * @param event event
   * @param message message
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void raiseMissing(BusinessEvent event, String message) {
    alerts.raise(
        CostCenterRuleService.MISSING,
        new AlertFacts(
            event.companyId(),
            event.branchId(),
            "AccountingEvent",
            event.sourceReference(),
            message,
            null,
            CostCenterRuleService.MISSING
                + ":"
                + event.eventType()
                + ":"
                + event.sourceReference()));
  }
}
