package com.iortatechnxt.finverse.consolidation.service;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRunLine;
import java.math.BigDecimal;

/**
 * Consolidated balance of one group account (net debit, consolidation currency), with the part that
 * comes from eliminations.
 *
 * @param accountCode account code
 * @param accountName account name
 * @param accountClass class
 * @param aggregated sum of translated member balances and CTA
 * @param eliminations sum of elimination lines
 */
public record ConsolidatedBalance(
    String accountCode,
    String accountName,
    AccountClass accountClass,
    BigDecimal aggregated,
    BigDecimal eliminations) {

  /**
   * Starts a balance from one line.
   *
   * @param l line
   * @return balance
   */
  public static ConsolidatedBalance of(ConsolidationRunLine l) {
    boolean elimination = l.getType() == ConsolidationLineType.ELIMINATION;
    return new ConsolidatedBalance(
        l.getAccountCode(),
        l.getAccountName(),
        l.getAccountClass(),
        elimination ? BigDecimal.ZERO : l.getAmount(),
        elimination ? l.getAmount() : BigDecimal.ZERO);
  }

  /**
   * Adds another balance of the same account.
   *
   * @param other other balance
   * @return sum
   */
  public ConsolidatedBalance plus(ConsolidatedBalance other) {
    return new ConsolidatedBalance(
        accountCode,
        accountName,
        accountClass,
        aggregated.add(other.aggregated),
        eliminations.add(other.eliminations));
  }

  /**
   * Consolidated net balance.
   *
   * @return aggregated plus eliminations (debit positive)
   */
  public BigDecimal consolidated() {
    return aggregated.add(eliminations);
  }
}
