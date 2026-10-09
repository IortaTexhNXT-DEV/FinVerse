package com.iortatechnxt.brokerverse.configpromo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A new baseline.
 *
 * @param packageId package exported here or applied here
 * @param name name, e.g. "UAT signed-off baseline 2027-11"
 * @param remarks remarks
 */
public record BaselineRequest(
    @NotNull Long packageId,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String remarks) {}
