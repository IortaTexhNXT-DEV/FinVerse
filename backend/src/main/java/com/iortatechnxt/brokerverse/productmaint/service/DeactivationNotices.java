package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.DeactivationStatus;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import org.springframework.stereotype.Component;

/**
 * Notices of the package deactivation requests (BDOI FRS FRPM.003.04, FRPM.003.07 and FRPM.017.01),
 * in the system and by e-mail as each user prefers: the approver of a new request, the requestor of
 * the decision, and the new and the original approver of a reassignment.
 */
@Component
public class DeactivationNotices {

  private final NoticeDelivery delivery;
  private final UserDirectory directory;

  /**
   * Creates the notices.
   *
   * @param delivery in-app and e-mail delivery
   * @param directory user names
   */
  public DeactivationNotices(NoticeDelivery delivery, UserDirectory directory) {
    this.delivery = delivery;
    this.directory = directory;
  }

  /**
   * Tells the approver that a request waits for the decision.
   *
   * @param r request
   * @param link screen link
   */
  void forApproval(DeactivationRequest r, String link) {
    delivery.toUser(
        r.getApprover(),
        notice(
            "Package deactivation for approval: " + r.getRequestNo(),
            directory.displayName(r.getCreatedBy())
                + " asks to deactivate "
                + r.getPackageName()
                + " from "
                + DisplayFormat.date(r.getEffectiveDate())
                + ".",
            link,
            r),
        DeactivationService.APPROVAL_EVENT,
        true);
  }

  /**
   * Tells the requestor of the decision.
   *
   * @param r request
   * @param link screen link
   */
  void decided(DeactivationRequest r, String link) {
    String body =
        r.getStatus() == DeactivationStatus.APPROVED
            ? r.getPackageName()
                + " will be deactivated after its expiry date "
                + DisplayFormat.date(r.getExpiryDate())
                + "."
            : r.getPackageName() + " stays active. Remarks: " + r.getDecisionRemarks();
    delivery.toUser(
        r.getCreatedBy(),
        notice(
            "Package deactivation "
                + r.getStatus().label().toLowerCase(java.util.Locale.ROOT)
                + ": "
                + r.getRequestNo(),
            body,
            link,
            r),
        DeactivationService.DECIDED_EVENT,
        true);
  }

  /**
   * Tells the new approver of the request and the original approver of the reassignment.
   *
   * @param r request (new approver)
   * @param original original approver
   * @param link screen link
   */
  void reassigned(DeactivationRequest r, String original, String link) {
    forApproval(r, link);
    delivery.toUser(
        original,
        notice(
            "Package deactivation reassigned: " + r.getRequestNo(),
            "The request was reassigned to " + directory.displayName(r.getApprover()) + ".",
            link,
            r),
        DeactivationService.APPROVAL_EVENT,
        true);
  }

  private static Notice notice(String title, String body, String link, DeactivationRequest r) {
    return new Notice(title, body, link, DeactivationService.ENTITY, String.valueOf(r.getId()));
  }
}
