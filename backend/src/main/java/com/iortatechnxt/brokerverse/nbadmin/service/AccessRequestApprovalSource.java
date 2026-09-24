package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source (BRNB.079 "profile creation / modification"): pending user access requests
 * for holders of ACCESS_APPROVE, never the requester's own.
 */
@Component
public class AccessRequestApprovalSource implements PendingApprovalSource {

  private final AccessRequestService requests;

  /**
   * Creates the source.
   *
   * @param requests access requests
   */
  public AccessRequestApprovalSource(AccessRequestService requests) {
    this.requests = requests;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("ACCESS_APPROVE")) {
      return List.of();
    }
    return requests.pending().stream()
        .filter(r -> viewer.mayApproveItemOf(r.getCreatedBy()))
        .map(
            r ->
                new PendingApproval(
                    "BROKING_ADMIN",
                    "Access request",
                    r.getRequestNo(),
                    AccessRequestService.describe(r),
                    null,
                    null,
                    r.getCreatedBy(),
                    r.getCreatedAt(),
                    null,
                    AccessRequestService.SCREEN + "?id=" + r.getId()))
        .toList();
  }
}
