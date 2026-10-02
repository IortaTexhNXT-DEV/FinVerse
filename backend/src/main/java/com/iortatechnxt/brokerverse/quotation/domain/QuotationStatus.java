package com.iortatechnxt.brokerverse.quotation.domain;

/** Status of a quotation, mirrored from its NB_QUOTATION work case (BRNB.022). */
public enum QuotationStatus {
  /** Being prepared (or revised) by Marketing. */
  DRAFT,
  /** Submitted for approval (four eyes, BRNB.014/021). */
  FOR_REVIEW,
  /** Approved; may be sent to the client. */
  APPROVED,
  /** Sent to the client, waiting for the answer. */
  SENT_TO_CLIENT,
  /** Accepted by the client (acceptance e-mail attached, BRNB.045). */
  ACCEPTED,
  /** Accounts created. */
  CONVERTED,
  /** Declined by the client. */
  NOT_PROCEEDED,
  /** Voided while in process. */
  VOIDED
}
