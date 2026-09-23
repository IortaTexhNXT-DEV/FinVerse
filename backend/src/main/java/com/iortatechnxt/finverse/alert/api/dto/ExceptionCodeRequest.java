package com.iortatechnxt.finverse.alert.api.dto;

import com.iortatechnxt.finverse.alert.domain.AlertSeverity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Tunable settings of an exception code.
 *
 * @param severity severity
 * @param thresholdAmount threshold amount (optional)
 * @param thresholdDays threshold days (optional)
 * @param active monitored
 */
public record ExceptionCodeRequest(
    @NotNull AlertSeverity severity,
    @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal thresholdAmount,
    @Min(0) @Max(3650) Integer thresholdDays,
    boolean active) {}
