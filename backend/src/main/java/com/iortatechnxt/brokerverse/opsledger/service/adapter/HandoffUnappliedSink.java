package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;

/**
 * Default {@link UnappliedSink} while cashiering is not installed: records a hand-off for the
 * cashiers ({@code CASH_DISPOSITION}) to set up the unapplied item by hand and answers DEFERRED.
 */
public class HandoffUnappliedSink implements UnappliedSink {

  /** Port name on the hand-off. */
  public static final String PORT = "UnappliedSink";

  private final HandoffService handoffs;
  private final ObjectMapper json;

  /**
   * Creates the adapter.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper (request payload)
   */
  public HandoffUnappliedSink(HandoffService handoffs, ObjectMapper json) {
    this.handoffs = handoffs;
    this.json = json;
  }

  @Override
  public UnappliedHandle create(UnappliedRequest request) {
    OpsHandoff handoff =
        handoffs.record(
            request.companyId(),
            PORT,
            "CASH_DISPOSITION",
            new OpsHandoff.Spec(
                request.source().module(),
                request.source().reference(),
                request.invoiceNo(),
                request.amount(),
                request.currency(),
                "Set up an unapplied "
                    + request.origin()
                    + " item for client "
                    + request.party().clientCode()
                    + (request.invoiceNo() == null ? "" : " from invoice " + request.invoiceNo()),
                payload(request)));
    return new UnappliedHandle(
        Status.DEFERRED,
        "HANDOFF-" + handoff.getId(),
        "Cashiering is not active: unapplied item handed over as hand-off " + handoff.getId());
  }

  private String payload(Object request) {
    try {
      return json.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      return String.valueOf(request);
    }
  }
}
