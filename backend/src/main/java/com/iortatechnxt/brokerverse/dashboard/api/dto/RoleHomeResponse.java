package com.iortatechnxt.brokerverse.dashboard.api.dto;

import com.iortatechnxt.brokerverse.dashboard.domain.RoleHome;
import com.iortatechnxt.brokerverse.dashboard.service.RoleHomeService;
import java.time.Instant;
import java.util.List;

/**
 * The home page of a role on the Role Home Set-up screen.
 *
 * @param roleCode role code
 * @param roleName role name
 * @param widgets widget codes shown, in order
 * @param delivered whether the role shows the delivered widgets
 * @param pendingWidgets requested widgets waiting for approval (null when none)
 * @param pendingReason reason of the request
 * @param pendingBy requester (user name)
 * @param pendingAt request time
 * @param changedReason reason of the last change
 * @param changedBy requester of the last change
 * @param changedAt time of the last change
 * @param approvedBy approver of the last change
 */
public record RoleHomeResponse(
    String roleCode,
    String roleName,
    List<String> widgets,
    boolean delivered,
    List<String> pendingWidgets,
    String pendingReason,
    String pendingBy,
    Instant pendingAt,
    String changedReason,
    String changedBy,
    Instant changedAt,
    String approvedBy) {

  /**
   * Maps an entry.
   *
   * @param e role and its widget set
   * @return the response
   */
  public static RoleHomeResponse from(RoleHomeService.Entry e) {
    RoleHome h = e.home();
    return new RoleHomeResponse(
        e.role().getCode(),
        e.role().getName(),
        h.getWidgets(),
        h.isDelivered(),
        h.getPendingWidgets(),
        h.getPendingReason(),
        h.getPendingBy(),
        h.getPendingAt(),
        h.getChangedReason(),
        h.getChangedBy(),
        h.getChangedAt(),
        h.getApprovedBy());
  }
}
