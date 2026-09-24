package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;

/**
 * Current year against the previous year for one month.
 *
 * @param month month label {@code yyyy-MM} of the current year
 * @param current amount of the month
 * @param priorYear amount of the same month one year earlier
 */
public record TrendPoint(String month, BigDecimal current, BigDecimal priorYear) {}
