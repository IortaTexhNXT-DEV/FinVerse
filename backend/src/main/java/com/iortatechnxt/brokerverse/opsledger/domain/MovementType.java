package com.iortatechnxt.brokerverse.opsledger.domain;

/**
 * Kinds of invoice movements (RMTID.038, ADJID.024) and the balance bucket each one moves. A
 * component's balance is {@code booked + adjusted - applied + reversed - remitted - writtenOff};
 * amounts are signed (a negative REMITTED amount undoes a remittance).
 */
public enum MovementType {
  /** The invoice was booked (booking feed). */
  BOOKED(Bucket.BOOKED),
  /** A payment was applied (cashiering, CSHID.020/022). */
  APPLIED(Bucket.APPLIED),
  /** An application was reversed: receipt cancelled, re-application (CSHID.012, ADJID.009). */
  UNAPPLIED(Bucket.REVERSED),
  /** Remitted to the insurer (remittance batch approved and paid, RMTID). */
  REMITTED(Bucket.REMITTED),
  /** Adjusted by an endorsement or correction (ADJID). */
  ADJUSTED(Bucket.ADJUSTED),
  /** Written off (ADJID.026). */
  WRITE_OFF(Bucket.WRITTEN_OFF),
  /** Direct payment PR reversal after the insurer confirmed the collection (MKTID.012). */
  DP_REVERSAL(Bucket.WRITTEN_OFF),
  /**
   * 2% CWT portion reclassified from PR to PR2307 (CSHID.027): negative on PR, positive on PR2307.
   */
  CWT_RECLASS(Bucket.ADJUSTED),
  /** Minimal balance reversed automatically (CSHID.016). */
  MIN_BAL(Bucket.WRITTEN_OFF);

  private final Bucket bucket;

  MovementType(Bucket bucket) {
    this.bucket = bucket;
  }

  /**
   * The balance bucket the movement changes.
   *
   * @return bucket
   */
  public Bucket bucket() {
    return bucket;
  }

  /** Balance buckets of a component. */
  public enum Bucket {
    /** Booked amount. */
    BOOKED,
    /** Applied payments. */
    APPLIED,
    /** Reversed applications. */
    REVERSED,
    /** Remitted to the insurer. */
    REMITTED,
    /** Adjustments. */
    ADJUSTED,
    /** Written off, minimal balances and DP reversals. */
    WRITTEN_OFF
  }
}
