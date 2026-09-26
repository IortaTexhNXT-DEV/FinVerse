package com.iortatechnxt.brokerverse.account.domain;

/**
 * How an account was created (shared work item BT0; column {@code acc_account.origin}, V822).
 * Business modules that create accounts on their own flow name themselves here.
 */
public enum AccountOrigin {
  /** Accepted quotation (BRNB.102). */
  QUOTATION,
  /** Accepted non-package proposal request (PRF). */
  PROPOSAL,
  /** Created on the account screen or by bulk upload. */
  DIRECT,
  /** Renewal of a submitted policy (Submitted Policies, BRIDSP-26/27). */
  SUBMITTED_POLICY,
  /** Placement of an Employee Benefits cycle (BRID-017). */
  EMPLOYEE_BENEFITS,
  /** Renewal module (BRRN.033). */
  RENEWAL;

  /**
   * The origin kind of quotation / PRF references: quotation, PRF or direct.
   *
   * @param refs quotation and proposal references
   * @return origin kind
   */
  public static AccountOrigin of(Account.Origin refs) {
    if (present(refs.quotationRef())) {
      return QUOTATION;
    }
    return present(refs.proposalRef()) ? PROPOSAL : DIRECT;
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
