package com.iortatechnxt.brokerverse.approval.api;

import com.iortatechnxt.brokerverse.approval.api.dto.BulkApprovalRequest;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService.ApprovalCounts;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Outcome;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final BulkApprovalService bulk;

  /**
   * Creates the controller.
   *
   * @param inbox inbox service
   * @param bulk bulk approval
   */
  public ApprovalController(ApprovalInboxService inbox, BulkApprovalService bulk) {
    this.inbox = inbox;
    this.bulk = bulk;
  }

  /**
   * Approves several inbox items at once (FRBS 2.5.6, BASAU 2.5.3). Each module checks the
   * approving permission and approves every item on its own; the outcome of each item is returned.
   *
   * @param request items
   * @return outcomes in the order of the items
   */
  @PostMapping("/bulk-approve")
  @PreAuthorize("isAuthenticated()")
  public List<Outcome> bulkApprove(@Valid @RequestBody BulkApprovalRequest request) {
    return bulk.approve(request.toItems());
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
