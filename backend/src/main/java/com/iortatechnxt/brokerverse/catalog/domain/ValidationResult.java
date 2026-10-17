package com.iortatechnxt.brokerverse.catalog.domain;

/** Outcome of the validation checkpoint of a package version (PMADD06). */
public enum ValidationResult {
  /** Validated and released. */
  PASSED,
  /** Returned to MBS with a reason. */
  RETURNED
}
