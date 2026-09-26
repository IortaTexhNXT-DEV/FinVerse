package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Reactivates one or more cancelled placements (BRD 2.1.16).
 *
 * @param arns accounts
 * @param comment comment
 */
public record ReactivateRequest(
    @NotEmpty @Size(max = 200) List<@NotNull String> arns, @Size(max = 1000) String comment) {}
