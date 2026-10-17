package com.iortatechnxt.brokerverse.catalog.domain;

/**
 * Tax registration of an insurer (BDOI inputs TX-Q02, template R04): it decides whether the premium
 * carries VAT or premium tax. An insurer without a status follows the taxes of the product line.
 */
public enum InsurerTaxStatus {
  /** Registered for VAT: VAT on premium, no premium tax. */
  VAT_REGISTERED,
  /** Not registered for VAT: premium tax, no VAT on premium. */
  NON_VAT
}
