package com.iortatechnxt.brokerverse.reinsurance.domain;

/** Kind of reinsurance treaty. */
public enum TreatyType {
  /** Proportional: a fixed percentage of every risk up to the treaty limit. */
  QUOTA_SHARE,
  /** Proportional: the sum insured above the retention, up to a number of lines. */
  SURPLUS,
  /** Non-proportional excess of loss: losses above a priority, up to a limit, per layer. */
  XOL;

  /**
   * Whether premium and losses are shared in proportion to the sum insured ceded.
   *
   * @return true for quota share and surplus
   */
  public boolean isProportional() {
    return this != XOL;
  }
}
