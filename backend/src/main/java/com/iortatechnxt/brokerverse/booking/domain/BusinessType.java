package com.iortatechnxt.brokerverse.booking.domain;

/**
 * New Business vs Renewal classification of a booked invoice (BRNB.097, BRID-022.01), copied from
 * the account's business type at booking (shared work item BT0).
 */
public enum BusinessType {
  /** New Business (BRD-1). */
  NEW_BUSINESS,
  /** Renewal (Renewal, Employee Benefits and Submitted Policies renewals). */
  RENEWAL;

  /**
   * The invoice classification of an account's business type.
   *
   * @param accountType account business type, null for new business
   * @return invoice business type
   */
  public static BusinessType of(
      com.iortatechnxt.brokerverse.account.domain.BusinessType accountType) {
    return accountType == null ? NEW_BUSINESS : valueOf(accountType.name());
  }
}
