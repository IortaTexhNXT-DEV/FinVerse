package com.iortatechnxt.brokerverse.catalog.domain;

/** VAT treatment of an other charge billed with the premium. */
public enum ChargeVatTreatment {
  /** VAT is added to the charge at the VAT rate of the line. */
  VATABLE,
  /** No VAT: the charge is exempt. */
  EXEMPT,
  /** No VAT: the charge is zero-rated. */
  ZERO_RATED
}
