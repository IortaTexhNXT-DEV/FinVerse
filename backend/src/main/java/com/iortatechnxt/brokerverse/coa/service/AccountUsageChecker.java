package com.iortatechnxt.brokerverse.coa.service;

/**
 * Port answering whether an account already carries postings.
 *
 * <p>Implemented by the ledger module; declared here so the chart of accounts does not depend on
 * the ledger (dependency inversion keeps the module graph acyclic).
 */
public interface AccountUsageChecker {

  /**
   * Checks whether postings exist for an account.
   *
   * @param accountId account id
   * @return true when the account has ledger entries
   */
  boolean hasPostings(Long accountId);
}
