package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Cancels the placement of one or more accounts (BRNB.062).
 *
 * @param arns accounts
 * @param reasonCode reason (list CANCELLATION_REASON)
 * @param comment comment
 */
public record CancelPlacementRequest(
    @NotEmpty @Size(max = 200) List<@NotNull String> arns,
    @NotBlank String reasonCode,
    @Size(max = 1000) String comment) {}
