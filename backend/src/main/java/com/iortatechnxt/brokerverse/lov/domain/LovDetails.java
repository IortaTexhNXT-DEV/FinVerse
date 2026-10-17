package com.iortatechnxt.brokerverse.lov.domain;

import java.time.LocalDate;

/**
 * Maintainable attributes of a list value.
 *
 * @param label display label
 * @param sortOrder display order
 * @param parentCode optional parent value code (dependent lists)
 * @param effectiveFrom first valid date
 * @param effectiveTo last valid date, null when open ended
 */
public record LovDetails(
    String label,
    int sortOrder,
    String parentCode,
    LocalDate effectiveFrom,
    LocalDate effectiveTo) {}
