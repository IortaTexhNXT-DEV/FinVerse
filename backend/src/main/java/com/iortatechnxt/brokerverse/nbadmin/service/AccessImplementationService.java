package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.security.service.ApprovedRoleRequests;
import com.iortatechnxt.brokerverse.security.service.RoleChangedOnRequest;
import java.time.Clock;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of approved group-profile requests by the System Administrator (BRD-11 p.6 "System
 * Administrator to create / modify group profile"; FR-UA-045; PQ17): "Implement Request" applies
 * the approved change through the security administration service with the request number and
 * approver, and the request becomes IMPLEMENTED. The implementer is never the requester.
 *
 * <p>Also the {@code nbadmin} adapter of the security port {@link ApprovedRoleRequests}: the Roles
 * screen may change a role with the number of a request waiting for implementation, and such a
 * change ({@link RoleChangedOnRequest}) implements the request.
 */
@Service
@Transactional
public class AccessImplementationService implements ApprovedRoleRequests {

  private final AccessRequestRepository requests;
  private final AccessChangeApplier applier;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param applier applies the approved change
   * @param history request history
   * @param notifier notifications
   * @param currentUser current user (the System Administrator)
   * @param clock clock
   */
  public AccessImplementationService(
      AccessRequestRepository requests,
      AccessChangeApplier applier,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.applier = applier;
    this.history = history;
    this.notifier = notifier;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Implements an approved group-profile request (FR-UA-045).
   *
   * @param id request
   * @return the implemented request
   */
  public AccessRequest implement(Long id) {
    AccessRequest r =
        requests
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(AccessRequestService.ENTITY, id));
    requireImplementable(r);
    applier.apply(r);
    if (r.getStatus() == AccessRequestStatus.FOR_IMPLEMENTATION) {
      markImplemented(r, currentUser.username());
    }
    return r;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> approverOf(String requestNo, String roleCode) {
    return requests
        .findByRequestNo(requestNo)
        .filter(r -> r.getStatus() == AccessRequestStatus.FOR_IMPLEMENTATION)
        .filter(r -> roleCode != null && roleCode.equals(r.getRoleCode()))
        .filter(
            r -> !CurrentUser.sameUser(currentUser.username(), AccessRequestNotifier.requester(r)))
        .map(AccessRequest::getDecidedBy);
  }

  /**
   * A role changed on the authority of a request: the request waiting for implementation is
   * implemented (the Roles screen path of PQ17, or {@link #implement}).
   *
   * @param event the change
   */
  @EventListener
  public void on(RoleChangedOnRequest event) {
    requests
        .findByRequestNo(event.requestNo())
        .filter(r -> r.getStatus() == AccessRequestStatus.FOR_IMPLEMENTATION)
        .filter(r -> event.roleCode().equals(r.getRoleCode()))
        .ifPresent(r -> markImplemented(r, event.actor()));
  }

  private void requireImplementable(AccessRequest r) {
    if (r.getStatus() != AccessRequestStatus.FOR_IMPLEMENTATION) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_NOT_FOR_IMPLEMENTATION",
          "Request "
              + r.getRequestNo()
              + " is "
              + r.getStatus()
              + ", not waiting for implementation");
    }
    if (CurrentUser.sameUser(currentUser.username(), AccessRequestNotifier.requester(r))
        || CurrentUser.sameUser(currentUser.username(), r.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_IMPLEMENTER_IS_REQUESTER",
          "A request is implemented by someone other than its requester");
    }
  }

  private void markImplemented(AccessRequest r, String by) {
    r.implemented(by, clock.instant());
    history.record(r, AccessRequestAction.IMPLEMENT, AccessRequestStatus.FOR_IMPLEMENTATION, null);
    notifier.decided(r, "implemented");
  }
}
