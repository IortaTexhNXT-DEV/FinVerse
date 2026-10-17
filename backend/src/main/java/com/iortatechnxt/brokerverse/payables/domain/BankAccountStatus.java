package com.iortatechnxt.brokerverse.payables.domain;

/** Operating status of a company bank account (DIS 2.24.2), apart from its maker-checker status. */
public enum BankAccountStatus {
  /** In use for payments and receipts. */
  ACTIVE,
  /** Tagged inactive: refused for new payments. */
  INACTIVE
}
