package com.iortatechnxt.brokerverse.tax.domain;

/**
 * BIR payee classification: ATCs starting with WI apply to individuals, WC to corporations
 * (juridical persons). Individuals are reported with last, first and middle name on the alphalists.
 */
public enum PayeeClass {
  INDIVIDUAL,
  CORPORATE
}
