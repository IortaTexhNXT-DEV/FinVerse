package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.crm.domain.PayoutDetails;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import com.iortatechnxt.brokerverse.payrequest.domain.CancellationTarget;
import com.iortatechnxt.brokerverse.payrequest.domain.Payee;
import com.iortatechnxt.brokerverse.payrequest.domain.PayeeType;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * What an approved request does outside payrequest (MKT 2.24.0, 2.25.0, 1.19.0): a refund or cash
 * advance is sent to Disbursement through the Operations {@code DisbursementGateway} (type REFUND
 * or CASH_ADVANCE, the request number as RFP number, the documents of the request); the CA / SA
 * information of a refund is added to the client record; an approved check cancellation is handed
 * to the Disbursement approvers, who cancel the approved DV (DIS 2.20.0) since the gateway has no
 * cancellation call.
 */
@Component
public class DisbursementLink {

  /** Port name of the hand-off of a check cancellation. */
  public static final String CANCEL_PORT = "DV_CANCELLATION";

  private static final int MAX_DESCRIPTION = 500;

  private final DisbursementGateway gateway;
  private final ClientPayoutAccounts payouts;
  private final HandoffService handoffs;
  private final AttachmentService attachments;
  private final AuditTrailService audit;

  /**
   * Creates the link.
   *
   * @param gateway Disbursement port
   * @param payouts client CA / SA information
   * @param handoffs hand-offs
   * @param attachments supporting documents
   * @param audit audit trail
   */
  public DisbursementLink(
      DisbursementGateway gateway,
      ClientPayoutAccounts payouts,
      HandoffService handoffs,
      AttachmentService attachments,
      AuditTrailService audit) {
    this.gateway = gateway;
    this.payouts = payouts;
    this.handoffs = handoffs;
    this.attachments = attachments;
    this.audit = audit;
  }

  /**
   * Sends an approved refund or cash advance to Disbursement (MKT 2.24.0), idempotent on the
   * request number.
   *
   * @param request approved request
   */
  public void send(PaymentRequest request) {
    boolean refund = request.getKind() == RequestKind.REFUND;
    Payee payee = request.getPayee();
    DisbursementRequest.Type type =
        refund ? DisbursementRequest.Type.REFUND : DisbursementRequest.Type.CASH_ADVANCE;
    DisbursementRequest.Spec spec =
        new DisbursementRequest.Spec(
                type,
                PayRequests.MODULE,
                request.nextSendRef(),
                payee.code(),
                payee.name(),
                request.getContent().currency(),
                request.getAmount(),
                description(request),
                null)
            .routed(
                request.getRequestNo(),
                payee.type() == PayeeType.CLIENT ? "CLIENT" : "EMPLOYEE",
                type.name(),
                request.rootInvoiceNo(),
                false)
            .withReferences(documents(request), List.of());
    DisbursementTicket ticket = gateway.send(request.getCompanyId(), spec);
    request.track(
        request
            .trackOrNone()
            .status(ticket.requestNo(), ticket.status().name(), ticket.dvNo(), ticket.message()));
    audit.record(
        PayRequests.ENTITY,
        request.getRequestNo(),
        AuditAction.SUBMIT,
        "Sent to Disbursement as " + ticket.requestNo());
    if (refund) {
      recordPayout(request);
    }
  }

  /**
   * Hands an approved check cancellation to the Disbursement approvers (MKT 1.19.0, DIS 2.20.0).
   *
   * @param request approved check cancellation
   */
  public void handOffCancellation(PaymentRequest request) {
    CancellationTarget target = request.targetOrNone();
    OpsHandoff handoff =
        handoffs.record(
            request.getCompanyId(),
            CANCEL_PORT,
            "DISB_APPROVE",
            new OpsHandoff.Spec(
                PayRequests.MODULE,
                request.getRequestNo(),
                target.dvNo(),
                request.getAmount(),
                request.getContent().currency(),
                "Cancel DV "
                    + target.dvNo()
                    + (target.checkNo() == null ? "" : " (check " + target.checkNo() + ")")
                    + " of "
                    + target.requestNo()
                    + ", reason "
                    + target.reasonCode(),
                null));
    request.track(request.trackOrNone().handedOff("HANDOFF-" + handoff.getId()));
    audit.record(
        PayRequests.ENTITY,
        request.getRequestNo(),
        AuditAction.SUBMIT,
        "Check cancellation handed to Disbursement (hand-off " + handoff.getId() + ")");
  }

  private void recordPayout(PaymentRequest request) {
    Payee payee = request.getPayee();
    PayoutMode mode = PayRequestRules.payoutMode(payee.mode());
    if (mode == null || request.isPayoutRecorded()) {
      return;
    }
    payouts.record(
        request.getCompanyId(),
        payee.code(),
        new PayoutDetails(
            mode,
            payee.accountName() == null ? payee.name() : payee.accountName(),
            payee.accountNo()),
        PayRequests.MODULE,
        request.getRequestNo());
    request.payoutRecorded();
  }

  private List<String> documents(PaymentRequest request) {
    return attachments
        .list(new AttachmentTarget(PayRequests.ENTITY, String.valueOf(request.getId())))
        .stream()
        .map(a -> String.valueOf(a.getId()))
        .toList();
  }

  private static String description(PaymentRequest request) {
    String purpose = request.getContent().purpose();
    String text = request.getRequestNo() + " " + (purpose == null ? "" : purpose);
    return text.length() > MAX_DESCRIPTION ? text.substring(0, MAX_DESCRIPTION) : text;
  }
}
