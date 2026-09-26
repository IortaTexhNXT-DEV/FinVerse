package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;

/** Names and small rules shared by the payrequest services. */
public final class PayRequests {

  /** Module name on gateway requests, hand-offs, validations and the audit trail. */
  public static final String MODULE = "PAYREQUEST";

  /** Entity type of the work case, the attachments and the audit trail. */
  public static final String ENTITY = "PaymentRequest";

  /** Notification event of request status changes (V894). */
  public static final String STATUS_EVENT = "PRQ_REQUEST_STATUS";

  /** Base route of a request page. */
  public static final String LINK = "/payment-requests/requests/";

  /** Error code of an action in the wrong stage. */
  public static final String WRONG_STAGE = "PRQ_WRONG_STAGE";

  /** Error code of a four-eyes violation. */
  public static final String FOUR_EYES = "PRQ_FOUR_EYES";

  private PayRequests() {}

  /**
   * The route of a request page.
   *
   * @param id request id
   * @return frontend route
   */
  public static String link(Long id) {
    return LINK + id;
  }

  /**
   * Refuses an action outside the expected stages.
   *
   * @param request request
   * @param expected allowed stages
   */
  public static void requireStage(PaymentRequest request, RequestStage... expected) {
    for (RequestStage stage : expected) {
      if (request.getStage() == stage) {
        return;
      }
    }
    throw new BusinessRuleException(
        WRONG_STAGE, request.getRequestNo() + " is " + request.getStage() + " for this action");
  }

  /**
   * A workflow note from an optional comment.
   *
   * @param comment comment, may be blank
   * @return note
   */
  public static TransitionNote note(String comment) {
    return TransitionNote.comment(comment == null || comment.isBlank() ? null : comment.strip());
  }
}
