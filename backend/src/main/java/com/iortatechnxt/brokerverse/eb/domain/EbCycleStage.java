package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Stage of an EB cycle, the mirror of its {@code EB_CYCLE} work case (design 7.1; V1030). Terminal
 * stages close the cycle.
 */
public enum EbCycleStage {
  /** Opened by the AO (new business) or the RA job (renewal). */
  OPEN,
  /** Renewal advice sent, waiting for the client's feedback (BRID-001-003). */
  RA_SENT,
  /** Client requirements gathered. */
  REQUIREMENTS,
  /** Renewal with the incumbent, without remarketing. */
  INCUMBENT_TERMS,
  /** Remarketing: franchise requests to the insurers (BRID-026, 027). */
  FRANCHISE,
  /** TOR released, insurer proposals awaited (BRID-008, 009). */
  PROPOSALS,
  /** Comparative being built (BRID-010). */
  COMPARATIVE,
  /** Comparative waiting for the authorised signatory. */
  FOR_SIGNOFF,
  /** Above the value threshold: BDOI Management approval (BRID-016). */
  THRESHOLD_APPROVAL,
  /** Approved, to be presented to the client. */
  READY_TO_PRESENT,
  /** With the client for decision. */
  WITH_CLIENT,
  /** Client asked for revisions (BRID-012). */
  REVISION,
  /** Client confirmed the chosen proposals (BRID-017). */
  CONFIRMED,
  /** Accounts created and in the BRD-1 placement flow. */
  IN_PLACEMENT,
  /** Every account booked. */
  PLACED,
  /** Lost. */
  CLOSED_LOST,
  /** Renewal not renewed. */
  NOT_RENEWED;

  /**
   * Whether the stage closes the cycle.
   *
   * @return true for PLACED, CLOSED_LOST and NOT_RENEWED
   */
  public boolean terminal() {
    return this == PLACED || this == CLOSED_LOST || this == NOT_RENEWED;
  }
}
