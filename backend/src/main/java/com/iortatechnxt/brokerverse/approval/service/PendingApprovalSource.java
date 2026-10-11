package com.iortatechnxt.brokerverse.approval.service;

import java.util.List;

/**
 * Port through which a module contributes items to the universal approval inbox.
 *
 * <p>Implement it as a Spring bean in your module's {@code service} package (your module then
 * depends on {@code approval}, never the other way round). Return only items the viewer may act on:
 * check the permission with {@link ApprovalViewer#can}, exclude the viewer's own submissions with
 * {@link ApprovalViewer#mayApproveItemOf}, and return everything for {@link
 * ApprovalViewer#system()} (used by the pending-approval ageing alert).
 */
public interface PendingApprovalSource {

  /**
   * Pending items visible to a viewer.
   *
   * @param viewer viewer
   * @return pending items (any order)
   */
  List<PendingApproval> pendingFor(ApprovalViewer viewer);
}
