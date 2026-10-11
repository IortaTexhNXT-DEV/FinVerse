package com.iortatechnxt.brokerverse.receivables.domain;

/**
 * Column mapping of a spreadsheet statement (FRBS 3.3.1). Either {@code amountColumn} (signed,
 * money in positive) or both {@code debitColumn} and {@code creditColumn} are given.
 *
 * @param name layout name, e.g. "BDO current account export"
 * @param dateColumn column of the value date
 * @param descriptionColumn column of the description
 * @param referenceColumn column of the reference / cheque number
 * @param debitColumn column of withdrawals
 * @param creditColumn column of deposits
 * @param amountColumn column of a signed amount
 * @param balanceColumn column of the running balance
 * @param datePattern date pattern, e.g. yyyy-MM-dd or MM/dd/yyyy
 */
public record StatementLayoutValues(
    String name,
    String dateColumn,
    String descriptionColumn,
    String referenceColumn,
    String debitColumn,
    String creditColumn,
    String amountColumn,
    String balanceColumn,
    String datePattern) {

  /** Blank columns become null; the date pattern defaults to ISO. */
  public StatementLayoutValues {
    descriptionColumn = blankToNull(descriptionColumn);
    referenceColumn = blankToNull(referenceColumn);
    debitColumn = blankToNull(debitColumn);
    creditColumn = blankToNull(creditColumn);
    amountColumn = blankToNull(amountColumn);
    balanceColumn = blankToNull(balanceColumn);
    datePattern = datePattern == null || datePattern.isBlank() ? "yyyy-MM-dd" : datePattern.trim();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
