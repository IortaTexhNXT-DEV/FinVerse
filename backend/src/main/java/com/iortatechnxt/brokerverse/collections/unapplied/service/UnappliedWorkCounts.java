package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest.Status;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequestRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The unapplied-payment tiles of the Collections home (COLLECTIONS_DESIGN 11, CQ21) for the users
 * who dispose of unapplied payments ({@code CLX_UNAPPLIED_WORK}): the open unapplied payments
 * awaiting a disposition and my requests still open in Cashiering.
 */
@Component
@Transactional(readOnly = true)
public class UnappliedWorkCounts implements CollectionsWorkCountSource {

  private static final int ORDER = 50;

  private final UnappliedDirectory directory;
  private final ApplicationRequestRepository requests;
  private final CurrentUser currentUser;

  /**
   * Creates the source.
   *
   * @param directory Cashiering's unapplied items
   * @param requests requests to Cashiering
   * @param currentUser signed-in user
   */
  public UnappliedWorkCounts(
      UnappliedDirectory directory,
      ApplicationRequestRepository requests,
      CurrentUser currentUser) {
    this.directory = directory;
    this.requests = requests;
    this.currentUser = currentUser;
  }

  @Override
  public List<WorkCount> counts(Long companyId, String username) {
    if (!currentUser.hasAuthority("CLX_UNAPPLIED_WORK")) {
      return List.of();
    }
    long open =
        directory
            .open(
                companyId,
                new UnappliedFilter(null, null, null, "UNAPPLIED", null, null),
                PageRequest.of(0, 1))
            .getTotalElements();
    long mine =
        requests.countByCompanyIdAndRequestedByIgnoreCaseAndStatusIn(
            companyId, username, List.of(Status.SENT, Status.DEFERRED, Status.ACCEPTED));
    return List.of(
        new WorkCount(
            "unapplied",
            "Unapplied Awaiting Disposition",
            open,
            "/collections/unapplied?tab=UNAPPLIED",
            open > 0,
            ORDER),
        new WorkCount(
            "unappliedRequests",
            "My Requests in Cashiering",
            mine,
            "/collections/unapplied/requests",
            false,
            ORDER + 1));
  }
}
