package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port: ask Cashiering to dispose of an unapplied payment item on a collector's instruction
 * (BRCLXN.030-033, 041; COLLECTIONS_DESIGN sections 2.1 and 9). Cashiering creates or updates its
 * own disposition (source COLLECTIONS) so its approval rules stay; the outcome comes back as {@code
 * OpsLedgerEvents.UnappliedDispositionChanged}. Implemented by cashiering; the default adapter
 * ({@code HandoffDispositionRequests}) records a hand-off for the team {@code CASH_DISPOSITION}.
 */
public interface UnappliedDispositionRequests {

  /**
   * Requests a disposition, idempotent on (source, source reference).
   *
   * @param request what to do with which item
   * @return the ticket
   */
  DispositionTicket request(DispositionRequest request);

  /**
   * The ticket of an earlier request.
   *
   * @param source requesting module
   * @param sourceRef its reference
   * @return ticket, empty when never requested
   */
  Optional<DispositionTicket> status(String source, String sourceRef);

  /** What the collector asks for. */
  enum Action {
    /** Apply the payment to an invoice ("For application to invoice", BRCLXN.030/041). */
    APPLY_TO_INVOICE,
    /** Refund the payment to the payor. */
    REFUND,
    /** Reclassify the payment (other client or type). */
    RECLASS,
    /** Transfer the payment to another marketing unit. */
    TRANSFER
  }

  /** Where a request stands when it is answered. */
  enum Status {
    /** Cashiering recorded its disposition; the decision follows as an event. */
    SUBMITTED,
    /** No cashiering module: handed over to be done by hand. */
    DEFERRED,
    /** Refused at once (item unknown, fully disposed, amount above the balance). */
    REJECTED
  }

  /**
   * A disposition request.
   *
   * @param companyId company
   * @param unappliedRef item reference (Cashiering key)
   * @param action what to do
   * @param invoiceNo target invoice (mandatory for APPLY_TO_INVOICE), may be null
   * @param amount amount, null for the whole unapplied balance
   * @param requestedBy collector
   * @param source requesting module (COLLECTIONS)
   * @param sourceRef its reference (idempotency key)
   * @param remarks remarks, may be null
   */
  record DispositionRequest(
      Long companyId,
      String unappliedRef,
      Action action,
      String invoiceNo,
      BigDecimal amount,
      String requestedBy,
      String source,
      String sourceRef,
      String remarks) {}

  /**
   * Answer to a request.
   *
   * @param status submitted, deferred or rejected
   * @param reference Cashiering disposition reference, or the hand-off reference when deferred
   * @param message what happened
   */
  record DispositionTicket(Status status, String reference, String message) {}
}
