package com.iortatechnxt.brokerverse.finreport.domain;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A figure shown by a schedule, in column order (table {@code fin_schedule_column}).
 *
 * @param seq order
 * @param measure figure
 * @param label column heading
 */
@Embeddable
public record ScheduleColumn(
    @Column(nullable = false) int seq,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Measure measure,
    @Column(nullable = false, length = 60) String label) {}
