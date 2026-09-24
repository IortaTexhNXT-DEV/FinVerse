package com.iortatechnxt.brokerverse.booking.domain;

/** Role of a sub-ledger open item recorded for a booked invoice. */
public enum OpenItemRole {
  /** Premium receivable from the client (DEBIT). */
  CLIENT_PREMIUM,
  /** Premium due to the insurer, DTIP (CREDIT). */
  INSURER_DTIP,
  /** Commission (with VAT) receivable from the insurer (DEBIT). */
  INSURER_COMMISSION
}
