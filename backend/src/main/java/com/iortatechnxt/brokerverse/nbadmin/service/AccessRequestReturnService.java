package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Return of an access request to its requester and its resubmission (BASAU 2.4.1, 2.6.0 - 2.6.3).
 */
@Service
@Transactional
public class AccessRequestReturnService {

  private final AccessRequestService requests;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests access requests
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccessRequestReturnService(
      AccessRequestService requests,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Returns a request to its requester with remarks (BASAU 2.4.1, 2.6.0 / 2.6.1): four eyes, the
   * remarks are mandatory and the requester is notified.
   *
   * @param id request
   * @param comment remarks
   * @return the returned request
   */
  public AccessRequest returnRequest(Long id, String comment) {
    if (comment == null || comment.isBlank()) {
      throw new BusinessRuleException(
          "ACCESS_RETURN_REASON", "Enter the remarks for the requester");
    }
    AccessRequest request = requests.get(id);
    String approver = currentUser.username();
    if (CurrentUser.sameUser(approver, request.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_FOUR_EYES", "A request cannot be decided by the user who submitted it");
    }
    request.returnToRequester(approver, clock.instant(), comment.trim());
    audit.record(
        AccessRequestService.ENTITY,
        request.getRequestNo(),
        AuditAction.REJECT,
        "Returned: " + AccessRequestService.describe(request) + AccessRequestService.note(comment));
    requests.notifyRequester(request, "returned");
    return request;
  }

  /**
   * Resubmits a returned request (requester only) with a corrected justification.
   *
   * @param id request
   * @param justification justification answering the remarks
   * @return the pending request
   */
  public AccessRequest resubmit(Long id, String justification) {
    AccessRequest request = requests.get(id);
    if (!CurrentUser.sameUser(currentUser.username(), request.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_NOT_REQUESTER", "Only " + request.getCreatedBy() + " can resubmit this request");
    }
    if (justification == null || justification.isBlank()) {
      throw new BusinessRuleException("ACCESS_JUSTIFICATION", "Enter the justification");
    }
    request.resubmit(justification.trim());
    audit.record(
        AccessRequestService.ENTITY,
        request.getRequestNo(),
        AuditAction.SUBMIT,
        "Resubmitted: " + AccessRequestService.describe(request));
    notifications.notifyPermission(
        "ACCESS_APPROVE",
        new Notice(
            "Access request " + request.getRequestNo() + " resubmitted",
            AccessRequestService.describe(request),
            AccessRequestService.link(request),
            AccessRequestService.ENTITY,
            String.valueOf(request.getId())));
    return request;
  }
}
