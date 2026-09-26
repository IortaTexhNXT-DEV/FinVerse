package com.iortatechnxt.brokerverse.account.domain;

/**
 * New Business or Renewal classification of an account (BRNB.097, BRID-022.01; shared work item
 * BT0, cross-BRD decision D1). Required on every account; booking copies it to the invoice.
 */
public enum BusinessType {
  /** New business: every account unless created as a renewal. */
  NEW_BUSINESS,
  /** Renewal of an expiring policy ({@code renewal_of_ref} names what it renews). */
  RENEWAL
}
