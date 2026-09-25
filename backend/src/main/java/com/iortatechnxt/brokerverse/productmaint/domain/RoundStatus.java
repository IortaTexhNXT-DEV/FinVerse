package com.iortatechnxt.brokerverse.productmaint.domain;

/**
 * Status of a negotiation round's quotation slip (BRPM.012): prepared by a TSU officer, approved by
 * a TL or co-officer (four eyes) and sent to the insurers; closed when the next round opens or the
 * terms are final.
 */
public enum RoundStatus {
  /** Insurers and notes being prepared. */
  PREPARATION,
  /** Submitted for the QS approval. */
  FOR_APPROVAL,
  /** Approved and sent to the insurers; responses are keyed in. */
  SENT,
  /** Superseded by the next round or closed by terms final. */
  CLOSED
}
