package com.iortatechnxt.brokerverse.tax.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to issue the BIR Form 2307 certificates of a quarter.
 *
 * @param companyId company
 * @param year year
 * @param quarter quarter 1-4
 */
public record CertificateBatchRequest(
    @NotNull Long companyId, @Min(2000) @Max(2100) int year, @Min(1) @Max(4) int quarter) {}
