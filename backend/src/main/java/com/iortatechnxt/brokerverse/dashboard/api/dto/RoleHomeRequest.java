package com.iortatechnxt.brokerverse.dashboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for a new set of home-page widgets of a role.
 *
 * @param widgets widget codes in display order
 * @param reason why the set changes (stored with the change)
 */
public record RoleHomeRequest(
    @NotEmpty @Size(max = 20) List<String> widgets, @NotBlank @Size(max = 400) String reason) {}
