package com.iortatechnxt.finverse.dimension.api.dto;

import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dimension value maintenance request.
 *
 * @param companyId company
 * @param type type
 * @param code code
 * @param name name
 */
public record DimensionValueRequest(
    @NotNull Long companyId,
    @NotNull DimensionType type,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_\\-]+") String code,
    @NotBlank @Size(max = 120) String name) {}
