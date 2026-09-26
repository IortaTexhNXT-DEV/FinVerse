package com.iortatechnxt.brokerverse.catalog.domain;

/**
 * Lifecycle of a package product version (BRPM.006/007, PMADD06; PRODUCT_MAINTENANCE_DESIGN section
 * 5.1). Only RELEASED versions are sellable; versions are never deleted.
 */
public enum ProductVersionStatus {
  /** Being set up by MBS (editable, invisible to rating). */
  DRAFT,
  /** Submitted for the post-set-up validation checkpoint (PRODUCT_VALIDATE, not the maker). */
  FOR_VALIDATION,
  /** Validated and released: sells from its effective date. */
  RELEASED,
  /** Replaced by a newer released version (renewals may still reference it, BRPM.007). */
  SUPERSEDED,
  /** The package end date passed without a newer released version (BRPM.006/017). */
  EXPIRED
}
