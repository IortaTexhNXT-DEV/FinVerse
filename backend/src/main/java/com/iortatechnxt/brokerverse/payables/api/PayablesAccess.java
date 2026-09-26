package com.iortatechnxt.brokerverse.payables.api;

/**
 * Permission expressions of the payables API.
 *
 * <ul>
 *   <li>Read: anyone with journal inquiry rights (finance users, auditors, read-only users).
 *   <li>Capture: {@code RECEIPT_PAYMENT_MAINTAIN} (maker).
 *   <li>Approve / post / reverse: {@code RECEIPT_PAYMENT_AUTHORIZE} (checker).
 *   <li>Bank accounts and petty cash funds are master data: {@code MASTER_*}.
 * </ul>
 */
final class PayablesAccess {

  static final String VIEW = "hasAuthority('JOURNAL_VIEW')";
  static final String MAINTAIN = "hasAuthority('RECEIPT_PAYMENT_MAINTAIN')";
  static final String AUTHORIZE = "hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')";
  static final String MAINTAIN_OR_AUTHORIZE =
      "hasAnyAuthority('RECEIPT_PAYMENT_MAINTAIN','RECEIPT_PAYMENT_AUTHORIZE')";
  static final String MASTER_VIEW = "hasAuthority('MASTER_VIEW')";
  static final String MASTER_MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  static final String MASTER_AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private PayablesAccess() {}
}
