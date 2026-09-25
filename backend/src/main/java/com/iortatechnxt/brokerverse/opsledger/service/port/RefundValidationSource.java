package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;

/**
 * Port: validation of a client refund of a cancelled account before Marketing reviews it (MKT
 * 1.11.0, ACSL 2.5.5; ACCOUNTING_DISBURSEMENT_DESIGN 2.2). ACSL checks the cancelled premium and
 * the insurer's return of remitted premium; Cashiering confirms the premium is back in the
 * unapplied list with a new AR number. One bean per validator ({@link #validator()}): acsl
 * implements {@link #ACSL}, cashiering {@link #CASHIERING}. The result comes back as {@code
 * OpsLedgerEvents.RefundValidationCompleted}.
 *
 * <p>Callers use {@code opsledger.service.RefundValidations}, which routes a request to the bean of
 * its validator and hands it over ({@code HandoffRefundValidationSource}) when that module is not
 * installed.
 */
public interface RefundValidationSource {

  /** ACSL validation of the cancelled premium and the insurer's return (ACSL 2.5.5). */
  String ACSL = "ACSL";

  /** Cashiering confirmation of the reinstatement to the unapplied list (MKT 1.11.0). */
  String CASHIERING = "CASHIERING";

  /** Validator code of the hand-off default, which answers for any validator. */
  String ANY = "ANY";

  /**
   * The validator this bean answers for.
   *
   * @return {@link #ACSL} or {@link #CASHIERING}; the hand-off default answers {@link #ANY}
   */
  String validator();

  /**
   * Opens a validation task, idempotent on (validator, source module, source reference).
   *
   * @param request what to validate
   * @return the ticket
   */
  ValidationTicket open(ValidationRequest request);

  /** Where a validation stands when it is opened. */
  enum Status {
    /** A task was opened in the validating module; the result follows as an event. */
    OPENED,
    /** The validating module is not installed: handed over to be done by hand. */
    DEFERRED
  }

  /**
   * A refund to validate.
   *
   * @param companyId company
   * @param validator {@link #ACSL} or {@link #CASHIERING}
   * @param invoiceNo cancelled invoice (or its root)
   * @param arNo acknowledgement receipt of the payment to refund
   * @param clientCode client
   * @param currency currency
   * @param amount refund amount
   * @param source requesting module and reference
   */
  record ValidationRequest(
      Long companyId,
      String validator,
      String invoiceNo,
      String arNo,
      String clientCode,
      String currency,
      BigDecimal amount,
      Source source) {}

  /**
   * Where a validation comes from.
   *
   * @param module requesting module (PAYREQUEST)
   * @param reference its reference (idempotency key)
   * @param requestedBy user
   * @param remarks remarks, may be null
   */
  record Source(String module, String reference, String requestedBy, String remarks) {}

  /**
   * Answer to an opened validation.
   *
   * @param validator validator
   * @param status opened or deferred
   * @param reference task reference (ACSL case, cashiering disposition or hand-off)
   * @param message what happened
   */
  record ValidationTicket(String validator, Status status, String reference, String message) {}
}
