package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.BulkApprovalAction;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Approval of several access requests at once from the inbox (BASAU 2.5.3): each request is
 * approved and applied on its own by {@link AccessRequestService#approve}, with the four-eyes rule.
 * New-user requests are approved one by one, because their temporary password is shown only once.
 */
@Component
public class AccessRequestBulkApprovals implements BulkApprovalAction {

  private final AccessRequestService requests;
  private final CurrentUser currentUser;

  /**
   * Creates the component.
   *
   * @param requests access requests
   * @param currentUser current user
   */
  public AccessRequestBulkApprovals(AccessRequestService requests, CurrentUser currentUser) {
    this.requests = requests;
    this.currentUser = currentUser;
  }

  @Override
  public boolean supports(String module, String type) {
    return "BROKING_ADMIN".equals(module) && "Access request".equals(type);
  }

  @Override
  public String approve(Long companyId, String reference) {
    if (!currentUser.hasAuthority("ACCESS_APPROVE")) {
      throw new AccessDeniedException("Not permitted to approve access requests");
    }
    AccessRequest request = requests.byNumber(reference);
    if (request.getRequestType() == AccessRequestType.CREATE_USER) {
      throw new BusinessRuleException(
          "ACCESS_APPROVE_INDIVIDUALLY",
          "Approve " + reference + " on its own: the new user's temporary password is shown once");
    }
    return "Approved and applied "
        + requests.approve(request.getId(), null).request().getRequestNo();
  }
}
