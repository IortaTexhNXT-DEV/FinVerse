package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.SettlementTerms;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;

/**
 * Maker input of a settlement, at 100 %.
 *
 * @param payeeCode party to pay (claimant, garage, surveyor)
 * @param costType loss or expense
 * @param settlementType partial or final
 * @param assessedAmount assessed amount before deductions
 * @param deductible policy deductible, may be null
 * @param excess excess, may be null
 * @param narration narration
 */
public record SettlementCommand(
    String payeeCode,
    CostType costType,
    SettlementType settlementType,
    BigDecimal assessedAmount,
    BigDecimal deductible,
    BigDecimal excess,
    String narration) {

  /**
   * Domain terms with the resolved payee.
   *
   * @param payee payee party
   * @return terms
   */
  public SettlementTerms toTerms(Party payee) {
    return new SettlementTerms(
        payee, costType, settlementType, assessedAmount, deductible, excess, narration);
  }
}
