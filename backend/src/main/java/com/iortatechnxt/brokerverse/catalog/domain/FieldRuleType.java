package com.iortatechnxt.brokerverse.catalog.domain;

/** What a field rule checks (BRPM.004; PRODUCT_MAINTENANCE_DESIGN section 4.1). */
public enum FieldRuleType {
  /** The field must hold a value (the minimum-field matrix, BRNB.002/003). */
  REQUIRED,
  /** A given value must be an active code of a list of values. */
  LOV,
  /** A given number must be within a minimum and / or maximum. */
  RANGE,
  /** A given value must match a regular expression. */
  PATTERN
}
