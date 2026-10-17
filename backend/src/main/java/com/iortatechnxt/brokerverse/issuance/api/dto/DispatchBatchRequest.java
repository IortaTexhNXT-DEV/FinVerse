package com.iortatechnxt.brokerverse.issuance.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sends several e-policies with their proposed e-mail (BRNB.077 batch).
 *
 * @param epolicyIds confirmed e-policies
 * @param passwordHint password hint; the configured one when empty
 */
public record DispatchBatchRequest(
    @NotEmpty @Size(max = 200) List<@NotNull Long> epolicyIds,
    @Size(max = 300) String passwordHint) {}
