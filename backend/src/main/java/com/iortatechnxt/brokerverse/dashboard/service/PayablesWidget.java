package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vendor payables by due date.
 *
 * @param asOf reference date
 * @param overdue due before the reference date
 * @param dueIn7Days due within 7 days of the reference date
 * @param dueIn30Days due within 30 days of the reference date (includes the 7 days)
 * @param total all outstanding vendor payables
 * @param openItems number of outstanding items
 */
public record PayablesWidget(
    LocalDate asOf,
    BigDecimal overdue,
    BigDecimal dueIn7Days,
    BigDecimal dueIn30Days,
    BigDecimal total,
    long openItems) {}
