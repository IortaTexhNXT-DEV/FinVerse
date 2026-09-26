package com.iortatechnxt.brokerverse.adjustment.domain;

/**
 * How the amounts of a request are computed and posted, derived from its request type (Annex V
 * {@code ENDORSEMENT_REQUEST_TYPE}).
 */
public enum Computation {
  /** No financial effect (non-financial and internal requests without a financial request type). */
  NONE,
  /** Flat cancellation: every premium component and the commission are returned. */
  CANCELLATION_FLAT,
  /** Flat cancellation with the documentary stamp tax retained. */
  CANCELLATION_FLAT_RETAIN_DST,
  /** Partial cancellation of the unexpired term (pro-rata or short-period). */
  CANCELLATION_PARTIAL,
  /** Change of the total sum insured, recomputed per insurer for the remaining term. */
  SUM_INSURED,
  /** Premium component and / or commission changes entered by the processor. */
  AMOUNTS,
  /** Write-off of the outstanding premium receivable. */
  WRITE_OFF;

  /** Request type of a flat cancellation. */
  public static final String FLAT_CANCELLATION = "FLAT_CANCELLATION";

  /** Request type of a flat cancellation retaining DST. */
  public static final String FLAT_RETAIN_DST = "FLAT_CANCELLATION_RETAIN_DST";

  /** Request type of a partial cancellation. */
  public static final String PARTIAL_CANCELLATION = "PARTIAL_CANCELLATION";

  /** Request type of a change of the total sum insured. */
  public static final String TSI_CHANGE = "TSI_CHANGE";

  /** Request type of a write-off. */
  public static final String WRITE_OFF_TYPE = "WRITE_OFF";

  /**
   * The computation of a request type.
   *
   * @param requestType Annex V request type, null for none
   * @return computation
   */
  public static Computation forRequestType(String requestType) {
    if (requestType == null || requestType.isBlank()) {
      return NONE;
    }
    return switch (requestType) {
      case FLAT_CANCELLATION -> CANCELLATION_FLAT;
      case FLAT_RETAIN_DST -> CANCELLATION_FLAT_RETAIN_DST;
      case PARTIAL_CANCELLATION -> CANCELLATION_PARTIAL;
      case TSI_CHANGE -> SUM_INSURED;
      case WRITE_OFF_TYPE -> WRITE_OFF;
      default -> AMOUNTS;
    };
  }

  /**
   * Whether the computation is a cancellation.
   *
   * @return true for the three cancellation kinds
   */
  public boolean isCancellation() {
    return this == CANCELLATION_FLAT
        || this == CANCELLATION_FLAT_RETAIN_DST
        || this == CANCELLATION_PARTIAL;
  }

  /**
   * Whether the computation moves money.
   *
   * @return false only for NONE
   */
  public boolean isFinancial() {
    return this != NONE;
  }
}
