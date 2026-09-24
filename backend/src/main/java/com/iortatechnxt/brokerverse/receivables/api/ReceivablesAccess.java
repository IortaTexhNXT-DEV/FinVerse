package com.iortatechnxt.brokerverse.receivables.api;

/**
 * Permission expressions of the receivables API (same model as payables).
 *
 * <ul>
 *   <li>Read: anyone with journal inquiry rights (finance users, auditors, read-only users) as well
 *       as the receipt makers and checkers.
 *   <li>Capture: {@code RECEIPT_PAYMENT_MAINTAIN} (maker); approve / cancel / bounce: {@code
 *       RECEIPT_PAYMENT_AUTHORIZE} (checker).
 *   <li>Bank reconciliation: {@code RECONCILIATION_MANAGE}.
 * </ul>
 */
final class ReceivablesAccess {

  static final String VIEW =
      "hasAnyAuthority('JOURNAL_VIEW','RECEIPT_PAYMENT_MAINTAIN','RECEIPT_PAYMENT_AUTHORIZE')";
  static final String VIEW_OR_RECONCILE =
      "hasAnyAuthority('JOURNAL_VIEW','RECEIPT_PAYMENT_MAINTAIN','RECEIPT_PAYMENT_AUTHORIZE',"
          + "'RECONCILIATION_MANAGE')";
  static final String VIEW_OR_REPORT =
      "hasAnyAuthority('JOURNAL_VIEW','RECEIPT_PAYMENT_MAINTAIN','RECEIPT_PAYMENT_AUTHORIZE',"
          + "'REPORT_FINANCIAL')";

  private ReceivablesAccess() {}
}
