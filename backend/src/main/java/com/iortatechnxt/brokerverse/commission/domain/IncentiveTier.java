package com.iortatechnxt.brokerverse.commission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * One tier of an incentive scheme (CMRID.005/006): for target-tiered schemes the production target
 * reached with its rate and multiplier; for fixed-per-policy schemes the minimum basic premium with
 * its fixed amount per policy.
 *
 * @param minProduction production target of the tier (target-tiered)
 * @param ratePercent incentive rate in percent of the production (target-tiered)
 * @param multiplier multiplier of the rate, 1 when none (target-tiered)
 * @param minBasicPremium minimum basic premium of a policy (fixed per policy)
 * @param fixedAmount amount per qualifying policy (fixed per policy)
 */
@Embeddable
public record IncentiveTier(
    @Column(name = "min_production", precision = 19, scale = 2) BigDecimal minProduction,
    @Column(name = "rate_percent", precision = 9, scale = 4) BigDecimal ratePercent,
    @Column(precision = 9, scale = 4) BigDecimal multiplier,
    @Column(name = "min_basic_premium", precision = 19, scale = 2) BigDecimal minBasicPremium,
    @Column(name = "fixed_amount", precision = 19, scale = 2) BigDecimal fixedAmount) {}
