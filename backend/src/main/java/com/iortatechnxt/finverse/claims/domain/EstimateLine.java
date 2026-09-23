package com.iortatechnxt.finverse.claims.domain;

import java.math.BigDecimal;

/**
 * An amount on one side and cost type of a claim estimate.
 *
 * @param side payment or recovery
 * @param costType loss or expense (recoveries are loss only)
 * @param amount amount at 100 %
 */
public record EstimateLine(EstimateSide side, CostType costType, BigDecimal amount) {}
