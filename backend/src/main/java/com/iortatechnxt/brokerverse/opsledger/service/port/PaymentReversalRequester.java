package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Port: ask Cashiering for a sub-ledger payment reversal or re-application with approval (ACSL
 * 2.6.0-2.6.1; ACCOUNTING_DISBURSEMENT_DESIGN 2.2). Cashiering reverses the application with its
 * own events (payment back to unapplied) after approval; the outcome comes back as {@code
 * OpsLedgerEvents.PaymentReversalCompleted}. Implemented by cashiering; called by acsl. The default
 * adapter ({@code HandoffPaymentReversalRequester}) records a hand-off for the team {@code
 * CASH_APPLY} and posts nothing.
 */
public interface PaymentReversalRequester {

  /**
   * Requests a reversal, idempotent on (source module, source reference).
   *
   * @param request what to reverse
   * @return the ticket
   */
  ReversalTicket request(ReversalRequest request);

  /** Where a request stands when it is answered. */
  enum Status {
    /** Cashiering recorded the reversal request for approval; the decision follows as an event. */
    SUBMITTED,
    /** No cashiering adapter: handed over to be done by hand. */
    DEFERRED
  }

  /**
   * A payment reversal.
   *
   * @param companyId company
   * @param invoiceNo invoice the payment was applied to
   * @param receiptNo acknowledgement or official receipt of the payment
   * @param currency currency
   * @param amount amount to reverse, null for the whole application
   * @param valueDate value date of the reversal
   * @param reason reason shown on the reversal
   * @param source requesting module and reference
   */
  record ReversalRequest(
      Long companyId,
      String invoiceNo,
      String receiptNo,
      String currency,
      BigDecimal amount,
      LocalDate valueDate,
      String reason,
      Source source) {}

  /**
   * Where a reversal request comes from.
   *
   * @param module requesting module (ACSL)
   * @param reference its reference (idempotency key, e.g. the ACSL case number)
   * @param requestedBy user
   */
  record Source(String module, String reference, String requestedBy) {}

  /**
   * Answer to a request.
   *
   * @param status submitted or deferred
   * @param reference Cashiering reference, or the hand-off reference when deferred
   * @param message what happened
   */
  record ReversalTicket(Status status, String reference, String message) {}
}
