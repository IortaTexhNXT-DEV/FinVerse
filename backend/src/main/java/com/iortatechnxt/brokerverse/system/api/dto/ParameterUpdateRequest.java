package com.iortatechnxt.brokerverse.system.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New value of a business parameter.
 *
 * @param value value (validated against the parameter type)
 */
public record ParameterUpdateRequest(@NotNull @Size(max = 1000) String value) {}
