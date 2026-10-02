package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductExpired;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReturned;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to the catalog version events (PMADD06, BRPM.006/015/017): when the catalog publishes
 * inside its transaction the follow-up runs after the commit; when it publishes after commit (the
 * contract) it runs at once. {@link ReleaseFollowUp} does the work in its own transaction.
 */
@Component
public class CatalogEventsListener {

  private final ReleaseFollowUp followUp;

  /**
   * Creates the listener.
   *
   * @param followUp release follow-up
   */
  public CatalogEventsListener(ReleaseFollowUp followUp) {
    this.followUp = followUp;
  }

  /**
   * A version was released.
   *
   * @param event event
   */
  @TransactionalEventListener(fallbackExecution = true)
  public void onReleased(ProductVersionReleased event) {
    followUp.released(event);
  }

  /**
   * A version was returned to DRAFT by the validator.
   *
   * @param event event
   */
  @TransactionalEventListener(fallbackExecution = true)
  public void onReturned(ProductVersionReturned event) {
    followUp.returned(event);
  }

  /**
   * A package expired.
   *
   * @param event event
   */
  @TransactionalEventListener(fallbackExecution = true)
  public void onExpired(ProductExpired event) {
    followUp.expired(event);
  }
}
