package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;

/**
 * One value per month.
 *
 * @param month month label {@code yyyy-MM}
 * @param amount amount
 */
public record MonthlyValue(String month, BigDecimal amount) {}
