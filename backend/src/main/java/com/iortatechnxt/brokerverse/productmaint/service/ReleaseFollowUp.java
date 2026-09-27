package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductExpired;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReturned;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.service.ProductMasterFeed.ProductMasterChange;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the request process does when the catalog decides on a version (PMADD06, BRPM.015/016/022):
 * a released version moves its source request to RELEASED (system action {@code version_released}),
 * drafts the advisory, notifies Marketing and feeds the product master port; a returned version
 * sends the request back to MBS ({@code version_returned}); an expired package notifies TSU and
 * MBS. Each runs in its own transaction because the catalog publishes its events after commit.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ReleaseFollowUp {

  private static final String MARKETING_PERMISSION = "QUOTE_MAINTAIN";

  private final PackageRequestRepository requests;
  private final WorkflowService workflow;
  private final AdvisoryService advisories;
  private final NotificationService notifications;
  private final ProductMasterFeed feed;
  private final Clock clock;

  /**
   * Creates the follow-up.
   *
   * @param requests package requests
   * @param workflow workflow engine
   * @param advisories advisories
   * @param notifications in-app notifications
   * @param feed product master synchronisation port
   * @param clock clock
   */
  public ReleaseFollowUp(
      PackageRequestRepository requests,
      WorkflowService workflow,
      AdvisoryService advisories,
      NotificationService notifications,
      ProductMasterFeed feed,
      Clock clock) {
    this.requests = requests;
    this.workflow = workflow;
    this.advisories = advisories;
    this.notifications = notifications;
    this.feed = feed;
    this.clock = clock;
  }

  /**
   * A version was released: the source request is RELEASED and its advisory drafted.
   *
   * @param event catalog event
   */
  public void released(ProductVersionReleased event) {
    feed.publish(
        new ProductMasterChange(
            event.productCode(),
            event.versionNo(),
            "RELEASED",
            event.effectiveFrom(),
            event.sourceRequestNo()));
    Optional<PackageRequest> source = waiting(event.sourceRequestNo());
    if (source.isEmpty()) {
      return;
    }
    PackageRequest p = source.get();
    workflow.systemTransition(
        PackageRequests.ENTITY,
        String.valueOf(p.getId()),
        "version_released",
        TransitionNote.comment(
            "Version " + event.versionNo() + " validated by " + event.validatedBy()));
    p.markReleased(event.versionNo(), clock.instant());
    advisories.draftFor(p, advisoryType(p.getRequestType()), event.versionNo());
    notifications.notifyPermission(
        MARKETING_PERMISSION,
        new Notice(
            "Package available: " + event.productCode(),
            p.getTitle()
                + " (version "
                + event.versionNo()
                + ") sells from "
                + event.effectiveFrom()
                + ".",
            PackageRequests.link(p),
            PackageRequests.ENTITY,
            String.valueOf(p.getId())));
  }

  /**
   * A version was returned by the validator: the source request goes back to MBS.
   *
   * @param event catalog event
   */
  public void returned(ProductVersionReturned event) {
    waiting(event.sourceRequestNo())
        .ifPresent(
            p ->
                workflow.systemTransition(
                    PackageRequests.ENTITY,
                    String.valueOf(p.getId()),
                    "version_returned",
                    TransitionNote.comment(
                        "Version " + event.versionNo() + " returned: " + event.reason())));
  }

  /**
   * A package expired: TSU and MBS are notified and the product master port fed.
   *
   * @param event catalog event
   */
  public void expired(ProductExpired event) {
    feed.publish(
        new ProductMasterChange(
            event.productCode(), event.versionNo(), "EXPIRED", event.packageEndDate(), null));
    Notice notice =
        new Notice(
            "Package expired: " + event.productCode(),
            "Version "
                + event.versionNo()
                + " ended on "
                + event.packageEndDate()
                + " without a renewal; the package is no longer sold for new business.",
            "/product-maintenance/expiry",
            "Product",
            event.productCode());
    notifications.notifyPermission("PKG_NEGOTIATE", notice);
    notifications.notifyPermission("PRODUCT_MAINTAIN", notice);
  }

  private Optional<PackageRequest> waiting(String requestNo) {
    if (requestNo == null) {
      return Optional.empty();
    }
    return requests
        .findByRequestNo(requestNo)
        .filter(p -> p.getStatus() == RequestStage.FOR_VALIDATION);
  }

  private static Advisory.Type advisoryType(RequestType type) {
    return switch (type) {
      case NEW, REACTIVATE -> Advisory.Type.PACKAGE_READY;
      case RENEW -> Advisory.Type.RENEWAL;
      default -> Advisory.Type.PACKAGE_UPDATED;
    };
  }
}
