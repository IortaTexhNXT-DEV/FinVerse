package com.iortatechnxt.brokerverse.closing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a completed year-end close.
 *
 * @param companyId company
 * @param fiscalYearId fiscal year closed
 * @param yearCode year code
 * @param closingDate value date of the closing journals (last day of the year)
 * @param netResult profit (positive) or loss transferred to retained earnings
 * @param retainedEarningsAccount retained earnings account
 * @param closingBatches closing journal numbers (comma separated)
 * @param nextYearCode fiscal year prepared next (created if missing)
 */
public record YearEndCloseValues(
    Long companyId,
    Long fiscalYearId,
    int yearCode,
    LocalDate closingDate,
    BigDecimal netResult,
    String retainedEarningsAccount,
    String closingBatches,
    Integer nextYearCode) {}
