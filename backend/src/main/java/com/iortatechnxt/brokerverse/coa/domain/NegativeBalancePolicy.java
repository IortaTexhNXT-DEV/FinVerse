package com.iortatechnxt.brokerverse.coa.domain;

/**
 * What posting does when a journal would leave an account with a balance on the side opposite to
 * its natural side (FRBS 2.5.4, 2.8.4, 3.6.0b; AQ30). The natural side comes from the {@link
 * AccountClass}: debit for assets and expenses, credit for liabilities, equity and income.
 */
public enum NegativeBalancePolicy {
  /** No check (default: most accounts may swing either way). */
  ALLOW,
  /** The journal posts, but the maker and the checker see a warning. */
  WARN,
  /** The journal is refused. */
  BLOCK
}
