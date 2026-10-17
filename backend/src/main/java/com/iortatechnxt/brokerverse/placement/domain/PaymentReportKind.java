package com.iortatechnxt.brokerverse.placement.domain;

/** Kind of payment report and its matching key. */
public enum PaymentReportKind {
  /** CLPC payment report of CBG Fire accounts, matched by PN or loan application number. */
  CLPC,
  /** Payment report of the other segments, matched by the Account Reference Number. */
  REFERENCE
}
