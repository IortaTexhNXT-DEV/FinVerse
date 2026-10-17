package com.iortatechnxt.brokerverse.coa.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Numbering scheme of the children of a parent account (FRBS 2.3.2).
 *
 * @param companyId company
 * @param parentCode parent account code
 * @param separator "" / "." / "-"
 * @param width digits of the child sequence (1 to 6)
 * @param active whether codes are proposed
 */
public record CoaNumberingRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) String parentCode,
    @Pattern(regexp = "[.\\-]?") String separator,
    @Min(1) @Max(6) int width,
    boolean active) {}
