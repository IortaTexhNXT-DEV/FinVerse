package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source (BRNB.079; USER_ACCESS_DESIGN section 7): pending access requests for their
 * chosen approver (every approver when {@code UAM_ANY_APPROVER} is true, or when the request has no
 * chosen approver), PENDING_SECOND requests for the holders of UAM_SECOND_APPROVE other than the
 * first approver, and approved group-profile requests to implement for ROLE_MANAGE. Never the
 * requester's own requests, nor a request about the viewer's own access.
 */
@Component
public class AccessRequestApprovalSource implements PendingApprovalSource {

  /** Inbox module. */
  public static final String MODULE = "BROKING_ADMIN";

  /** Inbox type of a request to decide. */
  public static final String TYPE = "Access request";

  /** Inbox type of a group-profile request to implement. */
  public static final String IMPLEMENT_TYPE = "Group profile to implement";

  private final AccessRequestService requests;
  private final AccessSettings settings;

  /**
   * Creates the source.
   *
   * @param requests access requests
   * @param settings parameters (any approver)
   */
  public AccessRequestApprovalSource(AccessRequestService requests, AccessSettings settings) {
    this.requests = requests;
    this.settings = settings;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    boolean anyApprover = settings.anyApprover();
    return requests.pending().stream()
        .filter(r -> viewer.mayApproveItemOf(AccessRequestNotifier.requester(r)))
        .filter(
            r -> viewer.systemView() || !CurrentUser.sameUser(viewer.username(), r.getUsername()))
        .filter(r -> waitsFor(r, viewer, anyApprover))
        .map(AccessRequestApprovalSource::item)
        .toList();
  }

  private static boolean waitsFor(AccessRequest r, ApprovalViewer viewer, boolean anyApprover) {
    return switch (r.getStatus()) {
      case PENDING ->
          viewer.can(AccessApprovers.approvalPermission(r.getUserType()))
              && (viewer.systemView()
                  || anyApprover
                  || r.getAssignedApprover() == null
                  || CurrentUser.sameUser(viewer.username(), r.getAssignedApprover()));
      case PENDING_SECOND ->
          viewer.can("UAM_SECOND_APPROVE")
              && (viewer.systemView() || !r.approvedBy(viewer.username()));
      case FOR_IMPLEMENTATION -> viewer.can("ROLE_MANAGE");
      default -> false;
    };
  }

  private static PendingApproval item(AccessRequest r) {
    return new PendingApproval(
        MODULE,
        r.getStatus() == AccessRequestStatus.FOR_IMPLEMENTATION ? IMPLEMENT_TYPE : TYPE,
        r.getRequestNo(),
        AccessRequestService.describe(r),
        null,
        null,
        AccessRequestNotifier.requester(r),
        r.getSubmittedAt() == null ? r.getCreatedAt() : r.getSubmittedAt(),
        null,
        AccessRequestService.link(r));
  }
}
