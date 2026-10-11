package com.iortatechnxt.brokerverse.catalog.domain;

/** Where a field or document rule applies; a narrower scope overrides a wider one. */
public enum RuleScope {
  /** Every product. */
  ALL,
  /** Every product of one product line. */
  LINE,
  /** One product (risk code). */
  PRODUCT
}
