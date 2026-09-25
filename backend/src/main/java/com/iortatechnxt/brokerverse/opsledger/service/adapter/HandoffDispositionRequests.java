package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import java.util.Optional;

/**
 * Default {@link UnappliedDispositionRequests} while cashiering does not implement the port: the
 * collector's request becomes a hand-off for the cashiers ({@code CASH_DISPOSITION}) and is
 * answered DEFERRED (BRCLXN.030-033, COLLECTIONS_DESIGN section 9).
 */
public class HandoffDispositionRequests implements UnappliedDispositionRequests {

  /** Port name on the hand-off. */
  public static final String PORT = "UnappliedDispositionRequests";

  private final HandoffService handoffs;
  private final ObjectMapper json;

  /**
   * Creates the adapter.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper (request payload)
   */
  public HandoffDispositionRequests(HandoffService handoffs, ObjectMapper json) {
    this.handoffs = handoffs;
    this.json = json;
  }

  @Override
  public DispositionTicket request(DispositionRequest request) {
    OpsHandoff handoff =
        handoffs.record(
            request.companyId(),
            PORT,
            "CASH_DISPOSITION",
            new OpsHandoff.Spec(
                request.source(),
                request.sourceRef(),
                request.unappliedRef(),
                request.amount(),
                null,
                "Dispose of unapplied item "
                    + request.unappliedRef()
                    + ": "
                    + request.action()
                    + (request.invoiceNo() == null ? "" : " to invoice " + request.invoiceNo())
                    + " (requested by "
                    + request.requestedBy()
                    + ")",
                HandoffPayloads.of(json, request)));
    return ticket(handoff);
  }

  @Override
  public Optional<DispositionTicket> status(String source, String sourceRef) {
    return handoffs.find(PORT, source, sourceRef).map(HandoffDispositionRequests::ticket);
  }

  private static DispositionTicket ticket(OpsHandoff handoff) {
    String message =
        handoff.getStatus() == OpsHandoff.Status.CLOSED
            ? "Done by hand in Cashiering: " + handoff.getClosingNote()
            : "Cashiering is not active: request handed over as hand-off " + handoff.getId();
    return new DispositionTicket(
        Status.DEFERRED, HandoffPayloads.reference(handoff.getId()), message);
  }
}
