package com.iortatechnxt.brokerverse.booking.domain;

/** Type of an endorsement posted on a booked account (BRNB.076/081/094). */
public enum EndorsementType {
  /** Positive financial endorsement: additional premium, new invoice (BRNB.061). */
  POSITIVE,
  /** Negative financial endorsement: return premium, credit invoice (BRNB.081). */
  NEGATIVE,
  /** Non-financial endorsement: recorded only, no GL entry (BRNB.076). */
  NON_FINANCIAL,
  /** Cancellation after issuance (BRNB.094). */
  CANCELLATION
}
