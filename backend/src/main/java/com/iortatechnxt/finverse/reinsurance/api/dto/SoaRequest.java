package com.iortatechnxt.finverse.reinsurance.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Generates (or regenerates while pending) the statements of account of a treaty for a quarter.
 *
 * @param companyId company
 * @param treatyCode treaty code
 * @param reinsurerCode one participant, blank for every participant
 * @param year year
 * @param quarter quarter 1-4
 * @param statementDate statement date, blank for the day after the quarter end
 */
public record SoaRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) String treatyCode,
    @Size(max = 20) String reinsurerCode,
    @NotNull @Min(2000) @Max(2100) Integer year,
    @NotNull @Min(1) @Max(4) Integer quarter,
    LocalDate statementDate) {}
