package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;

/**
 * Default {@link PaymentReversalRequester} while cashiering does not implement the port (ACSL
 * 2.6.0-2.6.1): the reversal becomes a hand-off for the cashiers ({@code CASH_APPLY}), who reverse
 * the application with the cashiering screens; nothing is posted here.
 */
public class HandoffPaymentReversalRequester implements PaymentReversalRequester {

  /** Port name on the hand-off. */
  public static final String PORT = "PaymentReversalRequester";

  private final HandoffService handoffs;
  private final ObjectMapper json;

  /**
   * Creates the adapter.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper (request payload)
   */
  public HandoffPaymentReversalRequester(HandoffService handoffs, ObjectMapper json) {
    this.handoffs = handoffs;
    this.json = json;
  }

  @Override
  public ReversalTicket request(ReversalRequest request) {
    OpsHandoff handoff =
        handoffs.record(
            request.companyId(),
            PORT,
            "CASH_APPLY",
            new OpsHandoff.Spec(
                request.source().module(),
                request.source().reference(),
                request.invoiceNo(),
                request.amount(),
                request.currency(),
                "Reverse the payment "
                    + request.receiptNo()
                    + " applied to invoice "
                    + request.invoiceNo()
                    + ": "
                    + request.reason(),
                HandoffPayloads.of(json, request)));
    return new ReversalTicket(
        Status.DEFERRED,
        HandoffPayloads.reference(handoff.getId()),
        "Cashiering reversal is not available: handed over as hand-off " + handoff.getId());
  }
}
