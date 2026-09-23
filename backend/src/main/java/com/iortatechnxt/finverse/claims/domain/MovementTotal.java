package com.iortatechnxt.finverse.claims.domain;

import java.math.BigDecimal;

/**
 * Aggregate of movement lines of one claim, kind, side and cost type (company share).
 *
 * @param claimId claim
 * @param kind estimate or paid
 * @param side payment or recovery
 * @param costType loss or expense
 * @param amount total in claim currency
 * @param baseAmount total in base currency
 */
public record MovementTotal(
    Long claimId,
    MovementKind kind,
    EstimateSide side,
    CostType costType,
    BigDecimal amount,
    BigDecimal baseAmount) {}
