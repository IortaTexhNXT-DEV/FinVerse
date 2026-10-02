package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;

/**
 * Terms of a new settlement, at 100 %.
 *
 * @param payee party to pay
 * @param costType loss or expense
 * @param settlementType partial or final
 * @param assessedAmount assessed loss / fee before deductions
 * @param deductible policy deductible borne by the insured
 * @param excess excess (amount above limits, or voluntary excess) not payable
 * @param narration narration
 */
public record SettlementTerms(
    Party payee,
    CostType costType,
    SettlementType settlementType,
    BigDecimal assessedAmount,
    BigDecimal deductible,
    BigDecimal excess,
    String narration) {}
