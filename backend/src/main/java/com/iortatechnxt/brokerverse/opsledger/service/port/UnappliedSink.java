package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;

/**
 * Port: create an unapplied item for disposition (CSHID.024/025). Implemented by cashiering; called
 * by adjustment (excess after a decrease or cancellation), commission (DP reinstatement) and
 * remittance (return of an excluded paid AR). The default adapter, while cashiering is not
 * installed, records a hand-off and returns {@link Status#DEFERRED}.
 */
public interface UnappliedSink {

  /**
   * Creates an unapplied item, idempotent on (source module, source reference).
   *
   * @param request amount, client and source
   * @return the item reference, or a deferral
   */
  UnappliedHandle create(UnappliedRequest request);

  /** Outcome. */
  enum Status {
    /** Item created in the unapplied workbench. */
    CREATED,
    /** Handed over to be set up by hand (no cashiering module). */
    DEFERRED
  }

  /**
   * An unapplied item to create.
   *
   * @param companyId company
   * @param origin ADJUSTMENT, CANCELLATION, DP_REINSTATE or REMITTANCE_RETURN
   * @param party client code and sales unit the money belongs to
   * @param currency currency
   * @param amount positive amount
   * @param invoiceNo invoice it came from, may be null
   * @param dispositionHint suggested disposition (LOV {@code DISPOSITION_TYPE}), may be null
   * @param source module and reference (idempotency) with remarks
   */
  record UnappliedRequest(
      Long companyId,
      String origin,
      Party party,
      String currency,
      BigDecimal amount,
      String invoiceNo,
      String dispositionHint,
      Source source) {}

  /**
   * Whose money it is.
   *
   * @param clientCode client
   * @param salesUnit marketing unit
   */
  record Party(String clientCode, String salesUnit) {}

  /**
   * Where the request comes from.
   *
   * @param module source module
   * @param reference source reference
   * @param remarks remarks, may be null
   */
  record Source(String module, String reference, String remarks) {}

  /**
   * Result.
   *
   * @param status created or deferred
   * @param reference unapplied item reference, or the hand-off reference when deferred
   * @param message what happened
   */
  record UnappliedHandle(Status status, String reference, String message) {}
}
