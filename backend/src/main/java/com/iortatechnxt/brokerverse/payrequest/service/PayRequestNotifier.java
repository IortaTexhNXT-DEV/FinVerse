package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Notifications of the requests (MKT 1.20.0, 2.26.0): the requester hears of validations, returns,
 * approvals and the payment; the next team hears of work waiting for it.
 */
@Component
public class PayRequestNotifier {

  private final NotificationService notifications;

  /**
   * Creates the notifier.
   *
   * @param notifications notifications
   */
  public PayRequestNotifier(NotificationService notifications) {
    this.notifications = notifications;
  }

  /**
   * Tells the requester what happened.
   *
   * @param request request
   * @param what what happened, e.g. "disbursed"
   */
  public void requester(PaymentRequest request, String what) {
    notifications.notifyUser(
        request.getCreatedBy(), notice(request, what), PayRequests.STATUS_EVENT);
  }

  /**
   * Tells the holders of a permission that work waits for them.
   *
   * @param permission permission of the next step
   * @param request request
   * @param what what waits, e.g. "for approval"
   */
  public void team(String permission, PaymentRequest request, String what) {
    notifications.notifyPermission(permission, notice(request, what), PayRequests.STATUS_EVENT);
  }

  private static Notice notice(PaymentRequest request, String what) {
    return new Notice(
        request.getRequestNo() + " " + what,
        request.getKind().name().replace('_', ' ').toLowerCase(Locale.ROOT)
            + " request of "
            + request.getPayee().name()
            + " ("
            + request.getContent().currency()
            + " "
            + request.getAmount().toPlainString()
            + ")",
        PayRequests.link(request.getId()),
        PayRequests.ENTITY,
        String.valueOf(request.getId()));
  }
}
