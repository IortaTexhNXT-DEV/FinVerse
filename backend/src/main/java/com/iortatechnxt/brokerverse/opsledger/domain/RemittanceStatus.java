package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.Set;

/**
 * Remittance status of an invoice (RMTID.019). The ledger keeps {@link #WITH_OUTSTANDING_BALANCE}
 * and {@link #UNPROCESSED} in step with the payment status; the remittance module sets the other
 * values.
 */
public enum RemittanceStatus {
  /** Paid (or partly) and not yet in any extract. */
  UNPROCESSED,
  /** Payment received but not yet applied. */
  UNAPPLIED_PAYMENT,
  /** Premium not (fully) collected; see Cashiering. */
  WITH_OUTSTANDING_BALANCE,
  /** In a remittance batch under review. */
  REVIEW_IN_PROCESS,
  /** A hold request is pending. */
  REQUESTED_FOR_HOLD,
  /** Batch approved and forwarded to Disbursement; no longer editable. */
  APPROVED,
  /** Partly remitted: DV number assigned, DTIP balance remains. */
  PARTIALLY_REMITTED,
  /** Fully remitted: DV number assigned, no DTIP outstanding. */
  FULLY_REMITTED,
  /** Nothing to remit: direct payment (the client pays the insurer, BRNB.114). */
  NOT_APPLICABLE;

  private static final Set<RemittanceStatus> DERIVED =
      Set.of(UNPROCESSED, WITH_OUTSTANDING_BALANCE);

  /**
   * Whether the ledger may still derive the status from the payment status (the remittance module
   * has not taken the invoice over).
   *
   * @return true for UNPROCESSED and WITH_OUTSTANDING_BALANCE
   */
  public boolean isDerived() {
    return DERIVED.contains(this);
  }

  /**
   * The status the ledger derives from a payment status: paid invoices wait for extraction, others
   * have premium outstanding.
   *
   * @param payment payment status
   * @return UNPROCESSED or WITH_OUTSTANDING_BALANCE
   */
  public static RemittanceStatus derivedFrom(PaymentStatus payment) {
    return payment == PaymentStatus.PAID ? UNPROCESSED : WITH_OUTSTANDING_BALANCE;
  }
}
