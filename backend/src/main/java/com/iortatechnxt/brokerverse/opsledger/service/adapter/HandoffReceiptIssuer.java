package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;

/**
 * Default {@link ReceiptIssuer} while cashiering is not installed: records a hand-off for the
 * cashiers ({@code CASH_RECEIPT}) to issue the OR by hand and answers DEFERRED, never a number.
 */
public class HandoffReceiptIssuer implements ReceiptIssuer {

  /** Port name on the hand-off. */
  public static final String PORT = "ReceiptIssuer";

  private final HandoffService handoffs;
  private final ObjectMapper json;

  /**
   * Creates the adapter.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper (request payload)
   */
  public HandoffReceiptIssuer(HandoffService handoffs, ObjectMapper json) {
    this.handoffs = handoffs;
    this.json = json;
  }

  @Override
  public IssuedReceipt issueOfficialReceipt(ReceiptRequest request) {
    OpsHandoff handoff =
        handoffs.record(
            request.companyId(),
            PORT,
            "CASH_RECEIPT",
            new OpsHandoff.Spec(
                request.source().module(),
                request.source().reference(),
                request.payee().partyCode(),
                request.gross(),
                request.currency(),
                "Issue a "
                    + request.orType()
                    + " OR to "
                    + request.payee().name()
                    + " for "
                    + request.source().module()
                    + " "
                    + request.source().reference(),
                payload(request)));
    return new IssuedReceipt(
        Status.DEFERRED,
        null,
        null,
        "Cashiering is not active: OR handed over as hand-off " + handoff.getId());
  }

  private String payload(Object request) {
    try {
      return json.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      return String.valueOf(request);
    }
  }
}
