package com.iortatechnxt.brokerverse.tax.domain;

/**
 * Computation worksheet behind a tax form. {@link #NONE} marks forms prepared outside BrokerVerse
 * (e.g. 1601-C from the payroll system) that appear on the calendar as reminders only.
 */
public enum WorksheetKind {
  VAT,
  EWT,
  NONE
}
