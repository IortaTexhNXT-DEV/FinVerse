package com.iortatechnxt.brokerverse.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * New or changed cover type.
 *
 * @param lineCode product line (ignored on update)
 * @param code code (ignored on update)
 * @param name name
 * @param sortOrder display order
 */
public record CoverTypeRequest(
    @NotBlank @Size(max = 30) String lineCode,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 120) String name,
    @PositiveOrZero int sortOrder) {}
