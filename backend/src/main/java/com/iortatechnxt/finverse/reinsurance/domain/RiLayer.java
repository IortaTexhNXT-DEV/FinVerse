package com.iortatechnxt.finverse.reinsurance.domain;

/** Layer of a risk (or of a loss) in the reinsurance allocation. */
public enum RiLayer {
  /** Kept by the company. */
  RETENTION,
  /** Quota share treaty. */
  QUOTA_SHARE,
  /** Surplus treaty. */
  SURPLUS,
  /** Facultative placement (sum insured beyond treaty capacity). */
  FAC,
  /** Excess of loss treaty (losses only). */
  XOL;

  /**
   * Layer of a treaty type.
   *
   * @param type treaty type
   * @return layer
   */
  public static RiLayer of(TreatyType type) {
    return switch (type) {
      case QUOTA_SHARE -> QUOTA_SHARE;
      case SURPLUS -> SURPLUS;
      case XOL -> XOL;
    };
  }

  /**
   * Whether the layer is ceded to a proportional treaty.
   *
   * @return true for quota share and surplus
   */
  public boolean isProportionalTreaty() {
    return this == QUOTA_SHARE || this == SURPLUS;
  }
}
