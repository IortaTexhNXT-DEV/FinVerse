package com.iortatechnxt.finverse.closing.domain;

import java.time.LocalDate;

/**
 * Settings of a revaluation run.
 *
 * @param companyId company
 * @param periodId accounting period revalued
 * @param periodName period name
 * @param revaluationDate period end date (value date of the journal)
 * @param gainLossAccount unrealized FX gain/loss account
 * @param autoReverse whether to reverse on the first day of the next period
 * @param reversalDate first day of the next period (when auto-reversing)
 */
public record FxRevaluationHeader(
    Long companyId,
    Long periodId,
    String periodName,
    LocalDate revaluationDate,
    String gainLossAccount,
    boolean autoReverse,
    LocalDate reversalDate) {}
