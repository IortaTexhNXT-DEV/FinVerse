package com.iortatechnxt.brokerverse.investment.domain;

/**
 * PFRS 9 classification of a financial asset.
 *
 * <ul>
 *   <li>{@link #AMORTIZED_COST}: carried at amortized cost; no fair value remeasurement.
 *   <li>{@link #FVOCI}: remeasured to fair value through other comprehensive income (equity
 *       reserve); the reserve is recycled to profit or loss on derecognition.
 *   <li>{@link #FVPL}: remeasured to fair value through profit or loss.
 * </ul>
 */
public enum Classification {
  AMORTIZED_COST,
  FVOCI,
  FVPL;

  /**
   * Whether fair value remeasurement applies.
   *
   * @return true for FVOCI and FVPL
   */
  public boolean isFairValued() {
    return this != AMORTIZED_COST;
  }
}
