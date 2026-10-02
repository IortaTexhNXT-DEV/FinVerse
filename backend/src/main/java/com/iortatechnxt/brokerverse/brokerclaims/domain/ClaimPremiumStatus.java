package com.iortatechnxt.brokerverse.brokerclaims.domain;

/**
 * Result of the premium check of a claim's cover against the Operations invoice ledger (BRCLM.001,
 * CLAIMS_BROKING_DESIGN 8.3). The authorization code is issued only on PAID, or on DIRECT_PAYMENT
 * under parameter {@code BCL_AUTH_DP_POLICY}.
 */
public enum ClaimPremiumStatus {
  /** Every invoice of the policy year is paid. */
  PAID,
  /** At least one invoice is unpaid. */
  UNPAID,
  /** At least one invoice is partly paid and none is unpaid. */
  PARTIALLY_PAID,
  /** Every invoice is a direct payment to the insurer. */
  DIRECT_PAYMENT,
  /** The policy year has no invoice (cancelled invoices excluded). */
  NO_INVOICE
}
