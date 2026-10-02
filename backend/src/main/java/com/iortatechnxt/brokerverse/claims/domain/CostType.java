package com.iortatechnxt.brokerverse.claims.domain;

/**
 * What an estimate or payment covers. Reports leave out EXPENSE when "Include Expense Provision" is
 * unchecked.
 */
public enum CostType {
  /** Indemnity to the claimant (loss). */
  LOSS,
  /** Claim handling expense: surveyor, adjuster and legal fees (expense provision). */
  EXPENSE
}
