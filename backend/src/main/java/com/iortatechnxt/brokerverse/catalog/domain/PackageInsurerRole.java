package com.iortatechnxt.brokerverse.catalog.domain;

/**
 * Role of an insurer on a package version (PMADD02, PRODUCT_MAINTENANCE_DESIGN section 4.2; the
 * co-insurance model is PQ03 / OQ33).
 */
public enum PackageInsurerRole {
  /** Lead insurer of a co-insured package. */
  LEAD,
  /** Participating insurer of a co-insured package (share percent). */
  PARTICIPANT,
  /** One of several alternative insurers the package can be placed with. */
  PANEL
}
