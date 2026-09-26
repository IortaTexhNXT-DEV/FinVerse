package com.iortatechnxt.brokerverse.coa.domain;

/**
 * Primary classification of a GL account. Determines the normal balance side and whether the
 * account is permanent (balance sheet, carried forward) or temporary (P&amp;L, closed at year end).
 */
public enum AccountClass {
  ASSET(BalanceSide.DEBIT, true),
  LIABILITY(BalanceSide.CREDIT, true),
  EQUITY(BalanceSide.CREDIT, true),
  INCOME(BalanceSide.CREDIT, false),
  EXPENSE(BalanceSide.DEBIT, false),
  /** Off-balance-sheet memorandum accounts (e.g. sums insured exposure). */
  MEMORANDUM(BalanceSide.DEBIT, true);

  private final BalanceSide normalBalance;
  private final boolean balanceSheet;

  AccountClass(BalanceSide normalBalance, boolean balanceSheet) {
    this.normalBalance = normalBalance;
    this.balanceSheet = balanceSheet;
  }

  public BalanceSide normalBalance() {
    return normalBalance;
  }

  public boolean isBalanceSheet() {
    return balanceSheet;
  }
}
