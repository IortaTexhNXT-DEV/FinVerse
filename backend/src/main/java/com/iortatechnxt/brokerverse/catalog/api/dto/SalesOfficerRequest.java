package com.iortatechnxt.brokerverse.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Places a user in a sales team.
 *
 * @param companyId company
 * @param teamCode team
 * @param username user
 */
public record SalesOfficerRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) String teamCode,
    @NotBlank @Size(max = 50) String username) {}
