package com.iortatechnxt.brokerverse.approval.api;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService.ApprovalCounts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Universal approval inbox ("My Approvals"). Open to every signed-in user: each source returns only
 * the items the user holds the approving permission for, so no extra permission is required.
 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

  private final ApprovalInboxService inbox;

  /**
   * Creates the controller.
   *
   * @param inbox inbox service
   */
  public ApprovalController(ApprovalInboxService inbox) {
    this.inbox = inbox;
  }

  /**
   * Items waiting for the current user's approval, oldest first.
   *
   * @param companyId optional company filter
   * @return pending items
   */
  @GetMapping("/inbox")
  @PreAuthorize("isAuthenticated()")
  public List<PendingApproval> inbox(@RequestParam(required = false) Long companyId) {
    return inbox.inbox(companyId);
  }

  /**
   * Inbox counts (header badge).
   *
   * @param companyId optional company filter
   * @return counts
   */
  @GetMapping("/counts")
  @PreAuthorize("isAuthenticated()")
  public ApprovalCounts counts(@RequestParam(required = false) Long companyId) {
    return inbox.counts(companyId);
  }
}
