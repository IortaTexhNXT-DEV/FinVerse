package com.iortatechnxt.brokerverse.tax.service;

import java.math.BigDecimal;

/**
 * One computed line of an Insurance Commission schedule.
 *
 * @param lineCode mapping line code
 * @param description description
 * @param mapping accounts mapped (range / report group)
 * @param businessLine line of business (per-LOB schedules), else null
 * @param amount ledger amount presented on the line's natural side
 * @param signedAmount contribution to the schedule total (amount × sign factor)
 * @param rbcFactor RBC factor in percent (RBC lines), else null
 * @param requirement capital requirement = amount × factor (RBC lines), else null
 */
public record IcScheduleLine(
    String lineCode,
    String description,
    String mapping,
    String businessLine,
    BigDecimal amount,
    BigDecimal signedAmount,
    BigDecimal rbcFactor,
    BigDecimal requirement) {}
