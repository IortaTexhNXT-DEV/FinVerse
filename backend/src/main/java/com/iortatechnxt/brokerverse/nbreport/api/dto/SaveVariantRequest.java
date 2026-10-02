package com.iortatechnxt.brokerverse.nbreport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Saves a report variant (BRNB.057).
 *
 * @param reportCode report
 * @param name name of the variant
 * @param parameters parameter values to save
 * @param shared whether other users who may run the report see it
 */
public record SaveVariantRequest(
    @NotBlank @Size(max = 40) String reportCode,
    @NotBlank @Size(max = 80) String name,
    @NotNull Map<String, String> parameters,
    boolean shared) {}
