package com.iortatechnxt.brokerverse.catalog.domain;

/**
 * Commercial lifecycle of a product (BRPM.006; PRODUCT_MAINTENANCE_DESIGN section 4.2), next to its
 * maker-checker record status. Only ACTIVE products are sold to new business; expired and retired
 * products stay readable and searchable and are never deleted.
 */
public enum ProductLifecycle {
  /** Sellable. */
  ACTIVE,
  /** The package end date passed without a newer released version (BRPM.006/017). */
  EXPIRED,
  /** Retired by a RETIRE package request (BRPM.011 "deletion"). */
  RETIRED
}
