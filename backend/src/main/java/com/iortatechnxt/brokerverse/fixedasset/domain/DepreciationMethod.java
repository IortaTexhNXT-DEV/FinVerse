package com.iortatechnxt.brokerverse.fixedasset.domain;

/**
 * Depreciation methods.
 *
 * <ul>
 *   <li>{@link #STRAIGHT_LINE}: (cost - residual value) / useful life months, every month.
 *   <li>{@link #DECLINING_BALANCE}: double-declining balance, i.e. net book value x 2 / useful life
 *       months each month, never below the residual value; the last month of the useful life
 *       charges whatever remains above the residual value.
 * </ul>
 */
public enum DepreciationMethod {
  STRAIGHT_LINE,
  DECLINING_BALANCE
}
