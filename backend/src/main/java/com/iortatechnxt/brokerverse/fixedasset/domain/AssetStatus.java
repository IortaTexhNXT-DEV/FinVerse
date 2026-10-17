package com.iortatechnxt.brokerverse.fixedasset.domain;

/** Life cycle of a fixed asset in the register. */
public enum AssetStatus {
  /** Registered by a maker; not yet capitalized (no posting). */
  PENDING_CAPITALIZATION,
  /** Capitalized and depreciating. */
  ACTIVE,
  /** Net book value has reached the residual value; kept in the register until disposal. */
  FULLY_DEPRECIATED,
  /** Derecognized (sold or scrapped). */
  DISPOSED,
  /** In service at another branch after an inter-branch transfer; keeps depreciating. */
  TRANSFERRED;

  /**
   * Whether the asset is still carried in the books.
   *
   * @return true for ACTIVE, FULLY_DEPRECIATED and TRANSFERRED
   */
  public boolean isInService() {
    return this == ACTIVE || this == FULLY_DEPRECIATED || this == TRANSFERRED;
  }

  /**
   * Whether the monthly depreciation run picks the asset up.
   *
   * @return true for ACTIVE and TRANSFERRED
   */
  public boolean isDepreciable() {
    return this == ACTIVE || this == TRANSFERRED;
  }
}
