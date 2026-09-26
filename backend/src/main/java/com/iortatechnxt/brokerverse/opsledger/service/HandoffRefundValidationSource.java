package com.iortatechnxt.brokerverse.opsledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;

/**
 * Default {@link RefundValidationSource} for a validator whose module is not installed (MKT 1.11.0,
 * ACSL 2.5.5): records a hand-off for the validating team ({@code ACSL_PROCESS} for ACSL, {@code
 * CASH_DISPOSITION} for Cashiering) and answers DEFERRED; the result is then entered by hand. It is
 * the port's default bean ({@code OpsPortDefaults}) and the fallback of {@link RefundValidations}.
 */
public class HandoffRefundValidationSource implements RefundValidationSource {

  /** Port name on the hand-off. */
  public static final String PORT = "RefundValidationSource";

  private final HandoffService handoffs;
  private final ObjectMapper json;

  /**
   * Creates the adapter.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper (request payload)
   */
  public HandoffRefundValidationSource(HandoffService handoffs, ObjectMapper json) {
    this.handoffs = handoffs;
    this.json = json;
  }

  @Override
  public String validator() {
    return ANY;
  }

  @Override
  public ValidationTicket open(ValidationRequest request) {
    OpsHandoff handoff =
        handoffs.record(
            request.companyId(),
            PORT,
            ACSL.equals(request.validator()) ? "ACSL_PROCESS" : "CASH_DISPOSITION",
            new OpsHandoff.Spec(
                request.source().module(),
                request.validator() + ":" + request.source().reference(),
                request.invoiceNo(),
                request.amount(),
                request.currency(),
                "Validate the refund of AR "
                    + request.arNo()
                    + " of client "
                    + request.clientCode()
                    + " ("
                    + request.validator()
                    + ", "
                    + request.source().module()
                    + " "
                    + request.source().reference()
                    + ")",
                payload(request)));
    return new ValidationTicket(
        request.validator(),
        Status.DEFERRED,
        "HANDOFF-" + handoff.getId(),
        request.validator()
            + " is not active: validation handed over as hand-off "
            + handoff.getId());
  }

  private String payload(ValidationRequest request) {
    try {
      return json.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      return String.valueOf(request);
    }
  }
}
